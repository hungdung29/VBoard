package com.vboard.aac.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vboard.aac.domain.model.DailyStats
import com.vboard.aac.domain.model.WordUsage
import com.vboard.aac.domain.repository.IStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val todayStats: DailyStats = DailyStats("", 0, 0),
    val weeklyStats: List<DailyStats> = emptyList(),
    val topWords: List<WordUsage> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepo: IStatsRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        statsRepo.getTodayStats(),
        statsRepo.getWeeklyStats(),
        statsRepo.getTopWords(4)
    ) { today, weekly, topWords ->
        StatsUiState(
            todayStats = today,
            weeklyStats = weekly,
            topWords = topWords
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}
