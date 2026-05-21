package com.vboard.aac.domain.model

data class VoiceProfile(
    val id: String,
    val name: String,
    val createdAt: Long,
    val referenceAudioPath: String?,
    val speakerEmbedding: FloatArray,
    val isActive: Boolean,
    val modelVersion: String = "1.0"
) {
    companion object {
        const val DEFAULT_NAME = "Giọng người thân"
        const val SPEAKER_EMBEDDING_DIM = 512
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceProfile

        if (id != other.id) return false
        if (name != other.name) return false
        if (createdAt != other.createdAt) return false
        if (referenceAudioPath != other.referenceAudioPath) return false
        if (!speakerEmbedding.contentEquals(other.speakerEmbedding)) return false
        if (isActive != other.isActive) return false
        if (modelVersion != other.modelVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (referenceAudioPath?.hashCode() ?: 0)
        result = 31 * result + speakerEmbedding.contentHashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + modelVersion.hashCode()
        return result
    }
}
