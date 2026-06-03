package com.vboard.aac.data.repository

import com.vboard.aac.data.local.db.VoiceProfileDao
import com.vboard.aac.data.local.db.entity.toDomain
import com.vboard.aac.data.local.db.entity.toEntity
import com.vboard.aac.domain.model.VoiceProfile
import com.vboard.aac.domain.repository.IVoiceProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceProfileRepositoryImpl @Inject constructor(
    private val voiceProfileDao: VoiceProfileDao
) : IVoiceProfileRepository {

    override suspend fun saveProfile(profile: VoiceProfile): Result<Unit> = runCatching {
        voiceProfileDao.insert(profile.toEntity())
    }

    override suspend fun getActiveProfile(): VoiceProfile? {
        return voiceProfileDao.getActiveProfile()?.toDomain()
    }

    override suspend fun getAllProfiles(): List<VoiceProfile> {
        return voiceProfileDao.getAllProfiles().map { it.toDomain() }
    }

    override suspend fun deleteProfile(id: String): Result<Unit> = runCatching {
        voiceProfileDao.delete(id)
    }

    override suspend fun setActiveProfile(id: String, active: Boolean): Result<Unit> = runCatching {
        if (active) {
            voiceProfileDao.deactivateAll()
        }
        voiceProfileDao.activate(id, active)
    }

    override suspend fun deactivateAll(): Result<Unit> = runCatching {
        voiceProfileDao.deactivateAll()
    }

    override suspend fun hasProfile(): Boolean {
        return voiceProfileDao.getProfileCount() > 0
    }
}
