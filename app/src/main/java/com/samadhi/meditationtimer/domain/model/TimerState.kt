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
        val completedTimeEpochMs: Long,
        val isBoxBreathing: Boolean = false
    ) : TimerStatus()
}

data class TimerConfiguration(
    val targetDurationMinutes: Int = 15,
    val prepTimeSeconds: Int = 5,
    val soundBellResName: String = "tibetan_singing_bowl",
    val isAutoDndEnabled: Boolean = false,
    val isOpenEnded: Boolean = false,
    val isOverrunEnabled: Boolean = false,
    val showLeavesDuringMeditation: Boolean = true,
    val meditationLeafCount: Int = 16,
    val mindfulnessIntervalMinutes: Int = 0,
    val tapToBlackoutEnabled: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val isBoxBreathingMode: Boolean = false,
    val isCountdownHapticsEnabled: Boolean = true,
    val isCompletionBellEnabled: Boolean = true,
    val isStartBellEnabled: Boolean = false
) {
    val targetDurationSeconds: Int
        get() = if (isOpenEnded) 0 else targetDurationMinutes * 60
}
