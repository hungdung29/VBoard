package com.vboard.aac.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vboard.aac.data.local.db.entity.VoiceProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveProfile(): VoiceProfileEntity?

    @Query("SELECT * FROM voice_profiles ORDER BY created_at DESC")
    fun getAllProfiles(): Flow<List<VoiceProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: VoiceProfileEntity)

    @Query("UPDATE voice_profiles SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE voice_profiles SET is_active = :active WHERE id = :id")
    suspend fun activate(id: String, active: Boolean = true)

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM voice_profiles")
    suspend fun getProfileCount(): Int
}
