package com.vboard.aac.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_usage",
    indices = [Index("usage_date")]
)
data class WordUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "usage_date")
    val usageDate: String,

    @ColumnInfo(name = "count")
    val count: Int = 1
)
