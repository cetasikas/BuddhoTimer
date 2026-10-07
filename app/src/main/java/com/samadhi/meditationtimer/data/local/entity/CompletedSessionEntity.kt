package com.samadhi.meditationtimer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A lightweight, append-only record of every completed practice session,
 * independent of whether it is later saved to History. Feeds period-based
 * comparisons like the daily mindfulness ratio so that "finished" always
 * means finished, regardless of the summary-screen save/discard choice.
 */
@Entity(tableName = "completed_sessions")
data class CompletedSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedTimeEpochMs: Long,
    val actualDurationSec: Int,
    val isBreathing: Boolean = false
)