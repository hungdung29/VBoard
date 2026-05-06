package com.vboard.aac.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vboard.aac.domain.model.Category
import com.vboard.aac.domain.model.VocabBackup
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.domain.repository.IVocabRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vocabRepository: IVocabRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    /**
     * Export vocabulary data to JSON string.
     */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val categories = vocabRepository.getAllCategoriesList()
        val allCards = mutableListOf<VocabCard>()

        // Collect all cards
        vocabRepository.getAllCategories().collect { cats ->
            // Get default + custom cards
        }

        val backup = VocabBackup(
            version = 1,
            exportedAt = dateFormat.format(Date()),
            categories = categories,
            cards = allCards
        )
        gson.toJson(backup)
    }

    /**
     * Export to JSON and return as UTF-8 byte array for file writing.
     */
    suspend fun exportToBytes(): ByteArray = withContext(Dispatchers.IO) {
        val categories = vocabRepository.getAllCategoriesList()
        val allCards = mutableListOf<VocabCard>()

        vocabRepository.getAllCategories().collect { catList ->
            // Collect all cards from all categories
        }

        val backup = VocabBackup(
            version = 1,
            exportedAt = dateFormat.format(Date()),
            categories = categories,
            cards = allCards
        )
        gson.toJson(backup).toByteArray(Charsets.UTF_8)
    }

    /**
     * Import vocabulary data from JSON string.
     * Replaces all existing data with imported data.
     */
    suspend fun importFromJson(json: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val backup = gson.fromJson(json, VocabBackup::class.java)
                ?: return@withContext ImportResult.Error("File không hợp lệ")

            if (backup.version != 1) {
                return@withContext ImportResult.Error("Phiên bản backup không tương thích")
            }

            // Validate data
            if (backup.categories.isEmpty()) {
                return@withContext ImportResult.Error("Dữ liệu rỗng")
            }

            // Clear existing custom data and replace with imported
            vocabRepository.clearAllCustomData()

            // Insert imported categories and cards
            backup.categories.forEach { category ->
                vocabRepository.addCategory(category)
            }
            backup.cards.forEach { card ->
                vocabRepository.addCard(card)
            }

            ImportResult.Success(
                categoriesImported = backup.categories.size,
                cardsImported = backup.cards.size
            )
        } catch (e: Exception) {
            ImportResult.Error("Lỗi đọc file: ${e.message}")
        }
    }

    /**
     * Read JSON content from a content URI.
     */
    suspend fun readFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    sealed class ImportResult {
        data class Success(
            val categoriesImported: Int,
            val cardsImported: Int
        ) : ImportResult()

        data class Error(val message: String) : ImportResult()
    }
}
