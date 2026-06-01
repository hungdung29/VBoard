package com.vboard.aac.ui.voicetest

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        if (granted) viewModel.startRecording() else viewModel.onPermissionDenied()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext()).create()
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

        binding.btnCancel.setOnClickListener {
            viewModel.cancel()
            dismiss()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is VoiceRecordingEvent.Success -> dismiss()
                        is VoiceRecordingEvent.Error -> showError(event.message)
                        VoiceRecordingEvent.PermissionDenied -> {
                            binding.tvHint.text = getString(R.string.error_permission_denied)
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(state: VoiceRecordingUiState) {
        binding.progressRecording.progress = state.progress
        binding.tvTimer.text = "${state.elapsedSeconds}s / ${state.targetSeconds}s"

        binding.tvHint.text = when (state.recordingState) {
            State.IDLE -> getString(R.string.hint_record_start)
            State.RECORDING -> getString(R.string.hint_recording)
            State.STOPPED -> getString(R.string.hint_review)
            State.SAVING -> getString(R.string.hint_saving)
            else -> ""
        }

        binding.btnRecord.text = when (state.recordingState) {
            State.IDLE -> getString(R.string.btn_record)
            State.RECORDING -> getString(R.string.btn_stop)
            State.STOPPED -> getString(R.string.btn_rerecord)
            else -> ""
        }

        binding.btnSave.visibility = if (state.recordingState == State.STOPPED) View.VISIBLE else View.GONE
        binding.layoutProcessing.visibility = if (state.recordingState == State.SAVING) View.VISIBLE else View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "VoiceRecordingDialog"
        fun newInstance() = VoiceRecordingDialogFragment()
    }
}
