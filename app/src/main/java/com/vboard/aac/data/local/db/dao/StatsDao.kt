package com.vboard.aac.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vboard.aac.data.local.db.entity.DailyStatsEntity
import com.vboard.aac.data.local.db.entity.WordUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsByDate(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun getStatsByDateFlow(date: String): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :days")
    fun getRecentStats(days: Int): Flow<List<DailyStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: DailyStatsEntity)

    @Update
    suspend fun updateStats(stats: DailyStatsEntity)

    @Query("UPDATE daily_stats SET sentences_count = sentences_count + 1 WHERE date = :date")
    suspend fun incrementSentenceCount(date: String)

    @Query("UPDATE daily_stats SET unique_words = :count WHERE date = :date")
    suspend fun updateUniqueWords(date: String, count: Int)

    // Word usage
    @Query("SELECT * FROM word_usage WHERE usage_date = :date ORDER BY count DESC LIMIT :limit")
    fun getTopWords(date: String, limit: Int): Flow<List<WordUsageEntity>>

    @Query("SELECT * FROM word_usage WHERE word = :word AND usage_date = :date LIMIT 1")
    suspend fun getWordUsage(word: String, date: String): WordUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordUsage(usage: WordUsageEntity)

    @Query("UPDATE word_usage SET count = count + 1 WHERE word = :word AND usage_date = :date")
    suspend fun incrementWordCount(word: String, date: String)

    @Query("SELECT COUNT(DISTINCT word) FROM word_usage WHERE usage_date = :date")
    suspend fun getUniqueWordsCount(date: String): Int

    @Query("DELETE FROM daily_stats")
    suspend fun clearAllStats()

    @Query("DELETE FROM word_usage")
    suspend fun clearAllWordUsage()
}
