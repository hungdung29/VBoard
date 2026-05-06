package com.vboard.aac.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "sentences_count")
    val sentencesCount: Int = 0,

    @ColumnInfo(name = "unique_words")
    val uniqueWords: Int = 0
)
