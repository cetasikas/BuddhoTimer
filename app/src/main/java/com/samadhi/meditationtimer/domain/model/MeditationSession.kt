package com.samadhi.meditationtimer.domain.model

data class MeditationSession(
    val id: Long = 0,
    val startTimeEpochMs: Long,
    val completedTimeEpochMs: Long,
    val targetDurationSec: Int,
    val actualDurationSec: Int,
    val overrunSec: Int,
    val rating: Int = 0,
    val notesMarkdown: String = "",
    val completedSuccessfully: Boolean = true
) {
    val totalMinutes: Int
        get() = actualDurationSec / 60

    val overrunMinutes: Int
        get() = overrunSec / 60

    val isOverrunOccurred: Boolean
        get() = overrunSec > 0
}
