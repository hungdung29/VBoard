package com.vboard.aac.platform.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.vboard.aac.domain.repository.IVoiceProfileRepository
import com.vboard.aac.domain.repository.ISettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: ISettingsRepository,
    private val voiceProfileRepository: IVoiceProfileRepository,
    private val valtecTtsEngine: ValtecTtsEngine,
    private val vieneuCloneClient: VieneuCloneClient
) : TextToSpeech.OnInitListener {

    private companion object {
        private const val TAG = "TextToSpeechManager"
        private const val MAX_AUDIO_CACHE_ENTRIES = 24
    }

    private var tts: TextToSpeech? = null
    private var valtecAudioTrack: AudioTrack? = null
    private var presetPreviewPlayer: MediaPlayer? = null
    private var vieneuClonePlayer: MediaPlayer? = null
    private val playerLock = Any()
    private var speakJob: Job? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var onCompleteCallback: (() -> Unit)? = null
    private var speechRate = 0.4f
    private var valtecPreloadStarted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val valtecAudioCache = object : LinkedHashMap<String, FloatArray>(MAX_AUDIO_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>?): Boolean {
            return size > MAX_AUDIO_CACHE_ENTRIES
        }
    }
    private val vieneuAudioCache = object : LinkedHashMap<String, File>(MAX_AUDIO_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, File>?): Boolean {
            val shouldRemove = size > MAX_AUDIO_CACHE_ENTRIES
            if (shouldRemove) eldest?.value?.delete()
            return shouldRemove
        }
    }

    init {
        mainHandler.post {
            ensureSystemTts()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(java.util.Locale("vi", "VN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(java.util.Locale("vi"))
            }
            tts?.setSpeechRate(speechRate)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onDone(utteranceId: String?) {
                    onCompleteCallback?.let { callback ->
                        mainHandler.post { callback() }
                    }
                }

                override fun onError(utteranceId: String?) {}
                override fun onStart(utteranceId: String?) {}
            })
            _isReady.value = true
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (text.isBlank()) return

        speakJob?.cancel()
        stop()
        speakJob = scope.launch {
            val spokeWithVieneu = speakWithVieneuCloneIfAvailable(text, onComplete)
            if (spokeWithVieneu) return@launch

            val spokeWithValtec = speakWithValtecPresetIfAvailable(text, onComplete)
            if (!spokeWithValtec) {
                speakWithSystemTts(text, onComplete)
            }
        }
    }

    fun preloadValtec() {
        if (valtecPreloadStarted || valtecTtsEngine.isReady()) return
        valtecPreloadStarted = true
        scope.launch(Dispatchers.IO) {
            valtecTtsEngine.initialize()
        }
    }

    fun preview(text: String, voiceType: String) {
        if (text.isBlank()) return

        scope.launch {
            val previewAsset = presetPreviewAsset(voiceType)
            if (previewAsset != null && playPresetPreview(previewAsset)) {
                return@launch
            }

            val speakerId = ValtecTtsEngine.speakerIdForVoiceType(voiceType)
            if (speakerId == null) {
                speakWithSystemTts(text, null)
                return@launch
            }

            val audio = synthesizeValtec(
                text = text,
                speakerEmbedding = valtecTtsEngine.createSpeakerEmbedding(speakerId),
                cacheKey = cacheKey(text, voiceType)
            )
            if (audio.isNotEmpty()) {
                playValtecAudio(audio)
            } else {
                Log.w(TAG, "Valtec preview failed for voiceType=$voiceType")
            }
        }
    }

    suspend fun generateVieneuClonePreview(text: String): Result<Unit> = runCatching {
        require(text.isNotBlank()) { "Preview text is required" }
        getOrSynthesizeVieneuCloneAudio(text)
    }.map { }

    suspend fun playVieneuClonePreview(text: String): Result<Unit> = runCatching {
        require(text.isNotBlank()) { "Preview text is required" }
        playVieneuAudio(getOrSynthesizeVieneuCloneAudio(text))
    }

    private suspend fun playPresetPreview(assetPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, File(assetPath).name)
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val volume = settingsRepository.voiceVolume.first()
            synchronized(playerLock) {
                releasePresetPreviewPlayer()
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(cacheFile.absolutePath)
                    setVolume(volume, volume)
                    prepare()
                    setOnCompletionListener { completedPlayer ->
                        synchronized(playerLock) {
                            completedPlayer.release()
                            if (presetPreviewPlayer === completedPlayer) {
                                presetPreviewPlayer = null
                            }
                        }
                    }
                    start()
                }
                presetPreviewPlayer = player
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Preset preview failed for asset=$assetPath", e)
            false
        }
    }

    private fun presetPreviewAsset(voiceType: String): String? = when (voiceType) {
        ValtecTtsEngine.VOICE_NF, "nu-bac" -> "valtec/previews/valtec_nf.wav"
        ValtecTtsEngine.VOICE_SF, "nu-nam" -> "valtec/previews/valtec_sf.wav"
        ValtecTtsEngine.VOICE_NM1, "nam-bac" -> "valtec/previews/valtec_nm1.wav"
        ValtecTtsEngine.VOICE_SM, "nam-nam" -> "valtec/previews/valtec_sm.wav"
        ValtecTtsEngine.VOICE_NM2 -> "valtec/previews/valtec_nm2.wav"
        else -> null
    }

    private suspend fun speakWithVieneuCloneIfAvailable(
        text: String,
        onComplete: (() -> Unit)?
    ): Boolean {
        if (!settingsRepository.voiceCloningEnabled.first()) return false

        return try {
            playVieneuAudio(getOrSynthesizeVieneuCloneAudio(text))
            onComplete?.invoke()
            true
        } catch (e: Exception) {
            Log.e(TAG, "VieNeu cloning failed; falling back to Valtec preset", e)
            false
        }
    }

    private suspend fun getOrSynthesizeVieneuCloneAudio(text: String): File {
        val activeProfile = voiceProfileRepository.getActiveProfile()
            ?: error("No active cloned voice profile")
        val referenceAudioPath = activeProfile.referenceAudioPath
            ?: error("Cloned voice reference audio is missing")
        val referenceAudio = File(referenceAudioPath)
        require(referenceAudio.isFile) { "Cloned voice reference audio is missing" }

        val serverUrl = settingsRepository.vieneuServerUrl.first()
        val cacheKey = "vieneu|$serverUrl|${activeProfile.id}|${text.trim().lowercase()}"
        return synchronized(vieneuAudioCache) {
            vieneuAudioCache[cacheKey]
        } ?: vieneuCloneClient.synthesize(
            serverUrl = serverUrl,
            text = text,
            referenceAudio = referenceAudio
        ).also { generatedFile ->
            synchronized(vieneuAudioCache) {
                vieneuAudioCache[cacheKey] = generatedFile
            }
        }
    }

    private suspend fun speakWithValtecPresetIfAvailable(
        text: String,
        onComplete: (() -> Unit)?
    ): Boolean {
        val voiceType = settingsRepository.voiceType.first()
        val presetSpeakerId = ValtecTtsEngine.speakerIdForVoiceType(voiceType) ?: return false
        val speakerEmbedding = valtecTtsEngine.createSpeakerEmbedding(presetSpeakerId)

        return try {
            if (!valtecTtsEngine.isReady()) {
                valtecTtsEngine.initialize()
            }
            if (!valtecTtsEngine.isReady()) return false

            val audio = synthesizeValtec(
                text = text,
                speakerEmbedding = speakerEmbedding,
                cacheKey = cacheKey(text, voiceType)
            )
            if (audio.isEmpty()) return false
            playValtecAudio(audio)
            onComplete?.invoke()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Valtec TTS failed; falling back to system TTS", e)
            false
        }
    }

    private suspend fun synthesizeValtec(
        text: String,
        speakerEmbedding: FloatArray,
        cacheKey: String
    ): FloatArray {
        synchronized(valtecAudioCache) {
            valtecAudioCache[cacheKey]?.let { return it }
        }

        val audio = valtecTtsEngine.synthesize(
            text = text,
            speakerEmbedding = speakerEmbedding,
            lengthScale = 0.92f
        )
        if (audio.isNotEmpty()) {
            synchronized(valtecAudioCache) {
                valtecAudioCache[cacheKey] = audio
            }
        }
        return audio
    }

    private fun speakWithSystemTts(text: String, onComplete: (() -> Unit)?) {
        ensureSystemTts()
        if (_isReady.value.not()) return

        onCompleteCallback = onComplete
        val utteranceId = "vboard_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private suspend fun playValtecAudio(audio: FloatArray) = withContext(Dispatchers.IO) {
        if (audio.isEmpty()) return@withContext

        val volume = settingsRepository.voiceVolume.first()
        var track: AudioTrack? = null
        try {
            synchronized(playerLock) {
                releaseValtecAudioTrack()
                val pcm = ShortArray(audio.size) { index ->
                    (audio[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                val bufferSizeBytes = pcm.size * 2

                val newTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(ValtecTtsEngine.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSizeBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
                    .build()

                track = newTrack
                valtecAudioTrack = newTrack
                newTrack.setVolume(volume)
                newTrack.write(pcm, 0, pcm.size)
                newTrack.play()
            }

            val durationMs = (audio.size * 1000L / ValtecTtsEngine.SAMPLE_RATE).coerceAtLeast(100L)
            delay(durationMs + 50L)
        } finally {
            synchronized(playerLock) {
                if (valtecAudioTrack === track && track != null) {
                    releaseValtecAudioTrack()
                }
            }
        }
    }

    private suspend fun playVieneuAudio(audioFile: File) = withContext(Dispatchers.IO) {
        val volume = settingsRepository.voiceVolume.first()
        var player: MediaPlayer? = null
        val playCompletion = CompletableDeferred<Unit>()
        try {
            synchronized(playerLock) {
                releaseVieneuClonePlayer()
                val newPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(audioFile.absolutePath)
                    setVolume(volume, volume)
                    setOnCompletionListener {
                        playCompletion.complete(Unit)
                    }
                    setOnErrorListener { _, _, _ ->
                        playCompletion.complete(Unit)
                        false
                    }
                    prepare()
                    start()
                }
                player = newPlayer
                vieneuClonePlayer = newPlayer
            }
            playCompletion.await()
        } finally {
            synchronized(playerLock) {
                if (vieneuClonePlayer === player && player != null) {
                    releaseVieneuClonePlayer()
                }
            }
        }
    }

    fun stop() {
        tts?.stop()
        releaseValtecAudioTrack()
        releasePresetPreviewPlayer()
        releaseVieneuClonePlayer()
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.25f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun preview(text: String) {
        speak(text)
    }

    private fun cacheKey(text: String, voiceType: String): String {
        return "${voiceType.trim()}|${text.trim().lowercase()}"
    }

    fun shutdown() {
        speakJob?.cancel()
        speakJob = null
        tts?.stop()
        tts?.shutdown()
        releaseValtecAudioTrack()
        releasePresetPreviewPlayer()
        releaseVieneuClonePlayer()
        synchronized(vieneuAudioCache) {
            vieneuAudioCache.values.forEach { it.delete() }
            vieneuAudioCache.clear()
        }
        valtecTtsEngine.release()
        tts = null
        _isReady.value = false
        onCompleteCallback = null
    }

    private fun releaseValtecAudioTrack() {
        synchronized(playerLock) {
            try {
                valtecAudioTrack?.stop()
            } catch (e: Exception) {
                // Ignore stop errors for already released tracks.
            }
            valtecAudioTrack?.release()
            valtecAudioTrack = null
        }
    }

    private fun releasePresetPreviewPlayer() {
        synchronized(playerLock) {
            try {
                presetPreviewPlayer?.stop()
            } catch (e: Exception) {
                // Ignore stop errors for already released players.
            }
            presetPreviewPlayer?.release()
            presetPreviewPlayer = null
        }
    }

    private fun releaseVieneuClonePlayer() {
        synchronized(playerLock) {
            try {
                vieneuClonePlayer?.stop()
            } catch (e: Exception) {
                // Ignore stop errors for already released players.
            }
            vieneuClonePlayer?.release()
            vieneuClonePlayer = null
        }
    }

    private fun ensureSystemTts() {
        if (tts != null) return
        tts = TextToSpeech(context, this)
    }
}
