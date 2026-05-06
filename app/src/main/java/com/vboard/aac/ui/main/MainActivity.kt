package com.vboard.aac.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.vboard.aac.R
import com.vboard.aac.databinding.ActivityMainBinding
import com.vboard.aac.databinding.ItemCategoryChipBinding
import com.vboard.aac.databinding.ItemWordChipBinding
import com.vboard.aac.domain.model.Category
import com.vboard.aac.domain.model.SentenceItem
import com.vboard.aac.platform.feedback.HapticFeedbackManager
import com.vboard.aac.ui.common.CategoryTinter
import com.vboard.aac.ui.pin.PinActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: BoardViewModel by viewModels()

    @Inject
    lateinit var hapticManager: HapticFeedbackManager

    private lateinit var vocabAdapter: VocabGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupListeners()
        observeState()
    }

    private fun setupAdapters() {
        vocabAdapter = VocabGridAdapter { item ->
            hapticManager.tap()
            viewModel.addWordToSentence(item)
        }

        val initialColumns = resources.getInteger(R.integer.vb_board_columns)
        binding.vocabGrid.layoutManager = GridLayoutManager(this, initialColumns)
        binding.vocabGrid.adapter = vocabAdapter
    }

    private fun setupListeners() {
        binding.btnSpeakBottom.setOnClickListener { viewModel.speakSentence() }
        binding.btnClear.setOnClickListener { viewModel.clearSentence() }
        binding.btnBackspace.setOnClickListener { viewModel.removeLastWord() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, PinActivity::class.java))
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    (binding.vocabGrid.layoutManager as? GridLayoutManager)?.spanCount =
                        state.gridColumns
                    vocabAdapter.submitList(state.cardUiItems)
                    vocabAdapter.setShowLabels(state.showLabels)

                    setupCategoryChips(state.categories, state.activeCategoryId)
                    updateSentenceStrip(state.sentenceItems)

                    binding.placeholder.visibility =
                        if (state.placeholderVisible) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupCategoryChips(categories: List<Category>, activeId: String?) {
        binding.categoryContainer.removeAllViews()

        categories.forEach { category ->
            val chipBinding = ItemCategoryChipBinding.inflate(
                LayoutInflater.from(this), binding.categoryContainer, false,
            )
            chipBinding.chipIcon.text = category.icon
            chipBinding.chipText.text = category.name

            val isActive = category.id == activeId
            chipBinding.chipContainer.isSelected = isActive
            val textColor = if (isActive) {
                ContextCompat.getColor(this, android.R.color.white)
            } else {
                val (_, labelRes) = CategoryTinter.colorsFor(
                    BoardViewModel.CATEGORY_ID_TO_CODE[category.id],
                )
                ContextCompat.getColor(this, labelRes)
            }
            chipBinding.chipText.setTextColor(textColor)
            chipBinding.chipIcon.setTextColor(textColor)

            chipBinding.chipContainer.setOnClickListener {
                viewModel.selectCategory(if (category.id == activeId) null else category.id)
            }

            binding.categoryContainer.addView(chipBinding.root)
        }
    }

    private fun updateSentenceStrip(items: List<SentenceItem>) {
        binding.wordsContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val chipBinding = ItemWordChipBinding.inflate(
                LayoutInflater.from(this), binding.wordsContainer, false,
            )
            chipBinding.wordText.text = item.word
            chipBinding.wordChip.setOnClickListener { viewModel.removeWordAt(index) }
            binding.wordsContainer.addView(chipBinding.root)
        }
    }
}
