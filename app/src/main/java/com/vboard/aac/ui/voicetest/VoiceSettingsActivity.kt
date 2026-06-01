package com.vboard.aac.ui.voicetest

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vboard.aac.databinding.ActivityVoiceSettingsBinding
import com.vboard.aac.platform.tts.ValtecTtsEngine
import com.vboard.aac.ui.settings.VoiceSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VoiceSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceSettingsBinding
    private val viewModel: VoiceSettingsViewModel by viewModels()
    private val voicePresets = listOf(
        VoicePreset("Nu mien Bac (NF)", ValtecTtsEngine.VOICE_NF),
        VoicePreset("Nu mien Nam (SF)", ValtecTtsEngine.VOICE_SF),
        VoicePreset("Nam mien Bac 1 (NM1)", ValtecTtsEngine.VOICE_NM1),
        VoicePreset("Nam mien Nam (SM)", ValtecTtsEngine.VOICE_SM),
        VoicePreset("Nam mien Bac 2 (NM2)", ValtecTtsEngine.VOICE_NM2)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupVoicePresetDropdown()
        setupListeners()
        observeState()
    }

    private fun setupVoicePresetDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            voicePresets
        )
        binding.voiceTypeDropdown.setAdapter(adapter)
        binding.voiceTypeDropdown.threshold = 0
        binding.voiceTypeDropdown.setOnClickListener {
            binding.voiceTypeDropdown.showDropDown()
        }
        binding.voiceTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.setVoiceType(voicePresets[position].value)
        }
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

        binding.btnVoiceClone.alpha = 1f
        binding.btnVoiceClone.isEnabled = true
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
                        val preset = voicePresets.firstOrNull { it.value == normalizeVoiceType(type) }
                            ?: voicePresets[1]
                        if (binding.voiceTypeDropdown.text.toString() != preset.label) {
                            binding.voiceTypeDropdown.setText(preset.label, false)
                        }
                    }
                }
            }
        }
    }

    private fun normalizeVoiceType(type: String): String = when (type) {
        "nu-bac" -> ValtecTtsEngine.VOICE_NF
        "nu-nam" -> ValtecTtsEngine.VOICE_SF
        "nam-bac" -> ValtecTtsEngine.VOICE_NM1
        "nam-nam" -> ValtecTtsEngine.VOICE_SM
        else -> type
    }

    private data class VoicePreset(
        val label: String,
        val value: String
    ) {
        override fun toString(): String = label
    }
}
