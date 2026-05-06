package com.vboard.aac.ui.stats

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vboard.aac.R
import com.vboard.aac.databinding.ActivityStatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.sentenceCount.text = state.todayStats.sentencesCount.toString()
                    binding.wordCount.text = state.todayStats.uniqueWords.toString()

                    // Top words
                    binding.topWords1.text = if (state.topWords.isNotEmpty()) {
                        "${state.topWords[0].word} (${state.topWords[0].count})"
                    } else "Chưa có dữ liệu"
                    binding.topWords2.text = if (state.topWords.size > 1) {
                        "${state.topWords[1].word} (${state.topWords[1].count})"
                    } else ""
                    binding.topWords3.text = if (state.topWords.size > 2) {
                        "${state.topWords[2].word} (${state.topWords[2].count})"
                    } else ""

                    // Weekly chart - simple bar representation
                    val maxCount = state.weeklyStats.maxOfOrNull { it.sentencesCount } ?: 1
                    binding.chartBar1.apply {
                        val params = layoutParams
                        params.height = if (maxCount > 0) {
                            (state.weeklyStats.getOrNull(0)?.sentencesCount ?: 0) * 80 / maxCount
                        } else 4
                        layoutParams = params
                    }
                    binding.chartBar2.apply {
                        val params = layoutParams
                        params.height = if (maxCount > 0) {
                            (state.weeklyStats.getOrNull(1)?.sentencesCount ?: 0) * 80 / maxCount
                        } else 4
                        layoutParams = params
                    }
                    binding.chartBar3.apply {
                        val params = layoutParams
                        params.height = if (maxCount > 0) {
                            (state.weeklyStats.getOrNull(2)?.sentencesCount ?: 0) * 80 / maxCount
                        } else 4
                        layoutParams = params
                    }
                }
            }
        }
    }
}
