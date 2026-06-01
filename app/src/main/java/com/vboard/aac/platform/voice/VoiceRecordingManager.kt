package com.vboard.aac.platform.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages voice recording for voice cloning.
 * Records 10-second audio samples for Valtec-TTS speaker embedding extraction.
 */
@Singleton
class VoiceRecordingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val TARGET_DURATION_MS = 10_000L // 10 seconds
        const val SAMPLE_RATE = 16000
        const val MIN_AMPLITUDE = 0.1f
        const val MAX_AMPLITUDE = 0.95f
    }

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingStartTime: Long = 0
    private var maxAmplitudeSeen: Float = 0f

    /**
     * Check if app has microphone permission
     */
    fun hasRecordPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start recording a voice sample
     *
     * @param name Name for the voice profile
     * @return Result with file path on success
     */
    fun startRecording(name: String): Result<String> {
        return try {
            val filename = "voice_${name}_${System.currentTimeMillis()}"
            outputFile = File(context.cacheDir, "$filename.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioChannels(1)
                setAudioEncodingBitRate(64000)
                setOutputFile(outputFile!!.absolutePath)
                setMaxDuration(TARGET_DURATION_MS.toInt())
                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            maxAmplitudeSeen = 0f
            Result.success(outputFile!!.absolutePath)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop recording and return the recorded file
     *
     * @return Result with RecordingResult
     */
    fun stopRecording(): Result<RecordingResult> {
        return try {
            val duration = System.currentTimeMillis() - recordingStartTime

            val amplitude = getCurrentAmplitude().coerceAtLeast(maxAmplitudeSeen)

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val file = outputFile ?: throw IllegalStateException("No recording file")

            if (duration < 3000) {
                // Too short
                file.delete()
                return Result.failure(Exception("Recording too short"))
            }

            Result.success(
                RecordingResult(
                    filePath = file.absolutePath,
                    durationMs = duration,
                    amplitude = amplitude,
                    isValid = amplitude in MIN_AMPLITUDE..MAX_AMPLITUDE
                )
            )

        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            outputFile?.delete()
            Result.failure(e)
        }
    }

    /**
     * Cancel recording and delete the file
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
        outputFile?.delete()
        outputFile = null
        maxAmplitudeSeen = 0f
    }

    /**
     * Get current amplitude (0.0 - 1.0)
     */
    fun getCurrentAmplitude(): Float {
        return try {
            val maxAmp = mediaRecorder?.maxAmplitude ?: 0
            val current = (maxAmp / 32767f).coerceIn(0f, 1f)
            if (current > maxAmplitudeSeen) {
                maxAmplitudeSeen = current
            }
            current
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Get max amplitude during recording
     */
    private fun getMaxAmplitude(): Float {
        return maxAmplitudeSeen.coerceAtLeast(getCurrentAmplitude())
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = mediaRecorder != null

    /**
     * Copy recording to persistent storage
     *
     * @param sourcePath Source file path
     * @param profileId Profile ID for naming
     * @return Path to persistent file
     */
    suspend fun copyToPersistentStorage(sourcePath: String, profileId: String): String = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        val destDir = File(context.filesDir, "voice_profiles")
        destDir.mkdirs()

        val dest = File(destDir, "${profileId}_reference.m4a")
        source.copyTo(dest, overwrite = true)

        // Delete original
        source.delete()

        dest.absolutePath
    }

    /**
     * Delete persistent voice file
     */
    suspend fun deletePersistentFile(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).delete()
    }
}

/**
 * Result of a voice recording
 */
data class RecordingResult(
    val filePath: String,
    val durationMs: Long,
    val amplitude: Float,
    val isValid: Boolean
)
