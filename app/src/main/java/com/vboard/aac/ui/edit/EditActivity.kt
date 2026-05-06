package com.vboard.aac.ui.edit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.vboard.aac.R
import com.vboard.aac.databinding.ActivityEditBinding
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.ui.main.VocabGridAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding
    private val viewModel: EditViewModel by viewModels()
    private lateinit var adapter: EditCardAdapter

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pendingImageUri = it }
    }

    private var pendingImageUri: Uri? = null
    private var editingCard: VocabCard? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGrid()
        setupListeners()
        observeState()
    }

    private fun setupGrid() {
        adapter = EditCardAdapter(
            onEditClick = { card -> showEditDialog(card) },
            onDeleteClick = { card -> showDeleteDialog(card) }
        )
        binding.vocabGrid.layoutManager = GridLayoutManager(this, 3)
        binding.vocabGrid.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnDone.setOnClickListener { finish() }
        binding.fabAddCard.setOnClickListener { showAddDialog() }
        binding.fabAddFolder.setOnClickListener { showAddFolderDialog() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.cards)

                    // Update folder tabs
                    updateFolderTabs(state.categories)

                    state.message?.let { msg ->
                        Toast.makeText(this@EditActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }

    private fun updateFolderTabs(categories: List<com.vboard.aac.domain.model.Category>) {
        binding.folderContainer.removeAllViews()
        categories.forEach { category ->
            val tab = layoutInflater.inflate(
                R.layout.item_folder_tab, binding.folderContainer, false
            ) as android.widget.LinearLayout
            tab.findViewById<android.widget.TextView>(R.id.folderIcon).text = category.icon
            tab.findViewById<android.widget.TextView>(R.id.folderText).text = category.name
            tab.setOnClickListener {
                val currentId = viewModel.uiState.value.activeCategoryId
                val newId = if (currentId == category.id) null else category.id
                viewModel.selectCategory(newId)
            }
            binding.folderContainer.addView(tab)
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)
        AlertDialog.Builder(this)
            .setTitle("Thêm thẻ mới")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val word = dialogView.findViewById<android.widget.EditText>(R.id.editWord).text.toString()
                if (word.isNotBlank()) {
                    viewModel.addCard(word, "cat-1", null)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditDialog(card: VocabCard) {
        editingCard = card
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)
        val editText = dialogView.findViewById<android.widget.EditText>(R.id.editWord)
        editText.setText(card.word)
        AlertDialog.Builder(this)
            .setTitle("Sửa thẻ")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val word = editText.text.toString()
                if (word.isNotBlank()) {
                    viewModel.updateCard(card.id, word, card.categoryId, null)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDeleteDialog(card: VocabCard) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage("Bạn có chắc muốn xóa thẻ \"${card.word}\"?")
            .setPositiveButton(R.string.confirm_delete_yes) { _, _ ->
                viewModel.deleteCard(card.id)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showAddFolderDialog() {
        val categories = viewModel.uiState.value.categories
        val colors = listOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#F7DC6F", "#98D8C8")
        val icons = listOf("📁", "🎁", "🎀", "📚", "🎨", "🎵", "⚽", "🍎")
        var selectedColor = colors.first()
        var selectedIcon = icons.first()

        val editText = android.widget.EditText(this).apply {
            hint = "Tên thư mục"
        }

        AlertDialog.Builder(this)
            .setTitle("Tạo thư mục mới")
            .setView(editText)
            .setPositiveButton("Tạo") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    viewModel.addCategory(name, selectedIcon, selectedColor)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun openCamera() {
        // CameraX integration would go here
        pickImageLauncher.launch("image/*")
    }
}
