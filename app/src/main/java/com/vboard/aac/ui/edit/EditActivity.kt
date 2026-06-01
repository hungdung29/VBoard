package com.vboard.aac.ui.edit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        uri?.let {
            pendingImageUri = it
            pendingImagePreview?.setImageURI(it)
            pendingImagePreview?.visibility = android.view.View.VISIBLE
            pendingImagePlaceholder?.visibility = android.view.View.GONE
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingImageUri != null) {
            pendingImagePreview?.setImageURI(pendingImageUri)
            pendingImagePreview?.visibility = android.view.View.VISIBLE
            pendingImagePlaceholder?.visibility = android.view.View.GONE
        } else {
            pendingImageUri = null
        }
    }

    private var pendingImageUri: Uri? = null
    private var pendingImagePreview: ImageView? = null
    private var pendingImagePlaceholder: android.view.View? = null
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
        pendingImageUri = null
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)
        val takePhoto = dialogView.findViewById<android.view.View>(R.id.btnTakePhoto)
        val pickGallery = dialogView.findViewById<android.view.View>(R.id.btnPickGallery)
        val imageContainer = dialogView.findViewById<android.view.View>(R.id.imageContainer)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.selectedImage)
        val imagePlaceholder = dialogView.findViewById<android.view.View>(R.id.imagePlaceholder)
        pendingImagePreview = imagePreview
        pendingImagePlaceholder = imagePlaceholder
        takePhoto.setOnClickListener { openCamera() }
        pickGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        imageContainer.setOnClickListener { pickImageLauncher.launch("image/*") }

        AlertDialog.Builder(this)
            .setTitle("Thêm thẻ mới")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val word = dialogView.findViewById<android.widget.EditText>(R.id.editWord).text.toString()
                if (word.isNotBlank()) {
                    viewModel.addCard(word, "cat-1", pendingImageUri?.toString())
                }
            }
            .setNegativeButton("Hủy", null)
            .setOnDismissListener {
                pendingImageUri = null
                pendingImagePreview = null
                pendingImagePlaceholder = null
            }
            .show()
    }

    private fun showEditDialog(card: VocabCard) {
        editingCard = card
        pendingImageUri = null
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)
        val editText = dialogView.findViewById<android.widget.EditText>(R.id.editWord)
        val takePhoto = dialogView.findViewById<android.view.View>(R.id.btnTakePhoto)
        val pickGallery = dialogView.findViewById<android.view.View>(R.id.btnPickGallery)
        val imageContainer = dialogView.findViewById<android.view.View>(R.id.imageContainer)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.selectedImage)
        val imagePlaceholder = dialogView.findViewById<android.view.View>(R.id.imagePlaceholder)
        pendingImagePreview = imagePreview
        pendingImagePlaceholder = imagePlaceholder
        takePhoto.setOnClickListener { openCamera() }
        pickGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        imageContainer.setOnClickListener { pickImageLauncher.launch("image/*") }
        editText.setText(card.word)

        card.localImagePath?.let { path ->
            val uri = Uri.parse(path)
            imagePreview.setImageURI(uri)
            imagePreview.visibility = android.view.View.VISIBLE
            imagePlaceholder.visibility = android.view.View.GONE
        }

        AlertDialog.Builder(this)
            .setTitle("Sửa thẻ")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val word = editText.text.toString()
                if (word.isNotBlank()) {
                    viewModel.updateCard(card.id, word, card.categoryId, pendingImageUri?.toString())
                }
            }
            .setNegativeButton("Hủy", null)
            .setOnDismissListener {
                pendingImageUri = null
                pendingImagePreview = null
                pendingImagePlaceholder = null
            }
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
        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        val photoFile = createTempImageFile()
        val photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        pendingImageUri = photoUri
        takePictureLauncher.launch(photoUri)
    }

    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(cacheDir, "images").apply { mkdirs() }
        return File.createTempFile("VBOARD_${timeStamp}_", ".jpg", storageDir)
    }
}
