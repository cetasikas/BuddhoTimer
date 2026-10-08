package com.samadhi.meditationtimer.ui.screens.screentime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.data.ScreenTimePreferences
import com.samadhi.meditationtimer.service.DayUsage
import com.samadhi.meditationtimer.service.HourlyUsage
import com.samadhi.meditationtimer.service.MonthDayUsage
import com.samadhi.meditationtimer.service.UsageStatsHelper
import com.samadhi.meditationtimer.ui.components.ManualDurationInputDialog
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared intensity scale for every usage visual on this screen.
 *
 * One scale is used for hourly bars, weekly bars, the month grid and unlocks so
 * the same shade always means the same amount. The ramp is deliberately bounded:
 * it lightens towards the surface at the low end and saturates towards the theme
 * accent at the high end. There is no red end and no numeric scale, because a
 * heavy day should read as information rather than as a verdict.
 */
@Composable
fun usageColor(fraction: Float, hasData: Boolean = true): Color {
    if (!hasData) return ZenSurfaceVariant
    val accent = LocalZenTheme.current.primary
    // sqrt keeps small values distinguishable instead of collapsing them to nothing.
    val t = (0.16f + 0.72f * sqrt(fraction.coerceIn(0f, 1f))).coerceIn(0f, 1f)
    // The low end stays clearly visible. Fading a light accent towards
    // transparent made light days read as empty, which is indistinguishable
    // from a day with no data at all.
    return lerp(accent.copy(alpha = 0.34f), accent, t)
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    // rememberSaveable so a collapse survives scrolling the LazyColumn far
    // enough for this item to leave composition.
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val currentTheme = LocalZenTheme.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.85f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = ZenTextMuted
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = currentTheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

/**
 * Optional daily target.
 *
 * Presented as a plain choice rather than a commitment device: no streaks, no
 * missed-day language, and "Off" is a first-class option. The preset row covers
 * the common targets and a custom entry opens a numeric dialog for anything
 * else, so the intention can be set to any length of day. Selecting it feeds
 * the reference marks on every chart below.
 */
@Composable
fun DailyGoalSection(
    goalMinutes: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    var showEdit by remember { mutableStateOf(false) }
    val isCustom = goalMinutes > 0 && goalMinutes !in ScreenTimePreferences.goalPresets
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.85f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Daily Intention",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = ZenTextPrimary
            )
            Text(
                text = if (goalMinutes > 0) {
                    "A MARKER TO AIM AT, NOT A SCORE"
                } else {
                    "OPTIONAL - SET ONE IF IT HELPS"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontSize = 10.sp
                ),
                color = ZenTextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScreenTimePreferences.goalPresets.forEach { minutes ->
                    val selected = goalMinutes == minutes
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) currentTheme.primary else ZenSurfaceVariant,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable { onSelect(minutes) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = ScreenTimePreferences.presetLabel(minutes),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (selected) ZenObsidian else ZenTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { showEdit = true }
                    .padding(horizontal = 2.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = if (isCustom) currentTheme.primary else ZenTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = if (isCustom) {
                        "Custom \u00B7 ${ScreenTimePreferences.presetLabel(goalMinutes)}"
                    } else {
                        "Set your own amount"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                    color = if (isCustom) currentTheme.primary else ZenTextSecondary
                )
            }
        }

        if (showEdit) {
            ManualDurationInputDialog(
                initialMinutes = if (goalMinutes > 0) goalMinutes else 120,
                minMinutes = 5,
                maxMinutes = 24 * 60,
                title = "Set Your Daily Intention",
                subtitle = "Minutes of screen time per day",
                onDismiss = { showEdit = false },
                onConfirm = { minutes ->
                    onSelect(minutes)
                    showEdit = false
                }
            )
        }
    }
}

/**
 * Foreground time for each hour of today, one bar per hour.
 *
 * Bar height is the real total for that hour and the fill uses the shared
 * intensity scale, so a glance across the chart shows both shape and magnitude.
 * When a goal is set, a flat dashed line marks the even pace across the day
 * (the daily goal divided by 24), which shares units with the bars: anything
 * taller than the line is running above that pace for the hour.
 */
