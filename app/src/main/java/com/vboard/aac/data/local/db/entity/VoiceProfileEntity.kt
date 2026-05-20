package com.vboard.aac.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    val embeddingPath: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    val inferenceMode: String,
    val sampleRate: Int,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Int,
    @ColumnInfo(name = "quality_score")
    val qualityScore: Float
)

fun VoiceProfileEntity.toDomain(): com.vboard.aac.domain.model.VoiceProfile =
    com.vboard.aac.domain.model.VoiceProfile(
        id = id,
        name = name,
        createdAt = createdAt,
        embeddingPath = embeddingPath,
        isActive = isActive,
        inferenceMode = inferenceMode,
        sampleRate = sampleRate,
        durationMs = durationMs,
        qualityScore = qualityScore
    )

fun com.vboard.aac.domain.model.VoiceProfile.toEntity(): VoiceProfileEntity = VoiceProfileEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    embeddingPath = embeddingPath,
    isActive = isActive,
    inferenceMode = inferenceMode,
    sampleRate = sampleRate,
    durationMs = durationMs,
    qualityScore = qualityScore
)
