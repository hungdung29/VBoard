package com.vboard.aac.ui.voicetest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.repository.ISettingsRepository
import com.vboard.aac.platform.tts.TextToSpeechManager
import com.vboard.aac.platform.voice.RecordingResult
import com.vboard.aac.platform.voice.VoiceProfileManager
import com.vboard.aac.platform.voice.VoiceRecordingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceRecordingUiState(
    val recordingState: State = State.IDLE,
    val progress: Int = 0,
    val elapsedSeconds: Int = 0,
    val targetSeconds: Int = (VoiceRecordingManager.TARGET_DURATION_MS / 1000).toInt()
)

enum class State { IDLE, RECORDING, STOPPED, SAVING, GENERATING_SAMPLE, PREVIEW_READY, ERROR }

sealed class VoiceRecordingEvent {
    data object PermissionDenied : VoiceRecordingEvent()
    data object PreviewError : VoiceRecordingEvent()
    data class Error(val message: String) : VoiceRecordingEvent()
}

@HiltViewModel
class VoiceRecordingViewModel @Inject constructor(
    private val voiceProfileManager: VoiceProfileManager,
    private val settingsRepository: ISettingsRepository,
    private val ttsManager: TextToSpeechManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceRecordingUiState())
    val uiState: StateFlow<VoiceRecordingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VoiceRecordingEvent>()
    val events: SharedFlow<VoiceRecordingEvent> = _events.asSharedFlow()

    private var profileName: String = "Giong nguoi than"
    private var pendingRecording: RecordingResult? = null
    private var timerJob: Job? = null
    private var recordingStartedAt: Long = 0L

    fun setProfileName(name: String) {
        profileName = name
    }

    fun startRecording() {
        pendingRecording = null
        voiceProfileManager.startRecording(profileName).onSuccess {
            recordingStartedAt = System.currentTimeMillis()
            _uiState.value = VoiceRecordingUiState(recordingState = State.RECORDING)
            startTimer()
        }.onFailure { ex ->
            _uiState.value = VoiceRecordingUiState()
            viewModelScope.launch {
                _events.emit(VoiceRecordingEvent.Error(ex.message ?: "Unable to start recording"))
            }
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        voiceProfileManager.stopRecording().onSuccess { recording ->
            pendingRecording = recording
            _uiState.value = _uiState.value.copy(
                recordingState = State.STOPPED,
                progress = 100,
                elapsedSeconds = (recording.durationMs / 1000).toInt()
            )
        }.onFailure { ex ->
            _uiState.value = VoiceRecordingUiState()
            viewModelScope.launch {
                _events.emit(VoiceRecordingEvent.Error(ex.message ?: "Unable to stop recording"))
            }
        }
    }

    fun saveProfile() {
        val recording = pendingRecording
        if (recording == null) {
            viewModelScope.launch {
                _events.emit(VoiceRecordingEvent.Error("No recording to save"))
            }
            return
        }

        _uiState.value = _uiState.value.copy(recordingState = State.SAVING, progress = 0)
        viewModelScope.launch {
            voiceProfileManager.saveRecordingAsProfile(profileName, recording) { progress ->
                _uiState.value = _uiState.value.copy(progress = progress)
            }.onSuccess {
                settingsRepository.setVoiceCloningEnabled(true)
                pendingRecording = null
                generateSample()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(recordingState = State.STOPPED)
                _events.emit(VoiceRecordingEvent.Error(ex.message ?: "Unable to save voice profile"))
            }
        }
    }

    fun previewSample() {
        viewModelScope.launch {
            ttsManager.playVieneuClonePreview(PREVIEW_TEXT)
                .onFailure {
                    _events.emit(VoiceRecordingEvent.PreviewError)
                }
        }
    }

    fun onPermissionDenied() {
        viewModelScope.launch {
            _events.emit(VoiceRecordingEvent.PermissionDenied)
        }
    }

    fun cancel() {
        timerJob?.cancel()
        voiceProfileManager.cancelRecording()
        ttsManager.stop()
    }

    private suspend fun generateSample() {
        _uiState.value = _uiState.value.copy(recordingState = State.GENERATING_SAMPLE, progress = 100)
        ttsManager.generateVieneuClonePreview(PREVIEW_TEXT)
            .onSuccess {
                _uiState.value = _uiState.value.copy(recordingState = State.PREVIEW_READY)
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(recordingState = State.PREVIEW_READY)
                _events.emit(VoiceRecordingEvent.PreviewError)
            }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.recordingState == State.RECORDING) {
                val elapsedSeconds = ((System.currentTimeMillis() - recordingStartedAt) / 1000).toInt()
                val progress = (elapsedSeconds * 100 / _uiState.value.targetSeconds).coerceIn(0, 100)
                voiceProfileManager.getCurrentAmplitude()
                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = elapsedSeconds,
                    progress = progress
                )

                if (elapsedSeconds >= _uiState.value.targetSeconds) {
                    stopRecording()
                    break
                }
                delay(250)
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        if (_uiState.value.recordingState == State.RECORDING) {
            voiceProfileManager.cancelRecording()
        }
        super.onCleared()
    }

    private companion object {
        private const val PREVIEW_TEXT = "Con mu\u1ed1n u\u1ed1ng n\u01b0\u1edbc"
    }
}
