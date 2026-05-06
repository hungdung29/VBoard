package com.vboard.aac.domain.repository

import com.vboard.aac.domain.model.Category
import com.vboard.aac.domain.model.VocabCard
import kotlinx.coroutines.flow.Flow

interface IVocabRepository {
    fun getAllCards(): Flow<List<VocabCard>>
    fun getCardsByCategory(categoryId: String): Flow<List<VocabCard>>
    suspend fun getCardById(id: String): VocabCard?
    suspend fun addCard(card: VocabCard)
    suspend fun addCards(cards: List<VocabCard>)
    suspend fun updateCard(card: VocabCard)
    suspend fun deleteCard(id: String)
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getAllCategoriesList(): List<Category>
    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: String)
    suspend fun seedDefaultData()
    suspend fun clearAllCustomData()
}
