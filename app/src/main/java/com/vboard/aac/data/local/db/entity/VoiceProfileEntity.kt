package com.vboard.aac.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    // Reference to the original audio file (10 seconds)
    @ColumnInfo(name = "reference_audio_path")
    val referenceAudioPath: String?,

    // Speaker embedding as Base64-encoded JSON array
    // This is the 512-dim embedding extracted from reference audio
    @ColumnInfo(name = "speaker_embedding")
    val speakerEmbedding: String,

    // Whether this profile is currently active
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,

    // Valtec-TTS model version
    @ColumnInfo(name = "model_version")
    val modelVersion: String = "1.0"
)

private val gson = Gson()

fun VoiceProfileEntity.toDomain(): com.vboard.aac.domain.model.VoiceProfile =
    com.vboard.aac.domain.model.VoiceProfile(
        id = id,
        name = name,
        createdAt = createdAt,
        referenceAudioPath = referenceAudioPath,
        speakerEmbedding = decodeEmbedding(speakerEmbedding),
        isActive = isActive,
        modelVersion = modelVersion
    )

fun com.vboard.aac.domain.model.VoiceProfile.toEntity(): VoiceProfileEntity = VoiceProfileEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    referenceAudioPath = referenceAudioPath,
    speakerEmbedding = encodeEmbedding(speakerEmbedding),
    isActive = isActive,
    modelVersion = modelVersion
)

private fun encodeEmbedding(embedding: FloatArray): String {
    return gson.toJson(embedding.toList())
}

private fun decodeEmbedding(json: String): FloatArray {
    val list = gson.fromJson(json, List::class.java)
    return list.map { (it as Number).toFloat() }.toFloatArray()
}
