package com.vboard.aac.platform.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Records a mono PCM WAV sample that can be uploaded directly to VieNeu.
 */
@Singleton
class VoiceRecordingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val TARGET_DURATION_MS = 5_000L
        const val MIN_DURATION_MS = 3_000L
        const val SAMPLE_RATE = 16000
        const val MIN_AMPLITUDE = 0.1f
        const val MAX_AMPLITUDE = 0.95f
        private const val CHANNEL_COUNT = 1
        private const val BITS_PER_SAMPLE = 16
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var outputFile: File? = null
    private var recordingStartTime: Long = 0
    @Volatile private var recording = false
    @Volatile private var maxAmplitudeSeen: Float = 0f
    @Volatile private var recordingFailure: Throwable? = null

    fun hasRecordPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(name: String): Result<String> {
        if (!hasRecordPermission()) {
            return Result.failure(SecurityException("Microphone permission required"))
        }
        if (recording) {
            return Result.failure(IllegalStateException("Recording already in progress"))
        }

        return runCatching {
            outputFile?.delete()
            outputFile = null

            val safeName = name.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(context.cacheDir, "voice_${safeName}_${System.currentTimeMillis()}.wav")
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(SAMPLE_RATE / 2)

            val recorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .build()

            check(recorder.state == AudioRecord.STATE_INITIALIZED) {
                "Unable to initialize microphone"
            }

            writeEmptyWavHeader(file)
            outputFile = file
            audioRecord = recorder
            recorder.startRecording()
            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "Unable to start microphone"
            }

            maxAmplitudeSeen = 0f
            recordingFailure = null
            recordingStartTime = System.currentTimeMillis()
            recording = true
            recordingThread = Thread(
                {
                    runCatching {
                        appendAudioSamples(recorder, file, minBufferSize)
                    }.onFailure { error ->
                        recordingFailure = error
                    }
                },
                "VieNeuReferenceRecorder"
            ).also { it.start() }

            file.absolutePath
        }.onFailure {
            cancelRecording()
        }
    }

    fun stopRecording(): Result<RecordingResult> {
        if (!recording) {
            return Result.failure(IllegalStateException("No recording in progress"))
        }

        return runCatching {
            val duration = System.currentTimeMillis() - recordingStartTime
            stopRecorder()
            val file = outputFile ?: error("No recording file")
            recordingFailure?.let { throw IllegalStateException("Unable to record microphone audio", it) }
            finalizeWavHeader(file)

            if (duration < MIN_DURATION_MS) {
                file.delete()
                error("Recording too short")
            }

            val amplitude = maxAmplitudeSeen
            RecordingResult(
                filePath = file.absolutePath,
                durationMs = duration,
                amplitude = amplitude,
                isValid = amplitude in MIN_AMPLITUDE..MAX_AMPLITUDE
            )
        }.onFailure {
            cancelRecording()
        }
    }

    fun cancelRecording() {
        stopRecorder()
        outputFile?.delete()
        outputFile = null
        maxAmplitudeSeen = 0f
        recordingFailure = null
    }

    fun getCurrentAmplitude(): Float = maxAmplitudeSeen

    fun isRecording(): Boolean = recording

    suspend fun copyToPersistentStorage(sourcePath: String, profileId: String): String =
        withContext(Dispatchers.IO) {
            val source = File(sourcePath)
            val destDir = File(context.filesDir, "voice_profiles")
            destDir.mkdirs()

            val dest = File(destDir, "${profileId}_reference.wav")
            source.copyTo(dest, overwrite = true)
            source.delete()
            dest.absolutePath
        }

    suspend fun deletePersistentFile(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).delete()
    }

    private fun appendAudioSamples(recorder: AudioRecord, file: File, bufferSize: Int) {
        val samples = ShortArray(bufferSize / 2)
        DataOutputStream(FileOutputStream(file, true)).use { output ->
            while (recording) {
                val count = recorder.read(samples, 0, samples.size)
                if (count <= 0) continue
                var max = 0
                for (index in 0 until count) {
                    val sample = samples[index].toInt()
                    val amplitude = if (sample == Short.MIN_VALUE.toInt()) 32767 else abs(sample)
                    max = maxOf(max, amplitude)
                    output.writeByte(sample and 0xff)
                    output.writeByte(sample shr 8 and 0xff)
                }
                maxAmplitudeSeen = maxOf(maxAmplitudeSeen, (max / 32767f).coerceIn(0f, 1f))
            }
        }
    }

    private fun stopRecorder() {
        recording = false
        val recorder = audioRecord
        try {
            recorder?.stop()
        } catch (_: IllegalStateException) {
            // Recorder may already be stopped.
        }
        try {
            recordingThread?.join(2000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        recordingThread = null
        recorder?.release()
        audioRecord = null
    }

    private fun writeEmptyWavHeader(file: File) {
        DataOutputStream(FileOutputStream(file)).use { output ->
            output.writeBytes("RIFF")
            output.writeIntLE(0)
            output.writeBytes("WAVE")
            output.writeBytes("fmt ")
            output.writeIntLE(16)
            output.writeShortLE(1)
            output.writeShortLE(CHANNEL_COUNT)
            output.writeIntLE(SAMPLE_RATE)
            output.writeIntLE(SAMPLE_RATE * CHANNEL_COUNT * BITS_PER_SAMPLE / 8)
            output.writeShortLE(CHANNEL_COUNT * BITS_PER_SAMPLE / 8)
            output.writeShortLE(BITS_PER_SAMPLE)
            output.writeBytes("data")
            output.writeIntLE(0)
        }
    }

    private fun finalizeWavHeader(file: File) {
        val audioSize = (file.length() - 44).coerceAtLeast(0)
        RandomAccessFile(file, "rw").use { wav ->
            wav.seek(4)
            wav.writeIntLE((audioSize + 36).toInt())
            wav.seek(40)
            wav.writeIntLE(audioSize.toInt())
        }
    }

    private fun DataOutputStream.writeShortLE(value: Int) {
        writeByte(value and 0xff)
        writeByte(value shr 8 and 0xff)
    }

    private fun DataOutputStream.writeIntLE(value: Int) {
        writeByte(value and 0xff)
        writeByte(value shr 8 and 0xff)
        writeByte(value shr 16 and 0xff)
        writeByte(value shr 24 and 0xff)
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        writeByte(value and 0xff)
        writeByte(value shr 8 and 0xff)
        writeByte(value shr 16 and 0xff)
        writeByte(value shr 24 and 0xff)
    }
}

data class RecordingResult(
    val filePath: String,
    val durationMs: Long,
    val amplitude: Float,
    val isValid: Boolean
)
