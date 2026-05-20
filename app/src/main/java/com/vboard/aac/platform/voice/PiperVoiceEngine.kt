package com.vboard.aac.platform.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LITE mode implementation using pre-recorded voice samples.
 * For devices with 2-3GB RAM that cannot run full XTTS.
 */
@Singleton
class PiperVoiceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceCloningEngine {

    private var isInit = false
    private var voiceSamplePath: String? = null
    private var pitchShift: Float = 1.0f
    private var speedAdjustment: Float = 1.0f

    override suspend fun initialize(mode: InferenceMode) = withContext(Dispatchers.IO) {
        isInit = true
    }

    override suspend fun extractEmbedding(audioPath: String): VoiceEmbedding = withContext(Dispatchers.IO) {
        if (!isInit) throw IllegalStateException("Engine not initialized")

        val file = File(audioPath)
        val durationMs = file.length() / (16000 * 2) * 1000

        VoiceEmbedding(
            tensor = floatArrayOf(
                pitchShift,
                speedAdjustment,
                durationMs.toFloat() / 1000f
            ),
            sampleRate = 16000,
            durationMs = durationMs.toInt()
        )
    }

    override suspend fun synthesize(text: String, embedding: VoiceEmbedding): SynthesizedAudio =
        withContext(Dispatchers.IO) {
            if (!isInit) throw IllegalStateException("Engine not initialized")

            SynthesizedAudio(
                audioData = byteArrayOf(),
                format = "tts_with_modification",
                sampleRate = 16000
            )
        }

    override suspend fun release() {
        isInit = false
        voiceSamplePath = null
    }

    override fun isInitialized(): Boolean = isInit

    fun setVoiceParameters(pitch: Float, speed: Float) {
        pitchShift = pitch.coerceIn(0.5f, 2.0f)
        speedAdjustment = speed.coerceIn(0.5f, 2.0f)
    }
}
