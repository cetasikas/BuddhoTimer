package com.samadhi.meditationtimer.ui.screens.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.JournalAnalytics
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.components.MarkdownRenderer
import com.samadhi.meditationtimer.ui.components.moodOptions
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenDanger
import com.samadhi.meditationtimer.ui.theme.ZenOverrunBlue
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTealLight
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onEditSession: (MeditationSession) -> Unit,
    onLogManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    val sessions by viewModel.sessions.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    var sessionToDelete by remember { mutableStateOf<MeditationSession?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FallingLeavesEffect(leafCount = 14, ambientAlpha = 0.30f)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Journal",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = ZenTextPrimary
                )
                Text(
                    text = "Reflect on your consistency and insights",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZenTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ZenSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(
                                title = "Sessions",
                                value = "${analytics.totalSessions}",
                                icon = Icons.Default.SelfImprovement,
                                tint = currentTheme.primaryLight
                            )
                            StatItem(
                                title = "Total Time",
                                value = formatHoursMinutes(analytics.totalMinutes),
                                icon = Icons.Default.Timelapse,
                                tint = currentTheme.primary
                            )
                            StatItem(
                                title = "Streak",
                                value = "${analytics.currentStreakDays}d",
                                icon = Icons.Default.LocalFireDepartment,
                                tint = currentTheme.primaryLight
                            )
                            StatItem(
                                title = "Best",
                                value = "${analytics.longestStreakDays}d",
                                icon = Icons.Default.CalendarMonth,
                                tint = ZenTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                InsightsCard(analytics)

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = ZenTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No meditation entries yet.\nFinish a sit or log one manually to begin.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = ZenTextMuted),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "ENTRIES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = currentTheme.primary
                    )
                }
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onOpen = { onEditSession(session) },
                        onDeleteClick = { sessionToDelete = session }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        ExtendedFloatingActionButton(
            onClick = onLogManual,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = currentTheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Log a sit", fontWeight = FontWeight.SemiBold) }
        )

        sessionToDelete?.let { session ->
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text(text = "Delete Journal Entry", color = ZenTextPrimary) },
                text = { Text(text = "Are you sure you want to permanently delete this meditation entry and notes?", color = ZenTextSecondary) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSession(session)
                            sessionToDelete = null
                        }
                    ) {
                        Text(text = "Delete", color = ZenDanger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text(text = "Cancel", color = ZenTextSecondary)
                    }
                },
                containerColor = ZenSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun InsightsCard(analytics: JournalAnalytics) {
    val currentTheme = LocalZenTheme.current
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "insightsArrow")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Practice Insights",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenTextPrimary
                    )
                    Text(
                        text = if (expanded) "Tap to collapse" else "Tap to explore your patterns",
                        fontSize = 10.5.sp,
                        color = ZenTextMuted
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = currentTheme.primary,
                    modifier = Modifier.size(24.dp).rotate(rotation)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    WeekOverWeekRow(analytics)

                    Spacer(modifier = Modifier.height(18.dp))
                    ChartSectionTitle("Weekly Practice", "Minutes meditated per week, last 8 weeks")
                    WeeklyTrendChart(analytics.weeklyTrend)

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DetailStat("Average", "${analytics.averageMinutes}m")
                        DetailStat("Median", "${analytics.medianMinutes}m")
                        DetailStat("Longest", "${analytics.longestSessionMinutes}m")
                        DetailStat("Active Days", "${analytics.activeDays}")
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    ChartSectionTitle("State of Mind", "How your sessions felt")
                    MoodDistributionChart(analytics.moodCounts)

                    Spacer(modifier = Modifier.height(18.dp))
                    ChartSectionTitle("Time of Day", "When you tend to sit")
                    TimeOfDayChart(analytics.timeOfDay)

                    Spacer(modifier = Modifier.height(18.dp))
                    ChartSectionTitle("Last 30 Days", "Days you practiced")
                    ActivityStrip(analytics.last30Days)
                }
            }
        }
    }
}

@Composable
private fun WeekOverWeekRow(analytics: JournalAnalytics) {
    val currentTheme = LocalZenTheme.current
    val delta = analytics.weekOverWeekPercent
    val deltaColor = when {
        delta == null -> ZenTextMuted
        delta >= 0 -> ZenTealLight
        else -> ZenOverrunBlue
    }
    val deltaText = when {
        delta == null -> "No data yet"
        delta > 0 -> "+$delta% vs last week"
        delta == 0 -> "Same as last week"
        else -> "$delta% vs last week"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "THIS WEEK", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = currentTheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatHoursMinutes(analytics.thisWeekMinutes),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ZenTextPrimary
            )
        }
        Text(
            text = deltaText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = deltaColor
        )
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ZenTextPrimary)
        Text(text = label, fontSize = 10.sp, color = ZenTextSecondary)
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ZenTextPrimary)
        Text(text = title, fontSize = 11.sp, color = ZenTextSecondary)
    }
}

@Composable
private fun SessionCard(
    session: MeditationSession,
    onOpen: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val currentTheme = LocalZenTheme.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy • h:mm a") }
    val dateString = Instant.ofEpochMilli(session.completedTimeEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)

    val moodOption = moodOptions.find { it.level == session.rating }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = ZenSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = dateString, fontSize = 12.sp, color = ZenTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = NotificationHelper.formatSeconds(session.actualDurationSec),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZenTealLight
                        )
                        if (session.overrunSec > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ZenOverrunBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "+${NotificationHelper.formatSeconds(session.overrunSec)} overrun",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZenOverrunBlue
                                )
                            }
                        }
                        if (session.isManual) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ZenSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "logged manually",
                                    fontSize = 10.sp,
                                    color = ZenTextMuted
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (moodOption != null) {
                        Icon(
                            imageVector = moodOption.icon,
                            contentDescription = moodOption.title,
                            tint = currentTheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentTheme.surfaceVariant)
                                .padding(4.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Entry",
                            tint = ZenTextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Entry",
                            tint = ZenTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (session.notesMarkdown.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZenSurfaceVariant.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    MarkdownRenderer(markdown = session.notesMarkdown)
                }
            }
        }
    }
}

private fun formatHoursMinutes(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
