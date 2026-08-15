package com.samadhi.meditationtimer.data.backup.model

import com.samadhi.meditationtimer.domain.model.MeditationSession
import kotlinx.serialization.Serializable

@Serializable
data class BackupSessionDto(
    val id: Long,
    val startTimeEpochMs: Long,
    val completedTimeEpochMs: Long,
    val targetDurationSec: Int,
    val actualDurationSec: Int,
    val overrunSec: Int,
    val rating: Int = 0,
    val notesMarkdown: String = "",
    val completedSuccessfully: Boolean = true
) {
    fun toDomain(): MeditationSession {
        return MeditationSession(
            id = id,
            startTimeEpochMs = startTimeEpochMs,
            completedTimeEpochMs = completedTimeEpochMs,
            targetDurationSec = targetDurationSec,
            actualDurationSec = actualDurationSec,
            overrunSec = overrunSec,
            rating = rating,
            notesMarkdown = notesMarkdown,
            completedSuccessfully = completedSuccessfully
        )
    }

    companion object {
        fun fromDomain(domain: MeditationSession): BackupSessionDto {
            return BackupSessionDto(
                id = domain.id,
                startTimeEpochMs = domain.startTimeEpochMs,
                completedTimeEpochMs = domain.completedTimeEpochMs,
                targetDurationSec = domain.targetDurationSec,
                actualDurationSec = domain.actualDurationSec,
                overrunSec = domain.overrunSec,
                rating = domain.rating,
                notesMarkdown = domain.notesMarkdown,
                completedSuccessfully = domain.completedSuccessfully
            )
        }
    }
}

@Serializable
data class BackupRootDto(
    val version: Int = 1,
    val exportTimestampEpochMs: Long = System.currentTimeMillis(),
    val appName: String = "Samadhi Meditation Timer",
    val sessions: List<BackupSessionDto> = emptyList()
)
