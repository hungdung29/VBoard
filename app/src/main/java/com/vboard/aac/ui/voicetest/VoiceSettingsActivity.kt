package com.vboard.aac.ui.voicetest

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vboard.aac.databinding.ActivityVoiceSettingsBinding
import com.vboard.aac.ui.settings.VoiceSettingsViewModel
import com.vboard.aac.ui.voicetest.VoiceRecordingDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VoiceSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceSettingsBinding
    private val viewModel: VoiceSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.setVoiceVolume(progress / 100f)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnPreview.setOnClickListener {
            viewModel.preview()
        }

        // Voice type selection
        binding.voiceTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                binding.radioMaleNorth.id -> "nam-bac"
                binding.radioMaleSouth.id -> "nam-nam"
                binding.radioFemaleNorth.id -> "nu-bac"
                binding.radioFemaleSouth.id -> "nu-nam"
                else -> "nam-bac"
            }
            viewModel.setVoiceType(type)
        }

        binding.btnVoiceClone.setOnClickListener {
            VoiceRecordingDialogFragment.newInstance()
                .show(supportFragmentManager, VoiceRecordingDialogFragment.TAG)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.voiceVolume.collect { volume ->
                        binding.volumeSlider.progress = (volume * 100).toInt()
                    }
                }
                launch {
                    viewModel.voiceType.collect { type ->
                        when (type) {
                            "nam-bac" -> binding.radioMaleNorth.isChecked = true
                            "nam-nam" -> binding.radioMaleSouth.isChecked = true
                            "nu-bac" -> binding.radioFemaleNorth.isChecked = true
                            "nu-nam" -> binding.radioFemaleSouth.isChecked = true
                        }
                    }
                }
            }
        }
    }
}
