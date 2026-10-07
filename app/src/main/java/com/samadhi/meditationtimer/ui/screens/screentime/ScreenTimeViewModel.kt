package com.samadhi.meditationtimer.ui.screens.screentime

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.data.ScreenTimePreferences
import com.samadhi.meditationtimer.domain.model.MindfulPrompts
import com.samadhi.meditationtimer.service.AppUsage
import com.samadhi.meditationtimer.service.DayUsage
import com.samadhi.meditationtimer.service.HourlyUsage
import com.samadhi.meditationtimer.service.MonthDayUsage
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
    /** Screen-ons today, i.e. phone pickups. */
    val todayPickups: Int = 0,
    /** False on API < 29, where pickups cannot be detected. */
    val pickupsAvailable: Boolean = false,
    /** Foreground time per hour of today, for the hour-of-day chart. */
    val hourlyToday: List<HourlyUsage> = emptyList(),
    /** Per-day totals for the current calendar month. */
    val monthDays: List<MonthDayUsage> = emptyList(),
    /** Daily foreground target in minutes, or 0 when no goal is set. */
    val goalMinutes: Int = 0,
    /** False until the user has answered the screen time tracking prompt. */
    val trackingPromptAnswered: Boolean = false,
    /** Whether the user has opted into screen time tracking. */
    val trackingEnabled: Boolean = false,
    /** Completed practice sessions today, for the daily mindfulness ratio. */
    val todayPracticeCount: Int = 0,
    val todayPracticeMinutes: Long = 0L,
    val isLoading: Boolean = true,
    val todayMindfulPrompt: String = ""
)

class ScreenTimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MeditationApp).sessionRepository
    private val preferences = ScreenTimePreferences(application)

    // The Usage Access permission can be checked synchronously and cheaply, so the
    // initial state already reflects the real permission instead of briefly showing
    // the "Enable Usage Access" card before the async refresh completes.
    private val _uiState = MutableStateFlow(
        ScreenTimeUiState(hasPermission = UsageStatsHelper.hasUsageAccessPermission(application))
    )
    val uiState: StateFlow<ScreenTimeUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            preferences.dailyGoalMinutes.collect { minutes ->
                _uiState.value = _uiState.value.copy(goalMinutes = minutes)
            }
        }
        viewModelScope.launch {
            preferences.trackingPromptAnswered.collect { answered ->
                _uiState.value = _uiState.value.copy(trackingPromptAnswered = answered)
            }
        }
        viewModelScope.launch {
            preferences.trackingEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(trackingEnabled = enabled)
            }
        }
    }

    fun setGoalMinutes(minutes: Int) {
        viewModelScope.launch {
            preferences.setDailyGoalMinutes(minutes)
        }
    }

    /**
     * User accepted tracking. The system permission grant is a separate step and
     * is handled by opening the Usage Access settings page.
     */
    fun enableTracking() {
        viewModelScope.launch {
            preferences.setTrackingEnabled(true)
        }
        UsageStatsHelper.openUsageAccessSettings(getApplication())
    }

    /**
     * User declined for now. This is remembered so the prompt does not reappear,
     * but it is not treated as a permanent refusal.
     */
    fun dismissTrackingPrompt() {
        viewModelScope.launch {
            preferences.setTrackingPromptAnswered(true)
        }
    }

    fun refresh() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val hasPermission = UsageStatsHelper.hasUsageAccessPermission(context)
            val snapshot = if (hasPermission) {
                UsageStatsHelper.getScreenTimeSnapshot(context, days = 7)
            } else {
                null
            }
            val todayPractice = repository.getTodayPracticeStats()

            _uiState.value = _uiState.value.copy(
                hasPermission = hasPermission,
                todayScreenTimeMs = snapshot?.todayTotalMs ?: 0L,
                topApps = snapshot?.todayApps ?: emptyList(),
                weeklyDays = snapshot?.days ?: emptyList(),
                todayPickups = snapshot?.todayPickups ?: 0,
                pickupsAvailable = snapshot?.pickupsAvailable ?: false,
                hourlyToday = snapshot?.hourlyToday ?: emptyList(),
                monthDays = snapshot?.monthDays ?: emptyList(),
                todayPracticeCount = todayPractice.sessionCount,
                todayPracticeMinutes = todayPractice.practiceMinutes,
                isLoading = false,
                todayMindfulPrompt = MindfulPrompts.getDailyPrompt()
            )
        }
    }

    fun openUsageAccessSettings() {
        UsageStatsHelper.openUsageAccessSettings(getApplication())
    }
}
