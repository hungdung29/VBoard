package com.vboard.aac.platform.audio

data class AudioRecording(
    val file: java.io.File,
    val durationMs: Long,
    val sampleRate: Int,
    val amplitude: Float  // 0.0 - 1.0 average amplitude
)
