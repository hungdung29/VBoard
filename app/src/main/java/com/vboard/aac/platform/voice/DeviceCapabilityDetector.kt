package com.vboard.aac.platform.voice

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun detectInferenceMode(): InferenceMode {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)

        return when {
            totalRamGb >= 4.0 -> InferenceMode.FULL_XTTS
            totalRamGb >= 3.0 -> InferenceMode.QUANTIZED_XTTS
            else -> InferenceMode.LITE_PIPER
        }
    }

    fun getAvailableMemoryMb(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    fun canRunVoiceCloning(): Boolean {
        return getAvailableMemoryMb() >= 500  // At least 500MB available
    }
}
