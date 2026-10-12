package com.samadhi.meditationtimer.ui.screens.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.domain.model.JournalAnalytics
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SessionRepository =
        (application as MeditationApp).sessionRepository

    val sessions: StateFlow<List<MeditationSession>> = repository.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analytics: StateFlow<JournalAnalytics> = repository.getAllSessionsFlow()
        .map { JournalAnalytics.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalAnalytics.empty())

    fun deleteSession(session: MeditationSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
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
        }
    }
}
