package com.vboard.aac.domain.repository

import com.vboard.aac.domain.model.VoiceProfile

interface IVoiceProfileRepository {
    suspend fun saveProfile(profile: VoiceProfile): Result<Unit>
    suspend fun getActiveProfile(): VoiceProfile?
    suspend fun getAllProfiles(): List<VoiceProfile>
    suspend fun deleteProfile(id: String): Result<Unit>
    suspend fun setActiveProfile(id: String, active: Boolean = true): Result<Unit>
    suspend fun deactivateAll(): Result<Unit>
    suspend fun hasProfile(): Boolean
}
