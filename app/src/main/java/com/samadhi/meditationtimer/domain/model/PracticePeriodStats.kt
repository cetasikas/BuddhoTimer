package com.samadhi.meditationtimer.domain.model

/** Practice captured within a bounded window (e.g. today) so it can be set
 *  beside the same window of another measure, like daily screen time. */
data class PracticePeriodStats(
    val sessionCount: Int = 0,
    val practiceMinutes: Long = 0L
)