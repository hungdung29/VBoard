package com.vboard.aac.domain.model

data class VocabCard(
    val id: String,
    val word: String,
    val categoryId: String,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val isCustom: Boolean = false,
    val displayOrder: Int = 0
)

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val displayOrder: Int = 0
)

data class SentenceItem(
    val id: String,
    val word: String,
    val cardId: String? = null
)

data class WordUsage(
    val word: String,
    val count: Int,
    val usageDate: String
)

data class DailyStats(
    val date: String,
    val sentencesCount: Int,
    val uniqueWords: Int,
    val topWords: List<WordUsage> = emptyList()
)

data class AppSettings(
    val isDarkMode: Boolean = false,
    val gridColumns: Int = 3,
    val showLabels: Boolean = true,
    val fontScale: Float = 1.0f,
    val pinCode: String = "1234",
    val voiceVolume: Float = 1.0f,
    val voiceType: String = "nam-bac"
)

data class VocabBackup(
    val version: Int = 1,
    val exportedAt: String,
    val categories: List<Category>,
    val cards: List<VocabCard>
)
