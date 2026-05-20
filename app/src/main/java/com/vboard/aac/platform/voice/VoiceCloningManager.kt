package com.vboard.aac.platform.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCloningManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCapabilityDetector: DeviceCapabilityDetector
) {
    private var engine: VoiceCloningEngine? = null
    private var currentMode: InferenceMode = InferenceMode.LITE_PIPER

    fun initialize(): InferenceMode {
        currentMode = deviceCapabilityDetector.detectInferenceMode()

        engine = when (currentMode) {
            InferenceMode.FULL_XTTS -> PiperVoiceEngine(context)
            InferenceMode.QUANTIZED_XTTS -> PiperVoiceEngine(context)
            InferenceMode.LITE_PIPER -> PiperVoiceEngine(context)
        }

        runBlocking {
            engine?.initialize(currentMode)
        }

        return currentMode
    }

    fun getEngine(): VoiceCloningEngine? = engine

    fun getCurrentMode(): InferenceMode = currentMode

    fun isSupported(): Boolean = deviceCapabilityDetector.canRunVoiceCloning()

    fun release() {
        runBlocking {
            engine?.release()
        }
        engine = null
    }
}
