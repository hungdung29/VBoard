package com.vboard.aac.ui.stats

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vboard.aac.databinding.ActivityStatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val viewModel: StatsViewModel by viewModels()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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

                    // Weekly chart - 7 days (oldest -> newest)
                    val bars = listOf(
                        binding.chartBar1,
                        binding.chartBar2,
                        binding.chartBar3,
                        binding.chartBar4,
                        binding.chartBar5,
                        binding.chartBar6,
                        binding.chartBar7
                    )
                    val labels = listOf(
                        binding.label1,
                        binding.label2,
                        binding.label3,
                        binding.label4,
                        binding.label5,
                        binding.label6,
                        binding.label7
                    )
                    val values = listOf(
                        binding.value1,
                        binding.value2,
                        binding.value3,
                        binding.value4,
                        binding.value5,
                        binding.value6,
                        binding.value7
                    )

                    val maxCount = state.weeklyStats.maxOfOrNull { it.sentencesCount } ?: 1
                    state.weeklyStats.forEachIndexed { index, stat ->
                        val height = if (maxCount > 0) {
                            (stat.sentencesCount * 80 / maxCount).coerceAtLeast(4)
                        } else 4
                        bars.getOrNull(index)?.let { bar ->
                            val params = bar.layoutParams
                            params.height = height
                            bar.layoutParams = params
                        }
                        values.getOrNull(index)?.text = stat.sentencesCount.toString()
                        labels.getOrNull(index)?.text = toWeekLabel(stat.date)
                    }
                }
            }
        }
    }

    private fun toWeekLabel(date: String): String {
        val parsed = LocalDate.parse(date, dateFormatter)
        return when (parsed.dayOfWeek) {
            DayOfWeek.SUNDAY -> "CN"
            else -> "T${parsed.dayOfWeek.value + 1}"
        }
    }
}
