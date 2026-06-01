package com.vboard.aac.ui.pin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vboard.aac.R
import com.vboard.aac.databinding.ActivityPinBinding
import com.vboard.aac.platform.feedback.HapticFeedbackManager
import com.vboard.aac.ui.admin.AdminActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinBinding
    private val viewModel: PinViewModel by viewModels()

    @Inject
    lateinit var hapticManager: HapticFeedbackManager

    private val dots by lazy {
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNumpad()
        setupListeners()
        observeState()
    }

    private fun setupNumpad() {
        val numberButtons = mapOf(
            binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3",
            binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6",
            binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9",
            binding.btn0 to "0"
        )
        numberButtons.forEach { (btn, digit) ->
            btn.setOnClickListener {
                hapticManager.tap()
                viewModel.onDigitEntered(digit)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBackspace.setOnClickListener {
            hapticManager.tap()
            viewModel.onBackspace()
        }
        binding.btnCancel.setOnClickListener {
            finish()
        }
        binding.btnHeaderBack.setOnClickListener {
            finish()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update dots
                    dots.forEachIndexed { index, dot ->
                        val filled = index < state.enteredDigits.length
                        dot.setBackgroundResource(
                            if (filled) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot_empty
                        )
                    }

                    // Error state
                    binding.pinErrorText.visibility = if (state.isError) View.VISIBLE else View.GONE
                    if (state.isError) {
                        hapticManager.error()
                        dots.forEach { dot ->
                            dot.setBackgroundResource(R.drawable.bg_pin_dot_empty)
                        }
                    }

                    // Lockout state
                    binding.btn1.isEnabled = !state.isLocked
                    binding.btn2.isEnabled = !state.isLocked
                    binding.btn3.isEnabled = !state.isLocked
                    binding.btn4.isEnabled = !state.isLocked
                    binding.btn5.isEnabled = !state.isLocked
                    binding.btn6.isEnabled = !state.isLocked
                    binding.btn7.isEnabled = !state.isLocked
                    binding.btn8.isEnabled = !state.isLocked
                    binding.btn9.isEnabled = !state.isLocked
                    binding.btn0.isEnabled = !state.isLocked
                    binding.btnBackspace.isEnabled = !state.isLocked

                    // Unlocked
                    if (state.isUnlocked) {
                        hapticManager.success()
                        startActivity(Intent(this@PinActivity, AdminActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}