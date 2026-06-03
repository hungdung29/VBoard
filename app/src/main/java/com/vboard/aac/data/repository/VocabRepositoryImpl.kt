package com.vboard.aac.data.repository

import com.vboard.aac.data.local.db.dao.CategoryDao
import com.vboard.aac.data.local.db.dao.VocabCardDao
import com.vboard.aac.data.mapper.VocabCardMapper.toDomain
import com.vboard.aac.data.mapper.VocabCardMapper.toEntity
import com.vboard.aac.domain.model.Category
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.domain.repository.IVocabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabRepositoryImpl @Inject constructor(
    private val vocabCardDao: VocabCardDao,
    private val categoryDao: CategoryDao
) : IVocabRepository {

    override fun getAllCards(): Flow<List<VocabCard>> =
        vocabCardDao.getAllCards().map { list -> list.map { it.toDomain() } }

    override fun getCardsByCategory(categoryId: String): Flow<List<VocabCard>> =
        vocabCardDao.getCardsByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override suspend fun getCardById(id: String): VocabCard? =
        vocabCardDao.getCardById(id)?.toDomain()

    override suspend fun addCard(card: VocabCard) {
        vocabCardDao.insertCard(card.toEntity())
    }

    override suspend fun addCards(cards: List<VocabCard>) {
        vocabCardDao.insertCards(cards.map { it.toEntity() })
    }

    override suspend fun updateCard(card: VocabCard) {
        vocabCardDao.updateCard(card.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteCard(id: String) {
        vocabCardDao.deleteCardById(id)
    }

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllCategoriesList(): List<Category> =
        categoryDao.getAllCategoriesList().map { it.toDomain() }

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    override suspend fun seedDefaultData() {
        if (vocabCardDao.getCardCount() == 0) {
            categoryDao.insertCategories(DEFAULT_CATEGORIES.map { it.toEntity() })
            vocabCardDao.insertCards(DEFAULT_CARDS.map { it.toEntity() })
        } else if (vocabCardDao.getCardById(PAIN_CARD.id) == null) {
            vocabCardDao.shiftDisplayOrdersFrom(PAIN_CARD.displayOrder)
            vocabCardDao.insertCard(PAIN_CARD.toEntity())
        }
    }

    override suspend fun clearAllCustomData() {
        vocabCardDao.deleteDefaultCards()
    }

    companion object {
        private val PAIN_CARD = VocabCard("c61", "Đau", "cat-5", displayOrder = 41)

        val DEFAULT_CATEGORIES = listOf(
            Category("cat-1", "Gia đình", "👨‍👩‍👧", "#FF6B6B", 1),
            Category("cat-2", "Ăn uống", "🍎", "#4ECDC4", 2),
            Category("cat-3", "Nhà cửa", "🏠", "#45B7D1", 3),
            Category("cat-4", "Chơi", "🎮", "#96CEB4", 4),
            Category("cat-5", "Cảm xúc", "😢", "#FFEAA7", 5),
            Category("cat-6", "Hành động", "👋", "#DDA0DD", 6),
            Category("cat-7", "Đồ vật", "📦", "#F7DC6F", 7),
            Category("cat-8", "Nơi chốn", "📍", "#98D8C8", 8)
        )

        val DEFAULT_CARDS = listOf(
            // Gia đình
            VocabCard("c1", "Mẹ", "cat-1", displayOrder = 0),
            VocabCard("c2", "Ba", "cat-1", displayOrder = 1),
            VocabCard("c3", "Em", "cat-1", displayOrder = 2),
            VocabCard("c4", "Ông", "cat-1", displayOrder = 3),
            VocabCard("c5", "Bà", "cat-1", displayOrder = 4),
            VocabCard("c6", "Con", "cat-1", displayOrder = 5),
            VocabCard("c7", "Anh", "cat-1", displayOrder = 6),
            VocabCard("c8", "Chị", "cat-1", displayOrder = 7),
            // Ăn uống
            VocabCard("c9", "Nước", "cat-2", displayOrder = 8),
            VocabCard("c10", "Cơm", "cat-2", displayOrder = 9),
            VocabCard("c11", "Bánh", "cat-2", displayOrder = 10),
            VocabCard("c12", "Sữa", "cat-2", displayOrder = 11),
            VocabCard("c13", "Trái cây", "cat-2", displayOrder = 12),
            VocabCard("c14", "Thịt", "cat-2", displayOrder = 13),
            VocabCard("c15", "Cá", "cat-2", displayOrder = 14),
            VocabCard("c16", "Rau", "cat-2", displayOrder = 15),
            VocabCard("c17", "Trà", "cat-2", displayOrder = 16),
            VocabCard("c18", "Bánh mì", "cat-2", displayOrder = 17),
            // Nhà cửa
            VocabCard("c19", "Nhà", "cat-3", displayOrder = 18),
            VocabCard("c20", "Phòng", "cat-3", displayOrder = 19),
            VocabCard("c21", "Giường", "cat-3", displayOrder = 20),
            VocabCard("c22", "Cửa", "cat-3", displayOrder = 21),
            VocabCard("c23", "Cửa sổ", "cat-3", displayOrder = 22),
            VocabCard("c24", "Bếp", "cat-3", displayOrder = 23),
            VocabCard("c25", "Tivi", "cat-3", displayOrder = 24),
            // Chơi
            VocabCard("c26", "Chơi", "cat-4", displayOrder = 25),
            VocabCard("c27", "Đồ chơi", "cat-4", displayOrder = 26),
            VocabCard("c28", "Bóng", "cat-4", displayOrder = 27),
            VocabCard("c29", "Sách", "cat-4", displayOrder = 28),
            VocabCard("c30", "Đi dạo", "cat-4", displayOrder = 29),
            VocabCard("c31", "Bơi", "cat-4", displayOrder = 30),
            VocabCard("c32", "Nhảy", "cat-4", displayOrder = 31),
            VocabCard("c33", "Hát", "cat-4", displayOrder = 32),
            // Cảm xúc
            VocabCard("c34", "Vui", "cat-5", displayOrder = 33),
            VocabCard("c35", "Buồn", "cat-5", displayOrder = 34),
            VocabCard("c36", "Sợ", "cat-5", displayOrder = 35),
            VocabCard("c37", "Mệt", "cat-5", displayOrder = 36),
            VocabCard("c38", "Đói", "cat-5", displayOrder = 37),
            VocabCard("c39", "Khát", "cat-5", displayOrder = 38),
            VocabCard("c40", "Nóng", "cat-5", displayOrder = 39),
            VocabCard("c41", "Lạnh", "cat-5", displayOrder = 40),
            PAIN_CARD,
            // Hành động
            VocabCard("c42", "Muốn", "cat-6", displayOrder = 42),
            VocabCard("c43", "Cần", "cat-6", displayOrder = 43),
            VocabCard("c44", "Đi", "cat-6", displayOrder = 44),
            VocabCard("c45", "Ngủ", "cat-6", displayOrder = 45),
            VocabCard("c46", "Tắm", "cat-6", displayOrder = 46),
            VocabCard("c47", "Mặc", "cat-6", displayOrder = 47),
            VocabCard("c48", "Đi học", "cat-6", displayOrder = 48),
            VocabCard("c49", "Xem", "cat-6", displayOrder = 49),
            // Đồ vật
            VocabCard("c50", "Bút", "cat-7", displayOrder = 50),
            VocabCard("c51", "Giấy", "cat-7", displayOrder = 51),
            VocabCard("c52", "Bảng", "cat-7", displayOrder = 52),
            VocabCard("c53", "Điện thoại", "cat-7", displayOrder = 53),
            VocabCard("c54", "Máy tính", "cat-7", displayOrder = 54),
            VocabCard("c55", "Ô tô", "cat-7", displayOrder = 55),
            // Nơi chốn
            VocabCard("c56", "Trường", "cat-8", displayOrder = 56),
            VocabCard("c57", "Bệnh viện", "cat-8", displayOrder = 57),
            VocabCard("c58", "Công viên", "cat-8", displayOrder = 58),
            VocabCard("c59", "Siêu thị", "cat-8", displayOrder = 59),
            VocabCard("c60", "Biển", "cat-8", displayOrder = 60)
        )
    }
}