@Composable
fun HourlyUsageChart(
    hourly: List<HourlyUsage>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (hourly.isEmpty()) return
    val currentTheme = LocalZenTheme.current
    val accent = currentTheme.primary
    val borderColor = ZenBorder

    val totals = remember(hourly) { hourly.map { it.durationMs } }
    val goalMs = remember(goalMinutes) { goalMinutes * 60_000L }
    // Even pace is a per-hour reference (the day's goal over 24 hours), so it
    // is in the same unit as the bars and only lifts the ceiling when it is
    // taller than the busiest hour. Using the whole-day goal as the ceiling
    // squashed every bar into the bottom fifth of the chart.
    val paceMs = remember(goalMs) { if (goalMs > 0) goalMs / 24 else 0L }
    val peak = remember(totals, paceMs) {
        maxOf(totals.maxOrNull() ?: 0L, paceMs, 1L)
    }
    val nowHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val hasAnyUsage = totals.any { it > 0 }

    // Theme colors and the intensity scale are composable, so they cannot be
    // resolved inside the Canvas draw lambda.
    val usagePeak = remember(totals) { totals.maxOrNull()?.coerceAtLeast(1L) ?: 1L }
    val barColors = totals.map { usageColor(it.toFloat() / usagePeak) }
    val trackColor = ZenSurfaceVariant.copy(alpha = 0.55f)
    val futureShade = Color.Black.copy(alpha = 0.18f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (!hasAnyUsage) {
            Text(
                text = "No usage recorded yet today.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = ZenTextMuted
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            val count = totals.size
            val slot = if (count > 0) size.width / count else size.width
            val barWidth = slot * 0.62f
            val gap = (slot - barWidth) / 2f
            val trackHeight = size.height * 0.88f

            totals.forEachIndexed { index, ms ->
                val fraction = (ms.toFloat() / peak).coerceIn(0f, 1f)
                val barHeight = (fraction * trackHeight).coerceAtLeast(if (ms > 0) 2f else 0f)
                val x = index * slot + gap
                val top = size.height - barHeight

                // Track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, size.height - trackHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, trackHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                )

                if (barHeight > 0f) {
                    drawRoundRect(
                        color = barColors.getOrElse(index) { accent },
                        topLeft = Offset(x, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                    )
                }

                // Shade hours that have not happened yet.
                if (index > nowHour) {
                    drawRoundRect(
                        color = futureShade,
                        topLeft = Offset(x, size.height - trackHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                    )
                }
            }

            if (paceMs > 0) {
                // Flat dashed line at the even hourly pace. A diagonal that
                // compared cumulative goal progress with per-hour bars mixed
                // two different units, which read as noise; a level line at
                // the same unit reads as a calm reference.
                val paceY = size.height - (paceMs.toFloat() / peak) * trackHeight
                drawLine(
                    color = accent.copy(alpha = 0.75f),
                    start = Offset(0f, paceY),
                    end = Offset(size.width, paceY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            hourly.forEachIndexed { index, entry ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (index % 3 == 0) {
                        Text(
                            text = "%02d".format(entry.hour),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = ZenTextMuted
                        )
                    }
                }
            }
        }

        val busiest = totals.withIndex().maxByOrNull { it.value }
        val paceMinutes = ((paceMs / 60_000.0).roundToInt()).coerceAtLeast(1)
        val paceCaption = if (goalMs > 0) {
            "Dashed line is an even ${ScreenTimePreferences.presetLabel(paceMinutes)} per hour " +
                "toward your ${ScreenTimePreferences.presetLabel(goalMinutes)} intention."
        } else {
            ""
        }
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (busiest != null && busiest.value > 0) {
                buildString {
                    append(
                        "Busiest hour %02d:00 with %s. ".format(
                            busiest.index,
                            UsageStatsHelper.formatDuration(busiest.value)
                        )
                    )
                    append(paceCaption)
                }
            } else {
                paceCaption
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = ZenTextSecondary
        )
    }
}

/**
 * Compact day label for the week charts. "Today" stays whole, "Yesterday"
 * keeps four characters instead of the generic three-letter truncation that
 * turned it into "Yes".
 */
private fun weekBarLabel(dayName: String): String = when (dayName) {
    "Today" -> "Today"
    "Yesterday" -> "Yest"
    else -> dayName.take(3)
}

/**
 * Past week as horizontal bars using the shared intensity scale.
 *
 * When a goal is set, each row carries a thin tick at the goal position so the
 * day can be read against it directly.
 */
@Composable
fun WeeklyBarChart(
    days: List<DayUsage>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val currentTheme = LocalZenTheme.current
    val goalMs = remember(goalMinutes) { goalMinutes * 60_000L }
    val peak = remember(days, goalMs) {
        maxOf(days.maxOfOrNull { it.durationMs } ?: 0L, if (goalMs > 0) goalMs else 0L, 1L)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        days.forEach { day ->
            val fraction = (day.durationMs.toFloat() / peak).coerceIn(0f, 1f)
            val metGoal = goalMs > 0 && day.durationMs >= goalMs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekBarLabel(day.dayName),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (day.dayName == "Today") currentTheme.primaryLight else ZenTextSecondary,
                    modifier = Modifier.width(44.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(ZenSurfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(16.dp)
                                .background(usageColor(fraction), RoundedCornerShape(8.dp))
                        )
                    }
                    if (goalMs > 0) {
                        val goalFraction = (goalMs.toFloat() / peak).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(goalFraction)
                                .height(16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(16.dp)
                                    .background(
                                        if (metGoal) ZenTextMuted else currentTheme.primaryLight
                                    )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = UsageStatsHelper.formatDuration(day.durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (day.dayName == "Today") currentTheme.primaryLight else ZenTextMuted,
                    modifier = Modifier.width(58.dp)
                )
            }
        }

        if (goalMs > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The marker on each bar is your ${ScreenTimePreferences.presetLabel(goalMinutes)} intention.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = ZenTextMuted
            )
        }
    }
}

/**
 * The current calendar month as a grid of days.
 *
 * Each day carries a progress ring showing its usage against the daily goal,
 * wrapped around a fill that uses the shared intensity scale. A ring that has
 * closed simply reads as complete, with no overshoot or penalty styling.
 */
@Composable
fun MonthlyCalendar(
    monthDays: List<MonthDayUsage>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (monthDays.isEmpty()) return
    val goalMs = remember(goalMinutes) { goalMinutes * 60_000L }

    val (leadingBlanks, peak) = remember(monthDays) {
        val anchor = Calendar.getInstance().apply {
            // Anchor on the first of the month, not on today. Subtracting
            // (dayOfMonth - 1) from the first entry is always zero, which
            // silently derived the weekday from today and shifted the whole grid.
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val blanks = if (anchor.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            0
        } else {
            anchor.get(Calendar.DAY_OF_WEEK) - 1
        }
        blanks to (monthDays.maxOfOrNull { it.durationMs }?.coerceAtLeast(1L) ?: 1L)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = ZenTextMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val cells = leadingBlanks + monthDays.size
        val rows = (cells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val day = monthDays.getOrNull(row * 7 + column - leadingBlanks)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            DayCircle(
                                day = day,
                                peak = peak,
                                goalMs = goalMs,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (goalMs > 0) {
                "Ring shows progress toward your daily intention. Depth of colour shows the day's total."
            } else {
                "Deeper colour means more screen time. No score, just a picture of the month."
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = ZenTextMuted
        )
    }
}

@Composable
private fun DayCircle(
    day: MonthDayUsage,
    peak: Long,
    goalMs: Long,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    val accent = currentTheme.primary
    val ringTrack = ZenSurfaceVariant
    val fraction = (day.durationMs.toFloat() / peak).coerceIn(0f, 1f)
    val goalFraction = if (goalMs > 0) {
        (day.durationMs.toFloat() / goalMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    // Resolved up front: the intensity scale is composable and cannot be read
    // from inside the Canvas draw lambda.
    val ringColor = usageColor(fraction, hasData = day.hasData)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Ring showing how much of this month's busiest day this day used.
        // fillMaxSize, not fillMaxWidth: a Canvas only constrains width here and
        // would otherwise wrap to zero height, collapsing the arc to nothing.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - stroke,
                size.height - stroke
            )
            val topLeft = Offset(inset, inset)

            drawArc(
                color = if (day.isFuture) Color.Transparent else ringTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )

            if (!day.isFuture && day.hasData && fraction > 0f) {
                // Always drawn from the day's own usage. Previously this only
                // rendered when a goal was set, so with the default "Off" goal
                // every day showed an empty track.
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
            }

            // Goal marker: where this day sits against the chosen intention.
            if (!day.isFuture && goalMs > 0 && goalFraction < 1f) {
                drawLine(
                    color = accent,
                    start = Offset(
                        x = size.width / 2f + (arcSize.width / 2f) * cos(Math.toRadians((-90f + 360f * goalFraction).toDouble())).toFloat(),
                        y = size.height / 2f + (arcSize.height / 2f) * sin(Math.toRadians((-90f + 360f * goalFraction).toDouble())).toFloat()
                    ),
                    end = Offset(
                        x = size.width / 2f + (arcSize.width / 2f + 4.dp.toPx()) * cos(Math.toRadians((-90f + 360f * goalFraction).toDouble())).toFloat(),
                        y = size.height / 2f + (arcSize.height / 2f + 4.dp.toPx()) * sin(Math.toRadians((-90f + 360f * goalFraction).toDouble())).toFloat()
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .aspectRatio(1f)
                .background(
                    color = when {
                        day.isFuture -> Color.Transparent
                        !day.hasData -> ZenSurfaceVariant.copy(alpha = 0.5f)
                        else -> usageColor(fraction)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${day.dayOfMonth}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    day.isFuture -> ZenTextMuted.copy(alpha = 0.35f)
                    day.isToday -> currentTheme.primaryLight
                    !day.hasData -> ZenTextMuted
                    fraction > 0.15f -> ZenObsidian
                    else -> ZenTextSecondary
                }
            )
        }
    }
}

/**
 * Week of unlocks, coloured by the same intensity scale as the usage charts.
 */
@Composable
fun UnlocksWeekChart(
    days: List<DayUsage>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val peak = remember(days) { days.maxOfOrNull { it.unlocks }?.coerceAtLeast(1) ?: 1 }
    val todayUnlocks = days.lastOrNull()?.unlocks ?: 0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$todayUnlocks",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = usageColor(todayUnlocks.toFloat() / peak)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (todayUnlocks == 1) "unlock" else "unlocks",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = ZenTextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "How many times you unlocked your phone. Wakes that stayed on the lock screen don't count. Darker means more.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = ZenTextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            days.forEach { day ->
                val fraction = (day.unlocks.toFloat() / peak).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (day.unlocks > 0) "${day.unlocks}" else "",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ZenTextMuted
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((34f * fraction).coerceAtLeast(if (day.unlocks > 0) 3f else 1f).dp)
                                .background(
                                    if (day.unlocks > 0) usageColor(fraction) else ZenSurfaceVariant,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weekBarLabel(day.dayName),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (day.dayName == "Today") {
                            LocalZenTheme.current.primaryLight
                        } else {
                            ZenTextSecondary
                        }
                    )
                }
            }
        }
    }
}

/**
 * First-run prompt asking whether to turn on screen time tracking.
 *
 * Deliberately inline rather than a dialog: it explains what the feature does and
 * what it costs before asking, and both answers are equally weighted so declining
 * does not feel like a failure. Nothing is collected beyond the usage stats the
 * system already exposes to this app.
 */
@Composable
fun TrackingPromptCard(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.9f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(currentTheme.primary.copy(alpha = 0.35f))
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "See your screen time?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = ZenTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You can see where your attention actually goes, " +
                    "and how that sits alongside your practice. " +
                    "Everything stays on this phone. No account, no ads, no tracking of you.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = ZenTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(currentTheme.primary, RoundedCornerShape(10.dp))
                        .clickable { onEnable() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Turn On",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ZenObsidian
                    )
                }
                Box(
                    modifier = Modifier
                        .background(ZenSurfaceVariant, RoundedCornerShape(10.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Not Now",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = ZenTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun HourlyUsageSection(
    hourly: List<HourlyUsage>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (hourly.none { it.durationMs > 0 }) return
    ChartCard(title = "Hour by Hour", subtitle = "TODAY'S RHYTHM", modifier = modifier) {
        HourlyUsageChart(hourly = hourly, goalMinutes = goalMinutes)
    }
}

@Composable
fun WeeklyBarSection(
    days: List<DayUsage>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    ChartCard(title = "This Week", subtitle = "PAST 7 DAYS", modifier = modifier) {
        WeeklyBarChart(days = days, goalMinutes = goalMinutes)
    }
}

@Composable
fun MonthlyCalendarSection(
    monthDays: List<MonthDayUsage>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (monthDays.isEmpty()) return
    val title = remember {
        val names = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        names[Calendar.getInstance().get(Calendar.MONTH)]
    }
    ChartCard(
        title = title,
        subtitle = "MONTH IN VIEW",
        modifier = modifier
    ) {
        MonthlyCalendar(monthDays = monthDays, goalMinutes = goalMinutes)
    }
}

@Composable
fun UnlocksWeekSection(
    days: List<DayUsage>,
    unlocksAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    ChartCard(
        title = "Unlocks",
        subtitle = "SCREEN-ON TO UNLOCK",
        modifier = modifier
    ) {
        if (unlocksAvailable) {
            UnlocksWeekChart(days = days)
        } else {
            Text(
                text = "Unlock tracking needs Android 10 or newer. Your app-by-app times still work.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = ZenTextMuted
            )
        }
    }
}
