package com.vboard.aac.platform.voice

interface VoiceCloningEngine {
    suspend fun initialize(mode: InferenceMode)
    suspend fun extractEmbedding(audioPath: String): VoiceEmbedding
    suspend fun synthesize(text: String, embedding: VoiceEmbedding): SynthesizedAudio
    suspend fun release()
    fun isInitialized(): Boolean
}

data class SynthesizedAudio(
    val audioData: ByteArray,
    val format: String,
    val sampleRate: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SynthesizedAudio
        return audioData.contentEquals(other.audioData) &&
               format == other.format &&
               sampleRate == other.sampleRate
    }

    override fun hashCode(): Int {
        var result = audioData.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + sampleRate
        return result
    }
}
