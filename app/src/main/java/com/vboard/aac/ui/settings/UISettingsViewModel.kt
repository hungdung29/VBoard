package com.vboard.aac.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.model.AppSettings
import com.vboard.aac.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UISettingsViewModel @Inject constructor(
    private val settingsRepo: ISettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.appSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setDarkMode(enabled)
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            settingsRepo.setGridColumns(columns)
        }
    }

    fun setShowLabels(show: Boolean) {
        viewModelScope.launch {
            settingsRepo.setShowLabels(show)
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            settingsRepo.setFontScale(scale)
        }
    }
}
