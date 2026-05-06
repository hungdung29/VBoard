package com.vboard.aac.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.model.Category
import com.vboard.aac.domain.model.SentenceItem
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.domain.repository.ISettingsRepository
import com.vboard.aac.domain.repository.IStatsRepository
import com.vboard.aac.domain.repository.IVocabRepository
import com.vboard.aac.platform.tts.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * One vocab card's UI projection. Built by [BoardViewModel] from a [VocabCard]
 * + the matching [Category] so that the adapter can render category-aware
 * tint without re-reading the data layer.
 */
data class VocabCardUiItem(
    val id: String,
    val word: String,
    val emoji: String,
    val categoryCode: String,
)

data class BoardUiState(
    val cardUiItems: List<VocabCardUiItem> = emptyList(),
    val cards: List<VocabCard> = emptyList(),
    val categories: List<Category> = emptyList(),
    val sentenceItems: List<SentenceItem> = emptyList(),
    val activeCategoryId: String? = null,
    val gridColumns: Int = 3,
    val showLabels: Boolean = true,
    val ttsReady: Boolean = false,
    val placeholderVisible: Boolean = true,
)

private data class CombinedFlow1(
    val cards: List<VocabCard>,
    val categories: List<Category>,
    val sentences: List<SentenceItem>,
    val activeCat: String?,
    val gridColumns: Int,
)

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val vocabRepo: IVocabRepository,
    private val settingsRepo: ISettingsRepository,
    private val statsRepo: IStatsRepository,
    private val ttsManager: TextToSpeechManager,
) : ViewModel() {

    private val _sentenceItems = MutableStateFlow<List<SentenceItem>>(emptyList())
    private val _activeCategoryId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BoardUiState> = combine(
        combine(
            vocabRepo.getAllCards(),
            vocabRepo.getAllCategories(),
            _sentenceItems,
            _activeCategoryId,
            settingsRepo.gridColumns,
        ) { cards, categories, sentences, activeCat, gridColumns ->
            CombinedFlow1(cards, categories, sentences, activeCat, gridColumns)
        },
        settingsRepo.showLabels,
    ) { combined, showLabels ->
        val (cards, categories, sentences, activeCat, gridColumns) = combined
        val filteredCards = if (activeCat == null) cards
        else cards.filter { it.categoryId == activeCat }

        val categoryById: Map<String, Category> = categories.associateBy { it.id }
        val uiItems = filteredCards.map { card ->
            val category = categoryById[card.categoryId]
            VocabCardUiItem(
                id = card.id,
                word = card.word,
                emoji = category?.icon?.takeIf { it.isNotBlank() }
                    ?: VocabEmojiMap.emojiFor(card.word),
                categoryCode = CATEGORY_ID_TO_CODE[card.categoryId] ?: "none",
            )
        }

        BoardUiState(
            cardUiItems = uiItems,
            cards = filteredCards,
            categories = categories,
            sentenceItems = sentences,
            activeCategoryId = activeCat,
            gridColumns = gridColumns,
            showLabels = showLabels,
            ttsReady = ttsManager.isReady,
            placeholderVisible = sentences.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BoardUiState(ttsReady = ttsManager.isReady),
    )

    init {
        viewModelScope.launch { vocabRepo.seedDefaultData() }
    }

    fun selectCategory(categoryId: String?) {
        _activeCategoryId.value = categoryId
    }

    fun addWordToSentence(item: VocabCardUiItem) {
        _sentenceItems.value = _sentenceItems.value + SentenceItem(
            id = UUID.randomUUID().toString(),
            word = item.word,
            cardId = item.id,
        )
        viewModelScope.launch { statsRepo.recordWordUsage(item.word) }
    }

    /** Backwards-compat overload while non-Board code still passes VocabCard. */
    fun addWordToSentence(card: VocabCard) {
        _sentenceItems.value = _sentenceItems.value + SentenceItem(
            id = UUID.randomUUID().toString(),
            word = card.word,
            cardId = card.id,
        )
        viewModelScope.launch { statsRepo.recordWordUsage(card.word) }
    }

    fun removeLastWord() {
        val current = _sentenceItems.value
        if (current.isNotEmpty()) {
            _sentenceItems.value = current.dropLast(1)
        }
    }

    fun clearSentence() {
        _sentenceItems.value = emptyList()
    }

    fun speakSentence() {
        val words = _sentenceItems.value
        if (words.isEmpty()) return
        val text = words.joinToString(" ") { it.word }
        ttsManager.speak(text) {
            viewModelScope.launch { statsRepo.recordSentence() }
        }
    }

    fun removeWordAt(index: Int) {
        val current = _sentenceItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _sentenceItems.value = current
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    companion object {
        /**
         * Maps the seeded VBoard category IDs to the 6 design-token category
         * codes (food, family, emotion, activity, object, place). Two seed
         * categories share each visual code intentionally — see spec §2.1.
         */
        val CATEGORY_ID_TO_CODE: Map<String, String> = mapOf(
            "cat-1" to "family",
            "cat-2" to "food",
            "cat-3" to "place",     // Nhà cửa
            "cat-4" to "activity",  // Chơi
            "cat-5" to "emotion",
            "cat-6" to "activity",  // Hành động
            "cat-7" to "object",
            "cat-8" to "place",     // Nơi chốn
        )
    }
}

/**
 * Lookup of word → emoji used as a fallback when the underlying [Category]
 * doesn't supply an icon. Kept here (formerly in VocabGridAdapter) because the
 * adapter now consumes pre-built UI items.
 */
internal object VocabEmojiMap {
    fun emojiFor(word: String): String = MAP[word] ?: "📝"

    private val MAP = mapOf(
        "Mẹ" to "👩", "Ba" to "👨", "Em" to "👧", "Ông" to "👴", "Bà" to "👵",
        "Con" to "👶", "Anh" to "🧑", "Chị" to "👩‍🦱",
        "Nước" to "💧", "Cơm" to "🍚", "Bánh" to "🍰", "Sữa" to "🥛",
        "Trái cây" to "🍎", "Thịt" to "🥩", "Cá" to "🐟", "Rau" to "🥬",
        "Trà" to "🍵", "Bánh mì" to "🥖",
        "Nhà" to "🏠", "Phòng" to "🚪", "Giường" to "🛏️", "Cửa" to "🚪",
        "Cửa sổ" to "🪟", "Bếp" to "🍳", "Tivi" to "📺",
        "Chơi" to "🎮", "Đồ chơi" to "🧸", "Bóng" to "⚽", "Sách" to "📖",
        "Đi dạo" to "🚶", "Bơi" to "🏊", "Nhảy" to "🕺", "Hát" to "🎵",
        "Vui" to "😊", "Buồn" to "😢", "Sợ" to "😨", "Mệt" to "😫",
        "Đói" to "😫", "Khát" to "🥤", "Nóng" to "🔥", "Lạnh" to "❄️",
        "Muốn" to "💭", "Cần" to "✋", "Đi" to "🚶", "Ngủ" to "😴",
        "Tắm" to "🚿", "Mặc" to "👕", "Đi học" to "🏫", "Xem" to "👀",
        "Bút" to "✏️", "Giấy" to "📄", "Bảng" to "📋",
        "Điện thoại" to "📱", "Máy tính" to "💻", "Ô tô" to "🚗",
        "Trường" to "🏫", "Bệnh viện" to "🏥", "Công viên" to "🌳",
        "Siêu thị" to "🛒", "Biển" to "🌊",
    )
}
