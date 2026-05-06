package com.vboard.aac.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class PinUiState(
    val enteredDigits: String = "",
    val isError: Boolean = false,
    val isUnlocked: Boolean = false,
    val mathQuestion: String = "",
    val mathAnswer: Int = 0
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val settingsRepo: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        generateMathChallenge()
    }

    private fun generateMathChallenge() {
        val a = Random.nextInt(1, 9)
        val b = Random.nextInt(1, 9)
        val useAddition = Random.nextBoolean()
        val (question, answer) = if (useAddition || a >= b) {
            "$a + $b = ?" to (a + b)
        } else {
            "$a - $b = ?" to (a - b)
        }
        _uiState.value = _uiState.value.copy(
            mathQuestion = question,
            mathAnswer = answer
        )
    }

    fun onDigitEntered(digit: String) {
        val current = _uiState.value.enteredDigits
        if (current.length >= 4) return

        val newDigits = current + digit
        _uiState.value = _uiState.value.copy(enteredDigits = newDigits, isError = false)

        if (newDigits.length == 4) {
            verifyPin(newDigits)
        }
    }

    fun onBackspace() {
        val current = _uiState.value.enteredDigits
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                enteredDigits = current.dropLast(1),
                isError = false
            )
        }
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(enteredDigits = "", isError = false)
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            val isCorrect = settingsRepo.verifyPin(pin)
            if (isCorrect) {
                _uiState.value = _uiState.value.copy(isUnlocked = true)
            } else {
                _uiState.value = _uiState.value.copy(enteredDigits = "", isError = true)
                generateMathChallenge()
            }
        }
    }
}
