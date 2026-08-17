package com.samadhi.meditationtimer.domain.model

sealed class TimerStatus {
    object Idle : TimerStatus()
    data class Preparing(val remainingSeconds: Int) : TimerStatus()
    data class Running(val remainingSeconds: Int, val elapsedSeconds: Int, val isOverrun: Boolean) : TimerStatus()
    data class Paused(val remainingSeconds: Int, val elapsedSeconds: Int, val isOverrun: Boolean) : TimerStatus()
    data class Completed(
        val targetDurationSec: Int,
        val actualDurationSec: Int,
        val overrunSec: Int,
        val startTimeEpochMs: Long,
        val completedTimeEpochMs: Long
    ) : TimerStatus()
}

data class TimerConfiguration(
    val targetDurationMinutes: Int = 15,
    val prepTimeSeconds: Int = 5,
    val soundBellResName: String = "tibetan_singing_bowl",
    val isAutoDndEnabled: Boolean = true,
    val isOpenEnded: Boolean = false,
    val isOverrunEnabled: Boolean = true,
    val showLeavesDuringMeditation: Boolean = true,
    val meditationLeafCount: Int = 16
) {
    val targetDurationSeconds: Int
        get() = if (isOpenEnded) 0 else targetDurationMinutes * 60
}
