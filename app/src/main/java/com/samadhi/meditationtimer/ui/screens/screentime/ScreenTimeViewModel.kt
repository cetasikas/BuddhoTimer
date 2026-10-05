package com.samadhi.meditationtimer.ui.screens.screentime

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.domain.model.MindfulPrompts
import com.samadhi.meditationtimer.domain.model.SessionStats
import com.samadhi.meditationtimer.service.AppUsage
import com.samadhi.meditationtimer.service.DayUsage
import com.samadhi.meditationtimer.service.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScreenTimeUiState(
    val hasPermission: Boolean = false,
    val todayScreenTimeMs: Long = 0L,
    val topApps: List<AppUsage> = emptyList(),
    val weeklyDays: List<DayUsage> = emptyList(),
    val sessionStats: SessionStats = SessionStats(),
    val isLoading: Boolean = true,
    val todayMindfulPrompt: String = ""
)

class ScreenTimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MeditationApp).sessionRepository

    private val _uiState = MutableStateFlow(ScreenTimeUiState())
    val uiState: StateFlow<ScreenTimeUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repository.getSessionStatsFlow().collect { stats ->
                _uiState.value = _uiState.value.copy(sessionStats = stats)
            }
        }
    }

    fun refresh() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val hasPermission = UsageStatsHelper.hasUsageAccessPermission(context)
            val todayMs = if (hasPermission) UsageStatsHelper.getTodayScreenTimeMs(context) ?: 0L else 0L
            val topApps = if (hasPermission) UsageStatsHelper.getTopAppsToday(context, 5) else emptyList()
            val weeklyDays = if (hasPermission) UsageStatsHelper.getWeeklyScreenTime(context) else emptyList()

            _uiState.value = _uiState.value.copy(
                hasPermission = hasPermission,
                todayScreenTimeMs = todayMs,
                topApps = topApps,
                weeklyDays = weeklyDays,
                isLoading = false,
                todayMindfulPrompt = MindfulPrompts.getDailyPrompt()
            )
        }
    }

    fun openUsageAccessSettings() {
        UsageStatsHelper.openUsageAccessSettings(getApplication())
    }
}
