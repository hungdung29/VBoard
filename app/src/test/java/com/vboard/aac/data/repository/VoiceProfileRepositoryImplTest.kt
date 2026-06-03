package com.vboard.aac.data.repository

import com.vboard.aac.data.local.db.VoiceProfileDao
import com.vboard.aac.data.local.db.entity.VoiceProfileEntity
import com.vboard.aac.domain.model.VoiceProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceProfileRepositoryImplTest {

    private lateinit var fakeDao: FakeVoiceProfileDao
    private lateinit var repository: VoiceProfileRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeVoiceProfileDao()
        repository = VoiceProfileRepositoryImpl(fakeDao)
    }

    @Test
    fun testSaveAndActivateMultipleProfiles() = runBlocking {
        // 1. Create first profile
        val profile1 = VoiceProfile(
            id = "id-1",
            name = "Profile 1",
            createdAt = 1000L,
            referenceAudioPath = "/path/1",
            speakerEmbedding = FloatArray(512) { 1.0f },
            isActive = true
        )

        repository.saveProfile(profile1).getOrThrow()

        // Verify first profile is active
        val active1 = repository.getActiveProfile()
        assertEquals("id-1", active1?.id)
        assertTrue(active1?.isActive == true)

        // 2. Simulate deactivating existing profiles (as done in saveRecordingAsProfile)
        repository.deactivateAll().getOrThrow()

        // Verify no active profiles
        assertNull(repository.getActiveProfile())

        // 3. Create second profile and save it
        val profile2 = VoiceProfile(
            id = "id-2",
            name = "Profile 2",
            createdAt = 2000L,
            referenceAudioPath = "/path/2",
            speakerEmbedding = FloatArray(512) { 2.0f },
            isActive = true
        )

        repository.saveProfile(profile2).getOrThrow()

        // Verify second profile is active
        val active2 = repository.getActiveProfile()
        assertEquals("id-2", active2?.id)
        assertTrue(active2?.isActive == true)

        // Verify first profile is inactive
        val allAfter = repository.getAllProfiles()
        assertEquals(2, allAfter.size)
        val p1 = allAfter.first { it.id == "id-1" }
        assertFalse(p1.isActive)
        val p2 = allAfter.first { it.id == "id-2" }
        assertTrue(p2.isActive)
    }

    class FakeVoiceProfileDao : VoiceProfileDao {
        private val profiles = mutableMapOf<String, VoiceProfileEntity>()

        override suspend fun getActiveProfile(): VoiceProfileEntity? {
            return profiles.values.firstOrNull { it.isActive }
        }

        override suspend fun getAllProfiles(): List<VoiceProfileEntity> {
            return profiles.values.sortedByDescending { it.createdAt }
        }

        override suspend fun insert(profile: VoiceProfileEntity) {
            profiles[profile.id] = profile
        }

        override suspend fun deactivateAll() {
            profiles.forEach { (id, profile) ->
                profiles[id] = profile.copy(isActive = false)
            }
        }

        override suspend fun activate(id: String, active: Boolean) {
            profiles[id]?.let {
                profiles[id] = it.copy(isActive = active)
            }
        }

        override suspend fun delete(id: String) {
            profiles.remove(id)
        }

        override suspend fun getProfileCount(): Int {
            return profiles.size
        }
    }
}
