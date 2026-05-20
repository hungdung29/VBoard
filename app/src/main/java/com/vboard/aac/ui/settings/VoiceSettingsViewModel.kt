package com.vboard.aac.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.repository.ISettingsRepository
import com.vboard.aac.platform.tts.TextToSpeechManager
import com.vboard.aac.platform.voice.InferenceMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceSettingsViewModel @Inject constructor(
    private val settingsRepo: ISettingsRepository,
    private val ttsManager: TextToSpeechManager
) : ViewModel() {

    data class VoiceSettingsUiState(
        val voiceCloningEnabled: Boolean = false,
        val hasVoiceProfile: Boolean = false,
        val inferenceMode: InferenceMode = InferenceMode.LITE_PIPER,
        val isVoiceCloningSupported: Boolean = true,
        val voiceVolume: Float = 1.0f,
        val voiceType: String = "nam-bac"
    )

    val uiState: StateFlow<VoiceSettingsUiState> = combine(
        settingsRepo.voiceCloningEnabled,
        settingsRepo.voiceVolume,
        settingsRepo.voiceType
    ) { voiceCloningEnabled, voiceVolume, voiceType ->
        VoiceSettingsUiState(
            voiceCloningEnabled = voiceCloningEnabled,
            voiceVolume = voiceVolume,
            voiceType = voiceType
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VoiceSettingsUiState())

    val voiceVolume = settingsRepo.voiceVolume
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val voiceType = settingsRepo.voiceType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "nam-bac")

    fun setVoiceVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepo.setVoiceVolume(volume)
        }
    }

    fun setVoiceType(type: String) {
        viewModelScope.launch {
            settingsRepo.setVoiceType(type)
        }
    }

    fun preview() {
        ttsManager.preview("Con muốn uống nước")
    }

    fun setVoiceCloningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setVoiceCloningEnabled(enabled)
        }
    }
}
