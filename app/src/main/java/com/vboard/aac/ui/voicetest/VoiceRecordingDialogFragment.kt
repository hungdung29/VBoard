package com.vboard.aac.ui.voicetest

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vboard.aac.R
import com.vboard.aac.databinding.DialogVoiceRecordingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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
        } else {
            viewModel.onPermissionDenied()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogVoiceRecordingBinding.inflate(layoutInflater)
        setupListeners()
        observeState()
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupListeners() {
        binding.btnRecord.setOnClickListener {
            when (viewModel.uiState.value.recordingState) {
                State.IDLE -> requestRecordPermission()
                State.RECORDING -> viewModel.stopRecording()
                State.STOPPED -> requestRecordPermission()
                else -> {}
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.saveProfile()
        }

        binding.btnReplaySample.setOnClickListener {
            viewModel.previewSample()
        }

        binding.btnDone.setOnClickListener {
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            viewModel.cancel()
            dismiss()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is VoiceRecordingEvent.Error -> showError(event.message)
                        VoiceRecordingEvent.PreviewError -> {
                            showError(getString(R.string.error_vieneu_preview))
                        }
                        VoiceRecordingEvent.PermissionDenied -> {
                            binding.tvHint.text = getString(R.string.error_permission_denied)
                            if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                                showPermissionSettingsDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(state: VoiceRecordingUiState) {
        binding.progressRecording.progress = state.progress
        binding.tvTimer.text = "${state.elapsedSeconds}s / ${state.targetSeconds}s"
        binding.tvHint.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
        )
        val recordColor = ContextCompat.getColor(
            requireContext(),
            if (state.recordingState == State.RECORDING) R.color.error else R.color.primary
        )
        binding.btnRecord.backgroundTintList = ColorStateList.valueOf(recordColor)
        binding.tvRecordAction.setTextColor(recordColor)

        binding.tvHint.text = when (state.recordingState) {
            State.IDLE -> getString(R.string.hint_record_start)
            State.RECORDING -> getString(R.string.hint_recording)
            State.STOPPED -> getString(R.string.hint_review)
            State.SAVING -> getString(R.string.hint_saving)
            State.GENERATING_SAMPLE -> getString(R.string.hint_generating_sample)
            State.PREVIEW_READY -> getString(R.string.hint_preview_ready)
            else -> ""
        }

        binding.tvRecordAction.text = when (state.recordingState) {
            State.IDLE -> getString(R.string.btn_record)
            State.RECORDING -> getString(R.string.btn_stop)
            State.STOPPED -> getString(R.string.btn_rerecord)
            else -> ""
        }

        val recordingControlsVisible = state.recordingState in setOf(
            State.IDLE,
            State.RECORDING,
            State.STOPPED
        )
        binding.btnRecord.visibility = if (recordingControlsVisible) View.VISIBLE else View.GONE
        binding.tvRecordAction.visibility = if (recordingControlsVisible) View.VISIBLE else View.GONE
        binding.btnSave.visibility = if (state.recordingState == State.STOPPED) View.VISIBLE else View.GONE
        binding.layoutActions.visibility =
            if (state.recordingState == State.PREVIEW_READY) View.GONE else View.VISIBLE
        binding.layoutPreviewActions.visibility =
            if (state.recordingState == State.PREVIEW_READY) View.VISIBLE else View.GONE
        binding.layoutProcessing.visibility =
            if (state.recordingState == State.SAVING || state.recordingState == State.GENERATING_SAMPLE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        binding.tvProcessingStatus.text =
            if (state.recordingState == State.GENERATING_SAMPLE) {
                getString(R.string.voice_recording_generating_sample)
            } else {
                getString(R.string.voice_recording_processing)
            }
    }

    private fun showError(message: String) {
        binding.tvHint.text = message
        binding.tvHint.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.error)
        )
    }

    private fun requestRecordPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> viewModel.startRecording()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun showPermissionSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_required)
            .setMessage(R.string.microphone_permission_rationale)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.btn_open_settings) { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                )
            }
            .show()
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.cancel()
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        const val TAG = "VoiceRecordingDialog"
        fun newInstance() = VoiceRecordingDialogFragment()
    }
}
