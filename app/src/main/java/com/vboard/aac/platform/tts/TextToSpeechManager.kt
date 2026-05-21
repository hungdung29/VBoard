package com.vboard.aac.platform.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.vboard.aac.domain.repository.ISettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: ISettingsRepository
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var onCompleteCallback: (() -> Unit)? = null
    private var speechRate = 0.4f
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        tts = TextToSpeech(context, this)
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
        if (_isReady.value.not() || text.isBlank()) return

        onCompleteCallback = onComplete
        val utteranceId = "vboard_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.25f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun preview(text: String) {
        speak(text)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        onCompleteCallback = null
    }
}
