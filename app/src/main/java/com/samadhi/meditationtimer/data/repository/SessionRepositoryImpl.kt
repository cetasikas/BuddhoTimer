package com.samadhi.meditationtimer.data.repository

import com.samadhi.meditationtimer.data.local.dao.SessionDao
import com.samadhi.meditationtimer.data.local.entity.SessionEntity
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.model.SessionStats
import com.samadhi.meditationtimer.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class SessionRepositoryImpl(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun getAllSessionsFlow(): Flow<List<MeditationSession>> {
        return sessionDao.getAllSessionsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSessionByIdFlow(id: Long): Flow<MeditationSession?> {
        return sessionDao.getSessionByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getSessionById(id: Long): MeditationSession? {
        return sessionDao.getSessionById(id)?.toDomain()
    }

    override suspend fun insertSession(session: MeditationSession): Long {
        return sessionDao.insertSession(SessionEntity.fromDomain(session))
    }

    override suspend fun updateSession(session: MeditationSession) {
        sessionDao.updateSession(SessionEntity.fromDomain(session))
    }

    override suspend fun deleteSession(session: MeditationSession) {
        sessionDao.deleteSession(SessionEntity.fromDomain(session))
    }

    override suspend fun deleteAllSessions() {
        sessionDao.deleteAllSessions()
    }

    override suspend fun insertAll(sessions: List<MeditationSession>) {
        sessionDao.insertAll(sessions.map { SessionEntity.fromDomain(it) })
    }

    override fun getSessionStatsFlow(): Flow<SessionStats> {
        return sessionDao.getAllSessionsFlow().map { entities ->
            if (entities.isEmpty()) {
                return@map SessionStats()
            }

            val totalCount = entities.size
            val totalSec = entities.sumOf { it.actualDurationSec.toLong() }
            val totalOverrunSec = entities.sumOf { it.overrunSec.toLong() }
            val avgSec = if (totalCount > 0) (totalSec / totalCount).toInt() else 0

            val streaks = calculateStreaks(entities)

            SessionStats(
                totalSessionsCount = totalCount,
                totalPracticeMinutes = totalSec / 60,
                averageSessionMinutes = avgSec / 60,
                currentStreakDays = streaks.first,
                longestStreakDays = streaks.second,
                totalOverrunMinutes = totalOverrunSec / 60
            )
        }
    }

    private fun calculateStreaks(entities: List<SessionEntity>): Pair<Int, Int> {
        if (entities.isEmpty()) return Pair(0, 0)

        val uniqueDates = entities
            .map {
                Instant.ofEpochMilli(it.completedTimeEpochMs)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .distinct()
            .sortedDescending()

        if (uniqueDates.isEmpty()) return Pair(0, 0)

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Calculate Current Streak
        var currentStreak = 0
        var checkDate = if (uniqueDates.contains(today)) today else if (uniqueDates.contains(yesterday)) yesterday else null

        if (checkDate != null) {
            for (date in uniqueDates) {
                if (date == checkDate) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                } else if (date.isBefore(checkDate)) {
                    break
                }
            }
        }

        // Calculate Longest Streak
        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        val ascendingDates = uniqueDates.sorted()
        for (date in ascendingDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else {
                val diffDays = ChronoUnit.DAYS.between(prevDate, date)
                if (diffDays == 1L) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevDate = date
        }

        return Pair(currentStreak, longestStreak)
    }
}
