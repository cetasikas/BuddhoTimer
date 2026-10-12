package com.samadhi.meditationtimer.ui.screens.journal

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.data.backup.BackupManager
import com.samadhi.meditationtimer.domain.model.JournalAnalytics
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.repository.SessionRepository
import com.samadhi.meditationtimer.widget.PracticeWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SessionRepository =
        (application as MeditationApp).sessionRepository
    private val backupManager = BackupManager(application.applicationContext, repository)
    private val prefs = application.getSharedPreferences("journal_prefs", android.content.Context.MODE_PRIVATE)

    private val allSessions = repository.getAllSessionsFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _entryType = MutableStateFlow("all")
    val entryType: StateFlow<String> = _entryType.asStateFlow()

    private val _rating = MutableStateFlow(0)
    val rating: StateFlow<Int> = _rating.asStateFlow()

    private val _minMinutes = MutableStateFlow(0)
    val minMinutes: StateFlow<Int> = _minMinutes.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _dailyGoalMinutes = MutableStateFlow(prefs.getInt("daily_goal_minutes", 15))
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes.asStateFlow()

    private val _restDayDates = MutableStateFlow(loadRestDays())
    val restDayDates: StateFlow<Set<String>> = _restDayDates.asStateFlow()

    val sessions: StateFlow<List<MeditationSession>> = combine(
        allSessions,
        _query,
        _entryType,
        _rating,
        _minMinutes
    ) { sessions, query, entryType, rating, minMinutes ->
        sessions.filter { session ->
            val matchesQuery = query.isBlank() || session.notesMarkdown.contains(query, ignoreCase = true)
            val matchesType = when (entryType) {
                "manual" -> session.isManual
                "timer" -> !session.isManual
                else -> true
            }
            val matchesRating = rating == 0 || session.rating == rating
            val matchesMin = minMinutes == 0 || session.actualDurationSec >= minMinutes * 60
            matchesQuery && matchesType && matchesRating && matchesMin
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analytics: StateFlow<JournalAnalytics> = combine(allSessions, _restDayDates) { sessions, restDays ->
        JournalAnalytics.from(sessions, restDays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalAnalytics.empty())

    val allJournalSessions: StateFlow<List<MeditationSession>> = allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(value: String) { _query.value = value }
    fun updateEntryType(value: String) { _entryType.value = value }
    fun updateRating(value: Int) { _rating.value = value }
    fun updateMinMinutes(value: Int) { _minMinutes.value = value }
    fun clearFilters() {
        _query.value = ""
        _entryType.value = "all"
        _rating.value = 0
        _minMinutes.value = 0
    }

    fun clearStatusMessage() { _statusMessage.value = null }

    fun updateDailyGoalMinutes(minutes: Int) {
        _dailyGoalMinutes.value = minutes
        prefs.edit().putInt("daily_goal_minutes", minutes).apply()
    }

    fun useRestDayToday() {
        val today = LocalDate.now().toString()
        val next = _restDayDates.value + today
        _restDayDates.value = next
        prefs.edit().putStringSet("rest_day_dates", next).apply()
        _statusMessage.value = "Rest day noted. The rhythm continues."
        PracticeWidgetProvider.updateAll(getApplication())
    }

    fun exportJournalMarkdown(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportJournalMarkdown(uri)
                .onSuccess { _statusMessage.value = "Exported $it journal entries to Markdown" }
                .onFailure { _statusMessage.value = "Export failed: ${it.localizedMessage}" }
        }
    }

    fun exportJournalJson(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportToJson(uri)
                .onSuccess { _statusMessage.value = "Exported $it journal entries to JSON" }
                .onFailure { _statusMessage.value = "Export failed: ${it.localizedMessage}" }
        }
    }

    private fun loadRestDays(): Set<String> = prefs.getStringSet("rest_day_dates", emptySet()) ?: emptySet()

    fun deleteSession(session: MeditationSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            PracticeWidgetProvider.updateAll(getApplication())
        }
    }

    /**
     * Inserts or updates a journal entry. The DAO uses REPLACE on the primary
     * key, so a session with id == 0 creates a new row and a session with an
     * existing id overwrites it.
     */
    fun saveSession(session: MeditationSession) {
        viewModelScope.launch {
            repository.insertSession(session)
            PracticeWidgetProvider.updateAll(getApplication())
        }
    }
}
