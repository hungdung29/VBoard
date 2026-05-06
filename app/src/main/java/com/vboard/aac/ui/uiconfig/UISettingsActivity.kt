package com.vboard.aac.ui.uiconfig

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vboard.aac.R
import com.vboard.aac.databinding.ActivityUiSettingsBinding
import com.vboard.aac.ui.settings.UISettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UISettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUiSettingsBinding
    private val viewModel: UISettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUiSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.grid2.setOnClickListener {
            viewModel.setGridColumns(2)
        }
        binding.grid3.setOnClickListener {
            viewModel.setGridColumns(3)
        }
        binding.grid4.setOnClickListener {
            viewModel.setGridColumns(4)
        }

        binding.switchLabels.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowLabels(isChecked)
        }

        binding.btnDarkMode.setOnClickListener {
            val current = viewModel.settings.value.isDarkMode
            viewModel.setDarkMode(!current)
            AppCompatDelegate.setDefaultNightMode(
                if (!current) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    binding.grid2.alpha = if (settings.gridColumns == 2) 1f else 0.4f
                    binding.grid3.alpha = if (settings.gridColumns == 3) 1f else 0.4f
                    binding.grid4.alpha = if (settings.gridColumns == 4) 1f else 0.4f
                    binding.switchLabels.isChecked = settings.showLabels

                    binding.btnDarkMode.text = if (settings.isDarkMode) "Chế độ Tối" else "Chế độ Sáng"
                }
            }
        }
    }
}
