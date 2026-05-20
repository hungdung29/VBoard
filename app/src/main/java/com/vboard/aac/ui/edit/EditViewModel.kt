package com.vboard.aac.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.model.Category
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.domain.repository.IVocabRepository
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

data class EditUiState(
    val cards: List<VocabCard> = emptyList(),
    val categories: List<Category> = emptyList(),
    val activeCategoryId: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class EditViewModel @Inject constructor(
    private val vocabRepo: IVocabRepository
) : ViewModel() {

    private val _activeCategoryId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EditUiState> = combine(
        vocabRepo.getAllCards(),
        vocabRepo.getAllCategories(),
        _activeCategoryId,
        _isLoading,
        _message
    ) { cards, categories, activeCat, isLoading, message ->
        val filtered = if (activeCat == null) cards else cards.filter { it.categoryId == activeCat }
        EditUiState(
            cards = filtered,
            categories = categories,
            activeCategoryId = activeCat,
            isLoading = isLoading,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EditUiState()
    )

    fun selectCategory(categoryId: String?) {
        _activeCategoryId.value = categoryId
    }

    fun addCard(word: String, categoryId: String, imagePath: String?) {
        if (word.isBlank()) {
            _message.value = "Vui lòng nhập từ"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val card = VocabCard(
                id = UUID.randomUUID().toString(),
                word = word.trim(),
                categoryId = categoryId,
                localImagePath = imagePath,
                isCustom = true,
                displayOrder = 0
            )
            vocabRepo.addCard(card)
            _message.value = "Đã thêm thẻ \"$word\""
            _isLoading.value = false
        }
    }

    fun updateCard(id: String, word: String, categoryId: String, imagePath: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existing = vocabRepo.getCardById(id)
                if (existing != null) {
                    vocabRepo.updateCard(
                        existing.copy(
                            word = word.trim(),
                            categoryId = categoryId,
                            localImagePath = imagePath ?: existing.localImagePath
                        )
                    )
                    _message.value = "Đã cập nhật thẻ"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            vocabRepo.deleteCard(id)
            _message.value = "Đã xóa thẻ"
        }
    }

    fun addCategory(name: String, icon: String, color: String) {
        if (name.isBlank()) {
            _message.value = "Vui lòng nhập tên thư mục"
            return
        }
        viewModelScope.launch {
            val categories = vocabRepo.getAllCategoriesList()
            val category = Category(
                id = "cat-${UUID.randomUUID()}",
                name = name.trim(),
                icon = icon.ifBlank { "📁" },
                color = color.ifBlank { "#7e775f" },
                displayOrder = categories.size
            )
            vocabRepo.addCategory(category)
            _message.value = "Đã tạo thư mục \"$name\""
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            vocabRepo.deleteCategory(id)
            _message.value = "Đã xóa thư mục"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
