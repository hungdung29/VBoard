package com.vboard.aac.domain.repository

import com.vboard.aac.domain.model.DailyStats
import com.vboard.aac.domain.model.WordUsage
import kotlinx.coroutines.flow.Flow

interface IStatsRepository {
    fun getTodayStats(): Flow<DailyStats>
    fun getTopWords(limit: Int): Flow<List<WordUsage>>
    fun getWeeklyStats(): Flow<List<DailyStats>>
    suspend fun recordSentence()
    suspend fun recordWordUsage(word: String)
    suspend fun clearAllStats()
}
