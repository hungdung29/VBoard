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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BoardUiState(
    val cards: List<VocabCard> = emptyList(),
    val categories: List<Category> = emptyList(),
    val sentenceItems: List<SentenceItem> = emptyList(),
    val activeCategoryId: String? = null,
    val gridColumns: Int = 3,
    val showLabels: Boolean = true,
    val ttsReady: Boolean = false,
    val placeholderVisible: Boolean = true
)

private data class CombinedFlow(
    val cards: List<VocabCard>,
    val categories: List<Category>,
    val sentences: List<SentenceItem>,
    val activeCat: String?,
    val gridColumns: Int
)

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val vocabRepo: IVocabRepository,
    private val settingsRepo: ISettingsRepository,
    private val statsRepo: IStatsRepository,
    private val ttsManager: TextToSpeechManager
) : ViewModel() {

    private val _sentenceItems = MutableStateFlow<List<SentenceItem>>(emptyList())
    private val _activeCategoryId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BoardUiState> = combine(
        combine(
            vocabRepo.getAllCards(),
            vocabRepo.getAllCategories(),
            _sentenceItems,
            _activeCategoryId,
            settingsRepo.gridColumns
        ) { cards, categories, sentences, activeCat, gridColumns ->
            CombinedFlow(cards, categories, sentences, activeCat, gridColumns)
        },
        settingsRepo.showLabels,
        ttsManager.isReady
    ) { combined, showLabels, ttsReady ->
        val (cards, categories, sentences, activeCat, gridColumns) = combined
        val filteredCards = if (activeCat == null) {
            cards
        } else {
            cards.filter { it.categoryId == activeCat }
        }
        BoardUiState(
            cards = filteredCards,
            categories = categories,
            sentenceItems = sentences,
            activeCategoryId = activeCat,
            gridColumns = gridColumns,
            showLabels = showLabels,
            ttsReady = ttsReady,
            placeholderVisible = sentences.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BoardUiState()
    )

    init {
        viewModelScope.launch {
            vocabRepo.seedDefaultData()
        }
    }

    fun selectCategory(categoryId: String?) {
        _activeCategoryId.value = categoryId
    }

    fun addWordToSentence(card: VocabCard) {
        val item = SentenceItem(
            id = UUID.randomUUID().toString(),
            word = card.word,
            cardId = card.id
        )
        _sentenceItems.value = _sentenceItems.value + item

        viewModelScope.launch {
            statsRepo.recordWordUsage(card.word)
        }
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
            viewModelScope.launch {
                statsRepo.recordSentence()
            }
        }
    }

    fun removeWordAt(index: Int) {
        val current = _sentenceItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _sentenceItems.value = current
        }
    }
}
