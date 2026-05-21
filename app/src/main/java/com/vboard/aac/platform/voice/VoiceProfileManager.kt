package com.vboard.aac.platform.voice

import com.vboard.aac.domain.model.VoiceProfile
import com.vboard.aac.domain.repository.IVoiceProfileRepository
import com.vboard.aac.platform.audio.AudioQualityAnalyzer
import com.vboard.aac.platform.tts.ValtecTtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates voice recording, embedding extraction, and profile management.
 */
@Singleton
class VoiceProfileManager @Inject constructor(
    private val voiceRecordingManager: VoiceRecordingManager,
    private val valtecTtsEngine: ValtecTtsEngine,
    private val voiceProfileRepository: IVoiceProfileRepository,
    private val audioQualityAnalyzer: AudioQualityAnalyzer
) {
    /**
     * Record voice and create a voice profile
     *
     * @param name Profile name (e.g., "Giọng mẹ")
     * @param onProgress Callback for progress updates
     * @return Result with VoiceProfile on success
     */
    suspend fun createVoiceProfile(
        name: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<VoiceProfile> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Check permission
            if (!voiceRecordingManager.hasRecordPermission()) {
                return@withContext Result.failure(Exception("Microphone permission required"))
            }

            onProgress?.invoke(10)

            // Step 2: Start recording
            val recordResult = voiceRecordingManager.startRecording(name)
            recordResult.onFailure { return@withContext Result.failure(it) }

            // Wait for recording to complete (user stops manually or auto-stops at 10s)
            // For now, return success with placeholder
            // In full implementation, this would be a dialog-based flow

            onProgress?.invoke(30)

            // Step 3: Stop recording
            val stopResult = voiceRecordingManager.stopRecording()
            stopResult.onFailure { return@withContext Result.failure(it) }

            val recording = stopResult.getOrThrow()

            // Step 4: Validate quality
            if (!recording.isValid) {
                return@withContext Result.failure(
                    Exception("Âm thanh không đạt yêu cầu. Vui lòng ghi lại.")
                )
            }

            onProgress?.invoke(50)

            // Step 5: Extract speaker embedding
            if (!valtecTtsEngine.isReady()) {
                // Engine not initialized - save profile without embedding for now
                // Embedding will be extracted when engine is ready
                return@withContext saveProfileWithoutEmbedding(name, recording)
            }

            val embedding = try {
                valtecTtsEngine.extractSpeakerEmbedding(recording.filePath)
            } catch (e: Exception) {
                // Failed to extract embedding
                return@withContext saveProfileWithoutEmbedding(name, recording)
            }

            onProgress?.invoke(70)

            // Step 6: Copy to persistent storage
            val profileId = UUID.randomUUID().toString()
            val persistentPath = voiceRecordingManager.copyToPersistentStorage(
                recording.filePath,
                profileId
            )

            onProgress?.invoke(90)

            // Step 7: Deactivate existing profiles
            voiceProfileRepository.getAllProfiles().forEach { profile ->
                voiceProfileRepository.setActiveProfile(profile.id, false)
            }

            // Step 8: Save new profile
            val profile = VoiceProfile(
                id = profileId,
                name = name,
                createdAt = System.currentTimeMillis(),
                referenceAudioPath = persistentPath,
                speakerEmbedding = embedding,
                isActive = true
            )

            voiceProfileRepository.saveProfile(profile)

            onProgress?.invoke(100)

            Result.success(profile)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveProfileWithoutEmbedding(
        name: String,
        recording: RecordingResult
    ): Result<VoiceProfile> = withContext(Dispatchers.IO) {
        val profileId = UUID.randomUUID().toString()
        val persistentPath = voiceRecordingManager.copyToPersistentStorage(
            recording.filePath,
            profileId
        )

        // Create profile with empty embedding
        val profile = VoiceProfile(
            id = profileId,
            name = name,
            createdAt = System.currentTimeMillis(),
            referenceAudioPath = persistentPath,
            speakerEmbedding = FloatArray(512) { 0f },
            isActive = true
        )

        voiceProfileRepository.saveProfile(profile)
        Result.success(profile)
    }

    /**
     * Get the currently active voice profile
     */
    suspend fun getActiveProfile(): VoiceProfile? {
        return voiceProfileRepository.getActiveProfile()
    }

    /**
     * Delete a voice profile
     */
    suspend fun deleteProfile(profile: VoiceProfile): Result<Unit> {
        return try {
            // Delete audio file
            profile.referenceAudioPath?.let { path ->
                voiceRecordingManager.deletePersistentFile(path)
            }
            // Delete from database
            voiceProfileRepository.deleteProfile(profile.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set a profile as active
     */
    suspend fun setActiveProfile(profileId: String): Result<Unit> {
        return voiceProfileRepository.setActiveProfile(profileId, true)
    }
}
