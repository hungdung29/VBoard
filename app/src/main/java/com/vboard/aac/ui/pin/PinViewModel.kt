package com.vboard.aac.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    val mathAnswer: Int = 0,
    val isLocked: Boolean = false,
    val lockoutSeconds: Int = 0
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val settingsRepo: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    private var failedAttempts = 0
    private val maxAttempts = 5

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
        if (_uiState.value.isLocked) return
        val current = _uiState.value.enteredDigits
        if (current.length >= 4) return

        val newDigits = current + digit
        _uiState.value = _uiState.value.copy(enteredDigits = newDigits, isError = false)

        if (newDigits.length == 4) {
            verifyPin(newDigits)
        }
    }

    fun onBackspace() {
        if (_uiState.value.isLocked) return
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
                failedAttempts = 0
                _uiState.value = _uiState.value.copy(isUnlocked = true)
            } else {
                failedAttempts++
                _uiState.value = _uiState.value.copy(enteredDigits = "", isError = true)
                generateMathChallenge()

                if (failedAttempts >= maxAttempts) {
                    lockout()
                }
            }
        }
    }

    private fun lockout() {
        _uiState.value = _uiState.value.copy(isLocked = true, lockoutSeconds = 30)
        viewModelScope.launch {
            for (seconds in 30 downTo 1) {
                _uiState.value = _uiState.value.copy(lockoutSeconds = seconds)
                delay(1000)
            }
            failedAttempts = 0
            _uiState.value = _uiState.value.copy(isLocked = false, lockoutSeconds = 0)
        }
    }
}
