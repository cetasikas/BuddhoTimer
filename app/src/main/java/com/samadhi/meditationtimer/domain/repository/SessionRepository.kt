package com.samadhi.meditationtimer.domain.repository

import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.model.PracticePeriodStats
import com.samadhi.meditationtimer.domain.model.SessionStats
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessionsFlow(): Flow<List<MeditationSession>>
    fun getSessionByIdFlow(id: Long): Flow<MeditationSession?>
    suspend fun getSessionById(id: Long): MeditationSession?
    suspend fun insertSession(session: MeditationSession): Long
    suspend fun updateSession(session: MeditationSession)
    suspend fun deleteSession(session: MeditationSession)
    suspend fun deleteAllSessions()
    suspend fun insertAll(sessions: List<MeditationSession>)
    fun getSessionStatsFlow(): Flow<SessionStats>
    /** Logs a session that finished, whether or not it is saved to History. */
    suspend fun recordCompletion(completedAtMs: Long, durationSec: Int, isBreathing: Boolean)
    /** Practice completed since local midnight, from every finished session. */
    suspend fun getTodayPracticeStats(): PracticePeriodStats
}
