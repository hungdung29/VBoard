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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.vboard.aac.R
import com.vboard.aac.databinding.ActivityEditBinding
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.ui.main.VocabGridAdapter
import coil.load
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

    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val croppedUriString = result.data?.getStringExtra("cropped_uri")
            if (croppedUriString != null) {
                val croppedUri = Uri.parse(croppedUriString)
                pendingImageUri = croppedUri
                pendingImagePreview?.load(croppedUri)
                pendingImageContainer?.visibility = android.view.View.VISIBLE
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            startCropActivity(it)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingImageUri != null) {
            startCropActivity(pendingImageUri!!)
        } else {
            pendingImageUri = null
        }
    }

    private fun startCropActivity(uri: Uri) {
        val intent = Intent(this, CropActivity::class.java).apply {
            putExtra("image_uri", uri.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        cropImageLauncher.launch(intent)
    }

    private var pendingImageUri: Uri? = null
    private var pendingImagePreview: ImageView? = null
    private var pendingImageContainer: android.view.View? = null
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

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION) {
                    adapter.moveItem(fromPos, toPos)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No-op
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewModel.updateCardsOrder(adapter.getCards())
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.vocabGrid)
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
                        com.vboard.aac.ui.utils.CustomToast.show(this@EditActivity, msg)
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
        pendingImagePreview = imagePreview
        pendingImageContainer = imageContainer
        takePhoto.setOnClickListener { openCamera() }
        pickGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        imageContainer.setOnClickListener { pickImageLauncher.launch("image/*") }

        // Category selection dropdown setup
        val categories = viewModel.uiState.value.categories
        val categoryNames = categories.map { "${it.icon}  ${it.name}" }
        val categoryAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        val selectCategory = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.selectCategory)
        selectCategory.setAdapter(categoryAdapter)

        val activeCategoryId = viewModel.uiState.value.activeCategoryId
        var selectedCategoryIndex = categories.indexOfFirst { it.id == activeCategoryId }
        if (selectedCategoryIndex == -1 && categories.isNotEmpty()) {
            selectedCategoryIndex = 0
        }
        if (selectedCategoryIndex != -1) {
            selectCategory.setText(categoryNames[selectedCategoryIndex], false)
        }

        var selectedCategoryId = if (selectedCategoryIndex != -1) categories[selectedCategoryIndex].id else "cat-1"
        selectCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Thêm thẻ mới")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val word = dialogView.findViewById<android.widget.EditText>(R.id.editWord).text.toString()
                if (word.isNotBlank()) {
                    viewModel.addCard(word, selectedCategoryId, pendingImageUri?.toString())
                }
            }
            .setNegativeButton("Hủy", null)
            .setOnDismissListener {
                pendingImageUri = null
                pendingImagePreview = null
                pendingImageContainer = null
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
        pendingImagePreview = imagePreview
        pendingImageContainer = imageContainer
        takePhoto.setOnClickListener { openCamera() }
        pickGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        imageContainer.setOnClickListener { pickImageLauncher.launch("image/*") }
        editText.setText(card.word)

        card.localImagePath?.let { path ->
            val uri = Uri.parse(path)
            imagePreview.load(uri)
            imageContainer.visibility = android.view.View.VISIBLE
        }

        // Category selection dropdown setup
        val categories = viewModel.uiState.value.categories
        val categoryNames = categories.map { "${it.icon}  ${it.name}" }
        val categoryAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        val selectCategory = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.selectCategory)
        selectCategory.setAdapter(categoryAdapter)

        var selectedCategoryIndex = categories.indexOfFirst { it.id == card.categoryId }
        if (selectedCategoryIndex == -1 && categories.isNotEmpty()) {
            selectedCategoryIndex = 0
        }
        if (selectedCategoryIndex != -1) {
            selectCategory.setText(categoryNames[selectedCategoryIndex], false)
        }

        var selectedCategoryId = if (selectedCategoryIndex != -1) categories[selectedCategoryIndex].id else card.categoryId
        selectCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Sửa thẻ")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val word = editText.text.toString()
                if (word.isNotBlank()) {
                    viewModel.updateCard(card.id, word, selectedCategoryId, pendingImageUri?.toString())
                }
            }
            .setNegativeButton("Hủy", null)
            .setOnDismissListener {
                pendingImageUri = null
                pendingImagePreview = null
                pendingImageContainer = null
            }
            .show()
    }

    private fun showDeleteDialog(card: VocabCard) {
        MaterialAlertDialogBuilder(this)
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

        MaterialAlertDialogBuilder(this)
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
