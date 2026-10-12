package com.samadhi.meditationtimer.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samadhi.meditationtimer.domain.model.MeditationSession

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["startTimeEpochMs"], unique = false),
        Index(value = ["completedTimeEpochMs"], unique = false)
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimeEpochMs: Long,
    val completedTimeEpochMs: Long,
    val targetDurationSec: Int,
    val actualDurationSec: Int,
    val overrunSec: Int,
    val rating: Int = 0,
    val notesMarkdown: String = "",
    val completedSuccessfully: Boolean = true,
    val isManual: Boolean = false
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
            completedSuccessfully = completedSuccessfully,
            isManual = isManual
        )
    }

    companion object {
        fun fromDomain(domain: MeditationSession): SessionEntity {
            return SessionEntity(
                id = domain.id,
                startTimeEpochMs = domain.startTimeEpochMs,
                completedTimeEpochMs = domain.completedTimeEpochMs,
                targetDurationSec = domain.targetDurationSec,
                actualDurationSec = domain.actualDurationSec,
                overrunSec = domain.overrunSec,
                rating = domain.rating,
                notesMarkdown = domain.notesMarkdown,
                completedSuccessfully = domain.completedSuccessfully,
                isManual = domain.isManual
            )
        }
    }
}
