package com.vboard.aac.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vboard.aac.data.local.db.dao.CategoryDao
import com.vboard.aac.data.local.db.dao.StatsDao
import com.vboard.aac.data.local.db.dao.VocabCardDao
import com.vboard.aac.data.local.db.entity.CategoryEntity
import com.vboard.aac.data.local.db.entity.DailyStatsEntity
import com.vboard.aac.data.local.db.entity.VocabCardEntity
import com.vboard.aac.data.local.db.entity.VoiceProfileEntity
import com.vboard.aac.data.local.db.entity.WordUsageEntity

@Database(
    entities = [
        VocabCardEntity::class,
        CategoryEntity::class,
        WordUsageEntity::class,
        DailyStatsEntity::class,
        VoiceProfileEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class VBoardDatabase : RoomDatabase() {
    abstract fun vocabCardDao(): VocabCardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun statsDao(): StatsDao
    abstract fun voiceProfileDao(): VoiceProfileDao

    companion object {
        const val DATABASE_NAME = "vboard_aac.db"
    }
}
