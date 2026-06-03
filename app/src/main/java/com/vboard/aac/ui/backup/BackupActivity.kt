package com.vboard.aac.ui.backup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vboard.aac.R
import com.vboard.aac.data.backup.BackupManager
import com.vboard.aac.databinding.ActivityBackupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BackupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupBinding

    @Inject
    lateinit var backupManager: BackupManager

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importFromUri(it) }
    }

    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportToUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnExport.setOnClickListener {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "vboard_backup_${dateFormat.format(Date())}.json"
            createFileLauncher.launch(filename)
        }

        binding.btnImport.setOnClickListener {
            importFileLauncher.launch("application/json")
        }
    }

    private fun exportToUri(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val json = backupManager.exportToJson()
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(json)
                }
                showSuccess(getString(R.string.backup_success))
            } catch (e: Exception) {
                showError("Lỗi xuất file: ${e.message}")
            }
        }
    }

    private fun importFromUri(uri: android.net.Uri) {
        lifecycleScope.launch {
            val json = backupManager.readFromUri(uri)
            if (json == null) {
                showError("Không thể đọc file")
                return@launch
            }

            when (val result = backupManager.importFromJson(json)) {
                is BackupManager.ImportResult.Success -> {
                    showSuccess(
                        getString(
                            R.string.backup_import_success,
                            result.categoriesImported,
                            result.cardsImported
                        )
                    )
                }
                is BackupManager.ImportResult.Error -> {
                    showError(result.message)
                }
            }
        }
    }

    private fun showSuccess(message: String) {
        binding.statusMessage.text = message
        binding.statusMessage.setTextColor(getColor(R.color.secondary))
        binding.statusMessage.visibility = View.VISIBLE
        com.vboard.aac.ui.utils.CustomToast.show(this, message)
    }

    private fun showError(message: String) {
        binding.statusMessage.text = message
        binding.statusMessage.setTextColor(getColor(R.color.error))
        binding.statusMessage.visibility = View.VISIBLE
        com.vboard.aac.ui.utils.CustomToast.show(this, message)
    }
}
