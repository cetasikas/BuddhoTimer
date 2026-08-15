package com.samadhi.meditationtimer.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.model.SessionStats
import com.samadhi.meditationtimer.domain.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SessionRepository =
        (application as MeditationApp).sessionRepository

    val sessions: StateFlow<List<MeditationSession>> = repository.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<SessionStats> = repository.getSessionStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionStats())

    fun deleteSession(session: MeditationSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun saveSession(session: MeditationSession) {
        viewModelScope.launch {
            repository.insertSession(session)
        }
    }
}
