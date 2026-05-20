package com.vboard.aac.ui.voicetest

import android.Manifest
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vboard.aac.R
import com.vboard.aac.databinding.DialogVoiceRecordingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VoiceRecordingDialogFragment : DialogFragment() {

    private var _binding: DialogVoiceRecordingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VoiceRecordingViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVoiceRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnRecord.setOnClickListener {
            val state = viewModel.uiState.value.recordingState
            when (state) {
                RecordingState.IDLE, RecordingState.STOPPED -> {
                    if (hasAudioPermission()) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                RecordingState.RECORDING -> viewModel.stopRecording()
                else -> {}
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.saveProfile()
        }

        binding.btnCancel.setOnClickListener {
            viewModel.deleteRecording()
            dismiss()
        }

        binding.rgRecordingOption.setOnCheckedChangeListener { _, checkedId ->
            val option = when (checkedId) {
                R.id.rbOneTime -> RecordingOption.ONE_TIME
                R.id.rbThreeTimes -> RecordingOption.THREE_TIMES
                R.id.rbWizard -> RecordingOption.WIZARD
                else -> RecordingOption.ONE_TIME
            }
            viewModel.setRecordingOption(option)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: VoiceRecordingUiState) {
        when (state.recordingState) {
            RecordingState.IDLE -> {
                binding.btnRecord.text = "GHI ÂM"
                binding.btnRecord.isEnabled = true
            }
            RecordingState.RECORDING -> {
                binding.btnRecord.text = "DỪNG"
                binding.btnRecord.isEnabled = true
            }
            RecordingState.STOPPED -> {
                binding.btnRecord.text = "GHI LẠI"
                binding.btnRecord.isEnabled = true
                binding.btnSave.visibility = View.VISIBLE
            }
            RecordingState.PROCESSING -> {
                binding.btnRecord.isEnabled = false
            }
            else -> {
                binding.btnRecord.isEnabled = false
            }
        }

        val targetDuration = when (state.recordingOption) {
            RecordingOption.ONE_TIME -> 30
            RecordingOption.THREE_TIMES -> 10
            RecordingOption.WIZARD -> 10
        }
        binding.tvTimer.text = "${state.elapsedTimeMs / 1000}s / ${targetDuration}s"

        val progress = ((state.elapsedTimeMs.toFloat() / (targetDuration * 1000)) * 100).toInt()
        binding.progressRecording.progress = progress

        state.qualityResult?.let { quality ->
            binding.tvQuality.visibility = View.VISIBLE
            if (quality.isPass) {
                binding.tvQuality.text = "Chất lượng: Tốt (${(quality.score * 100).toInt()}%)"
                binding.tvQuality.setTextColor(resources.getColor(R.color.green_500, null))
            } else {
                val issues = quality.issues.joinToString("\n") { it.message }
                binding.tvQuality.text = "Chất lượng chưa đạt:\n$issues"
                binding.tvQuality.setTextColor(resources.getColor(R.color.error, null))
            }
        } ?: run {
            binding.tvQuality.visibility = View.GONE
        }

        binding.layoutProcessing.visibility = if (state.isProcessing) View.VISIBLE else View.GONE
        if (state.isProcessing) {
            binding.tvProcessingStatus.text = "Đang xử lý... ${(state.processingProgress * 100).toInt()}%"
        }

        if (state.isSaved) {
            dismiss()
        }

        state.errorMessage?.let { error ->
            binding.tvHint.text = error
            viewModel.clearError()
        }
    }

    private fun hasAudioPermission(): Boolean {
        return requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
               android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "VoiceRecordingDialog"

        fun newInstance(): VoiceRecordingDialogFragment {
            return VoiceRecordingDialogFragment()
        }
    }
}
