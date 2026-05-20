package com.vboard.aac.platform.voice

data class VoiceEmbedding(
    val tensor: FloatArray,
    val sampleRate: Int,
    val durationMs: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceEmbedding
        return tensor.contentEquals(other.tensor) &&
               sampleRate == other.sampleRate &&
               durationMs == other.durationMs
    }

    override fun hashCode(): Int {
        var result = tensor.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + durationMs
        return result
    }
}
