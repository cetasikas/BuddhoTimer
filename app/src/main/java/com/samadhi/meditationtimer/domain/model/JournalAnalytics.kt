package com.samadhi.meditationtimer.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** One week of practice for the trend chart. */
data class WeekBucket(val label: String, val minutes: Long, val sessions: Int)

/** Practice minutes grouped into a coarse part of the day. */
data class TimeBucket(val label: String, val minutes: Long)

/**
 * Derived, read-only view of the meditation journal used by the analytics
 * section. Pure computation from the saved sessions so it can be rebuilt
 * cheaply whenever the journal changes.
 */
data class JournalAnalytics(
    val totalSessions: Int = 0,
    val totalMinutes: Long = 0,
    val averageMinutes: Int = 0,
    val medianMinutes: Int = 0,
    val longestSessionMinutes: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val activeDays: Int = 0,
    val thisWeekMinutes: Long = 0,
    val lastWeekMinutes: Long = 0,
    val weeklyTrend: List<WeekBucket> = emptyList(),
    val moodCounts: Map<Int, Int> = emptyMap(),
    val timeOfDay: List<TimeBucket> = emptyList(),
    val last30Days: List<Boolean> = emptyList()
) {
    val totalHours: Long get() = totalMinutes / 60

    /** Percentage change of this week vs last week, null when there is no baseline. */
    val weekOverWeekPercent: Int?
        get() = when {
            lastWeekMinutes <= 0L && thisWeekMinutes <= 0L -> null
            lastWeekMinutes <= 0L -> 100
            else -> (((thisWeekMinutes - lastWeekMinutes) * 100) / lastWeekMinutes).toInt()
        }

    companion object {
        private val weekLabel = DateTimeFormatter.ofPattern("M/d")

        private val timeLabels = listOf(
            "Night",     // 00:00 - 05:59
            "Morning",   // 06:00 - 11:59
            "Afternoon", // 12:00 - 16:59
            "Evening",   // 17:00 - 20:59
            "Late"       // 21:00 - 23:59
        )

        fun from(sessions: List<MeditationSession>): JournalAnalytics {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val lastWeekStart = thisWeekStart.minusWeeks(1)

            val durations = sessions.map { it.actualDurationSec / 60 }.sorted()
            val totalMinutes = sessions.sumOf { it.actualDurationSec.toLong() } / 60

            fun localDate(session: MeditationSession): LocalDate =
                Instant.ofEpochMilli(session.completedTimeEpochMs).atZone(zone).toLocalDate()

            val uniqueDates = sessions.map { localDate(it) }.distinct().sortedDescending()
            val (currentStreak, longestStreak) = calculateStreaks(uniqueDates, today)

            val thisWeekMinutes = sessions
                .filter { !localDate(it).isBefore(thisWeekStart) }
                .sumOf { it.actualDurationSec.toLong() } / 60
            val lastWeekMinutes = sessions
                .filter { val d = localDate(it); !d.isBefore(lastWeekStart) && d.isBefore(thisWeekStart) }
                .sumOf { it.actualDurationSec.toLong() } / 60

            val trend = (0 until 8).reversed().map { weeksAgo ->
                val start = thisWeekStart.minusWeeks(weeksAgo.toLong())
                val end = start.plusWeeks(1)
                val inWeek = sessions.filter { val d = localDate(it); !d.isBefore(start) && d.isBefore(end) }
                WeekBucket(
                    label = start.format(weekLabel),
                    minutes = inWeek.sumOf { it.actualDurationSec.toLong() } / 60,
                    sessions = inWeek.size
                )
            }

            val moodCounts = sessions
                .filter { it.rating in 1..5 }
                .groupingBy { it.rating }
                .eachCount()

            val timeOfDay = timeLabels.mapIndexed { index, label ->
                val minutes = sessions
                    .filter { bucketFor(Instant.ofEpochMilli(it.completedTimeEpochMs).atZone(zone).hour) == index }
                    .sumOf { it.actualDurationSec.toLong() } / 60
                TimeBucket(label, minutes)
            }

            val last30Days = (29 downTo 0).map { daysAgo ->
                uniqueDates.contains(today.minusDays(daysAgo.toLong()))
            }

            return JournalAnalytics(
                totalSessions = sessions.size,
                totalMinutes = totalMinutes,
                averageMinutes = if (durations.isEmpty()) 0 else durations.average().toInt(),
                medianMinutes = if (durations.isEmpty()) 0 else durations[durations.size / 2],
                longestSessionMinutes = durations.lastOrNull() ?: 0,
                currentStreakDays = currentStreak,
                longestStreakDays = longestStreak,
                activeDays = uniqueDates.size,
                thisWeekMinutes = thisWeekMinutes,
                lastWeekMinutes = lastWeekMinutes,
                weeklyTrend = trend,
                moodCounts = moodCounts,
                timeOfDay = timeOfDay,
                last30Days = last30Days
            )
        }

        private fun bucketFor(hour: Int): Int = when {
            hour < 6 -> 0
            hour < 12 -> 1
            hour < 17 -> 2
            hour < 21 -> 3
            else -> 4
        }

        private fun calculateStreaks(uniqueDatesDesc: List<LocalDate>, today: LocalDate): Pair<Int, Int> {
            if (uniqueDatesDesc.isEmpty()) return 0 to 0

            val dateSet = uniqueDatesDesc.toHashSet()
            val yesterday = today.minusDays(1)

            var current = 0
            var cursor = when {
                dateSet.contains(today) -> today
                dateSet.contains(yesterday) -> yesterday
                else -> null
            }
            while (cursor != null && dateSet.contains(cursor)) {
                current++
                cursor = cursor.minusDays(1)
            }

            var longest = 0
            var running = 0
            var prev: LocalDate? = null
            for (date in uniqueDatesDesc.sorted()) {
                running = if (prev != null && ChronoUnit.DAYS.between(prev, date) == 1L) running + 1 else 1
                if (running > longest) longest = running
                prev = date
            }

            return current to longest
        }

        fun empty(): JournalAnalytics = JournalAnalytics(
            weeklyTrend = (0 until 8).reversed().map { WeekBucket("", 0L, 0) },
            timeOfDay = timeLabels.map { TimeBucket(it, 0L) },
            last30Days = List(30) { false }
        )
    }
}
