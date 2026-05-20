package com.vboard.aac.domain.model

data class VoiceProfile(
    val id: String,
    val name: String,
    val createdAt: Long,
    val embeddingPath: String,
    val isActive: Boolean,
    val inferenceMode: String,
    val sampleRate: Int,
    val durationMs: Int,
    val qualityScore: Float
) {
    companion object {
        const val DEFAULT_NAME = "Giọng người thân"
    }
}
