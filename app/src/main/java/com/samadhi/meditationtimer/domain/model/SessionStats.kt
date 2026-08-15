package com.samadhi.meditationtimer.domain.model

data class SessionStats(
    val totalSessionsCount: Int = 0,
    val totalPracticeMinutes: Long = 0L,
    val averageSessionMinutes: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val totalOverrunMinutes: Long = 0L
)
