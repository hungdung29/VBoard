package com.vboard.aac.ui.voicetest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.model.VoiceProfile
import com.vboard.aac.domain.repository.IVoiceProfileRepository
import com.vboard.aac.platform.audio.AudioQualityAnalyzer
import com.vboard.aac.platform.audio.AudioRecording
import com.vboard.aac.platform.audio.AudioRecorderManager
import com.vboard.aac.platform.voice.DeviceCapabilityDetector
import com.vboard.aac.platform.voice.InferenceMode
import com.vboard.aac.platform.voice.VoiceCloningManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class VoiceRecordingUiState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val recordingOption: RecordingOption = RecordingOption.ONE_TIME,
    val elapsedTimeMs: Long = 0,
    val currentAmplitude: Float = 0f,
    val qualityResult: AudioQualityAnalyzer.QualityResult? = null,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val isSaved: Boolean = false,
    val inferenceMode: InferenceMode = InferenceMode.LITE_PIPER
)

enum class RecordingState {
    IDLE, RECORDING, STOPPED, PROCESSING, SUCCESS, ERROR
}

enum class RecordingOption {
    ONE_TIME,      // 30 seconds once
    THREE_TIMES,   // 10 seconds x 3
    WIZARD         // Guided 5-step
}

@HiltViewModel
class VoiceRecordingViewModel @Inject constructor(
    private val audioRecorderManager: AudioRecorderManager,
    private val audioQualityAnalyzer: AudioQualityAnalyzer,
    private val voiceProfileRepository: IVoiceProfileRepository,
    private val voiceCloningManager: VoiceCloningManager,
    private val deviceCapabilityDetector: DeviceCapabilityDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceRecordingUiState())
    val uiState: StateFlow<VoiceRecordingUiState> = _uiState.asStateFlow()

    private var amplitudeJob: Job? = null
    private var currentRecording: AudioRecording? = null
    private var recordingStartTime: Long = 0

    init {
        _uiState.value = _uiState.value.copy(
            inferenceMode = deviceCapabilityDetector.detectInferenceMode()
        )
    }

    fun setRecordingOption(option: RecordingOption) {
        _uiState.value = _uiState.value.copy(recordingOption = option)
    }

    fun startRecording() {
        if (!audioRecorderManager.hasRecordPermission()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Cần quyền ghi âm"
            )
            return
        }

        val result = audioRecorderManager.startRecording("voice_${System.currentTimeMillis()}")
        result.onSuccess {
            recordingStartTime = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                recordingState = RecordingState.RECORDING,
                elapsedTimeMs = 0,
                errorMessage = null
            )
            startAmplitudeMonitoring()
        }.onFailure { e ->
            _uiState.value = _uiState.value.copy(
                recordingState = RecordingState.ERROR,
                errorMessage = "Không thể bắt đầu ghi âm: ${e.message}"
            )
        }
    }

    fun stopRecording() {
        amplitudeJob?.cancel()
        val result = audioRecorderManager.stopRecording()

        result.onSuccess { recording ->
            currentRecording = recording
            val quality = audioQualityAnalyzer.analyze(recording)

            _uiState.value = _uiState.value.copy(
                recordingState = RecordingState.STOPPED,
                elapsedTimeMs = recording.durationMs,
                qualityResult = quality
            )
        }.onFailure { e ->
            _uiState.value = _uiState.value.copy(
                recordingState = RecordingState.ERROR,
                errorMessage = "Lỗi ghi âm: ${e.message}"
            )
        }
    }

    fun saveProfile() {
        val recording = currentRecording ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingProgress = 0f
            )

            try {
                _uiState.value = _uiState.value.copy(processingProgress = 0.2f)
                val quality = audioQualityAnalyzer.analyze(recording)

                if (!quality.isPass) {
                    val issues = quality.issues.joinToString("\n") { it.message }
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Chất lượng không đạt:\n$issues"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(processingProgress = 0.5f)

                val profile = VoiceProfile(
                    id = UUID.randomUUID().toString(),
                    name = VoiceProfile.DEFAULT_NAME,
                    createdAt = System.currentTimeMillis(),
                    embeddingPath = recording.file.absolutePath,
                    isActive = true,
                    inferenceMode = _uiState.value.inferenceMode.name,
                    sampleRate = recording.sampleRate,
                    durationMs = recording.durationMs.toInt(),
                    qualityScore = quality.score
                )

                _uiState.value = _uiState.value.copy(processingProgress = 0.8f)
                voiceProfileRepository.saveProfile(profile)

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingProgress = 1f,
                    recordingState = RecordingState.SUCCESS,
                    isSaved = true
                )

                recording.file.delete()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    recordingState = RecordingState.ERROR,
                    errorMessage = "Lỗi lưu: ${e.message}"
                )
            }
        }
    }

    fun deleteRecording() {
        amplitudeJob?.cancel()
        audioRecorderManager.cancelRecording()
        currentRecording?.file?.delete()
        currentRecording = null
        _uiState.value = _uiState.value.copy(
            recordingState = RecordingState.IDLE,
            qualityResult = null,
            elapsedTimeMs = 0
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun startAmplitudeMonitoring() {
        amplitudeJob = viewModelScope.launch {
            while (_uiState.value.recordingState == RecordingState.RECORDING) {
                val amplitude = audioRecorderManager.getCurrentAmplitude()
                val elapsed = System.currentTimeMillis() - recordingStartTime
                _uiState.value = _uiState.value.copy(
                    currentAmplitude = amplitude,
                    elapsedTimeMs = elapsed
                )
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        amplitudeJob?.cancel()
        audioRecorderManager.cancelRecording()
    }
}
