package com.vboard.aac.data.repository

import com.vboard.aac.data.local.db.dao.StatsDao
import com.vboard.aac.data.local.db.entity.DailyStatsEntity
import com.vboard.aac.data.local.db.entity.WordUsageEntity
import com.vboard.aac.data.mapper.VocabCardMapper.toDomain
import com.vboard.aac.domain.model.DailyStats
import com.vboard.aac.domain.model.WordUsage
import com.vboard.aac.domain.repository.IStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val statsDao: StatsDao
) : IStatsRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val isoDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun getTodayStats(): Flow<DailyStats> {
        val today = dateFormat.format(Date())
        return statsDao.getStatsByDateFlow(today).map { entity ->
            entity?.let {
                DailyStats(
                    date = it.date,
                    sentencesCount = it.sentencesCount,
                    uniqueWords = it.uniqueWords
                )
            } ?: DailyStats(today, 0, 0)
        }
    }

    override fun getTopWords(limit: Int): Flow<List<WordUsage>> {
        val today = dateFormat.format(Date())
        return statsDao.getTopWords(today, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getWeeklyStats(): Flow<List<DailyStats>> {
        val last7Dates = last7DatesOldestToNewest()
        return statsDao.getRecentStats(7).map { list ->
            val byDate = list.associateBy { it.date }
            last7Dates.map { date ->
                val entry = byDate[date]
                DailyStats(
                    date = date,
                    sentencesCount = entry?.sentencesCount ?: 0,
                    uniqueWords = entry?.uniqueWords ?: 0
                )
            }
        }
    }

    override suspend fun recordSentence() {
        val today = dateFormat.format(Date())
        val existing = statsDao.getStatsByDate(today)
        if (existing == null) {
            statsDao.insertStats(DailyStatsEntity(today, 1, 0))
        } else {
            statsDao.incrementSentenceCount(today)
        }
    }

    override suspend fun recordWordUsage(word: String) {
        val today = dateFormat.format(Date())
        val existing = statsDao.getStatsByDate(today)
        if (existing == null) {
            statsDao.insertStats(DailyStatsEntity(today, 0, 1))
            statsDao.insertWordUsage(WordUsageEntity(word = word, usageDate = today, count = 1))
        } else {
            val wordUsage = statsDao.getWordUsage(word, today)
            if (wordUsage == null) {
                statsDao.insertWordUsage(WordUsageEntity(word = word, usageDate = today, count = 1))
                val uniqueCount = statsDao.getUniqueWordsCount(today)
                statsDao.updateUniqueWords(today, uniqueCount)
            } else {
                statsDao.incrementWordCount(word, today)
            }
        }
    }

    override suspend fun clearAllStats() {
        statsDao.clearAllStats()
        statsDao.clearAllWordUsage()
    }

    private fun last7DatesOldestToNewest(): List<String> {
        val today = LocalDate.now()
        return (6 downTo 0).map { daysAgo ->
            today.minusDays(daysAgo.toLong()).format(isoDateFormat)
        }
    }
}