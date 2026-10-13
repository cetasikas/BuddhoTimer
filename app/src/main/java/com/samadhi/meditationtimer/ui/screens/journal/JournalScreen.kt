package com.samadhi.meditationtimer.ui.screens.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.samadhi.meditationtimer.ui.components.TempleScreenHeader
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onEditSession: (MeditationSession) -> Unit,
    onLogManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    val sessions by viewModel.sessions.collectAsState()
    val allSessions by viewModel.allJournalSessions.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val query by viewModel.query.collectAsState()
    val entryType by viewModel.entryType.collectAsState()
    val ratingFilter by viewModel.rating.collectAsState()
    val minMinutes by viewModel.minMinutes.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val restDays by viewModel.restDayDates.collectAsState()
    var sessionToDelete by remember { mutableStateOf<MeditationSession?>(null) }
    var selectedSection by remember { mutableStateOf("entries") }
    val markdownExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> uri?.let { viewModel.exportJournalMarkdown(it) } }
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportJournalJson(it) } }

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
                TempleScreenHeader(
                    title = "Journal",
                    subtitle = "Practice notes, rhythm, and reflection"
                )
                Spacer(modifier = Modifier.height(16.dp))

                JournalSectionTabs(selectedSection = selectedSection, onSectionSelected = { selectedSection = it })

                Spacer(modifier = Modifier.height(8.dp))
            }

            when (selectedSection) {
                "overview" -> {
                    item {
                        SummaryCard(analytics)
                        Spacer(modifier = Modifier.height(16.dp))
                        CalendarHeatmap(allSessions)
                        Spacer(modifier = Modifier.height(16.dp))
                        GentleGoalCard(
                            sessions = allSessions,
                            dailyGoalMinutes = dailyGoalMinutes,
                            restDays = restDays,
                            onGoalChange = viewModel::updateDailyGoalMinutes,
                            onUseRestDay = viewModel::useRestDayToday
                        )
                    }
                }
                "insights" -> item { InsightsCard(analytics, defaultExpanded = true) }
                else -> {
                    item {
                        JournalToolsCard(
                            query = query,
                            entryType = entryType,
                            ratingFilter = ratingFilter,
                            minMinutes = minMinutes,
                            filteredCount = sessions.size,
                            totalCount = allSessions.size,
                            statusMessage = statusMessage,
                            onQueryChange = viewModel::updateQuery,
                            onEntryTypeChange = viewModel::updateEntryType,
                            onRatingChange = viewModel::updateRating,
                            onMinMinutesChange = viewModel::updateMinMinutes,
                            onClearFilters = viewModel::clearFilters,
                            onDismissStatus = viewModel::clearStatusMessage,
                            onExportMarkdown = { markdownExportLauncher.launch("buddho-journal.md") },
                            onExportJson = { jsonExportLauncher.launch("buddho-journal.json") }
                        )
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
                                text = if (allSessions.isEmpty()) {
                                    "No meditation entries yet.\nFinish a sit or log one manually to begin."
                                } else {
                                    "No entries match these filters.\nTry clearing or widening the search."
                                },
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
private fun JournalSectionTabs(selectedSection: String, onSectionSelected: (String) -> Unit) {
    val currentTheme = LocalZenTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZenSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("entries" to "Entries", "overview" to "Overview", "insights" to "Insights").forEach { (id, label) ->
            val selected = selectedSection == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) currentTheme.primary.copy(alpha = 0.22f) else ZenSurface)
                    .clickable { onSectionSelected(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) currentTheme.primaryLight else ZenTextSecondary
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(analytics: JournalAnalytics) {
    val currentTheme = LocalZenTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Practice Summary", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ZenTextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Sessions", "${analytics.totalSessions}", Icons.Default.SelfImprovement, currentTheme.primaryLight)
                StatItem("Total Time", formatHoursMinutes(analytics.totalMinutes), Icons.Default.Timelapse, currentTheme.primary)
                StatItem("Streak", "${analytics.currentStreakDays}d", Icons.Default.LocalFireDepartment, currentTheme.primaryLight)
                StatItem("Best", "${analytics.longestStreakDays}d", Icons.Default.CalendarMonth, ZenTextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalToolsCard(
    query: String,
    entryType: String,
    ratingFilter: Int,
    minMinutes: Int,
    filteredCount: Int,
    totalCount: Int,
    statusMessage: String?,
    onQueryChange: (String) -> Unit,
    onEntryTypeChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onMinMinutesChange: (Int) -> Unit,
    onClearFilters: () -> Unit,
    onDismissStatus: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportJson: () -> Unit
) {
    val currentTheme = LocalZenTheme.current
    var filtersExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (filtersExpanded) 180f else 0f, label = "filterArrow")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search journal notes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filtersExpanded = !filtersExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = currentTheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Filters and export", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ZenTextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$filteredCount of $totalCount", fontSize = 11.sp, color = ZenTextMuted)
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = currentTheme.primary, modifier = Modifier.rotate(rotation))
                }
            }

            AnimatedVisibility(visible = filtersExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("all" to "All", "timer" to "Timer", "manual" to "Manual").forEach { (id, label) ->
                            FilterChip(
                                selected = entryType == id,
                                onClick = { onEntryTypeChange(id) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = currentTheme.primary)
                            )
                        }
                        listOf(0 to "Any mood", 5 to "5", 4 to "4", 3 to "3").forEach { (rating, label) ->
                            FilterChip(
                                selected = ratingFilter == rating,
                                onClick = { onRatingChange(rating) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = currentTheme.primary)
                            )
                        }
                        listOf(0 to "Any length", 10 to "10m+", 20 to "20m+", 30 to "30m+").forEach { (minutes, label) ->
                            FilterChip(
                                selected = minMinutes == minutes,
                                onClick = { onMinMinutesChange(minutes) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = currentTheme.primary)
                            )
                        }
                    }

                    OutlinedButton(onClick = onClearFilters, modifier = Modifier.fillMaxWidth()) { Text("Clear filters") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onExportMarkdown, modifier = Modifier.weight(1f)) { Text("Markdown") }
                        OutlinedButton(onClick = onExportJson, modifier = Modifier.weight(1f)) { Text("JSON") }
                    }
                }
            }

            statusMessage?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ZenSurfaceVariant.copy(alpha = 0.65f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(it, fontSize = 11.sp, color = ZenTextSecondary, modifier = Modifier.weight(1f))
                    Text(
                        text = "Dismiss",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = currentTheme.primary,
                        modifier = Modifier.clickable { onDismissStatus() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeatmap(sessions: List<MeditationSession>) {
    val currentTheme = LocalZenTheme.current
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val counts = sessions.groupingBy {
        Instant.ofEpochMilli(it.completedTimeEpochMs).atZone(zone).toLocalDate()
    }.eachCount()
    val start = today.minusDays(83)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Practice Rhythm", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ZenTextPrimary)
            Text("Last 12 weeks", fontSize = 10.5.sp, color = ZenTextMuted)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(12) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(7) { day ->
                            val date = start.plusDays((week * 7 + day).toLong())
                            val count = counts[date] ?: 0
                            val alpha = when (count) {
                                0 -> 0.12f
                                1 -> 0.35f
                                2 -> 0.55f
                                else -> 0.78f
                            }
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(currentTheme.primary.copy(alpha = alpha))
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GentleGoalCard(
    sessions: List<MeditationSession>,
    dailyGoalMinutes: Int,
    restDays: Set<String>,
    onGoalChange: (Int) -> Unit,
    onUseRestDay: () -> Unit
) {
    val currentTheme = LocalZenTheme.current
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val todayMinutes = sessions
        .filter { Instant.ofEpochMilli(it.completedTimeEpochMs).atZone(zone).toLocalDate() == today }
        .sumOf { it.actualDurationSec } / 60
    val isRestDay = restDays.contains(today.toString())
    val progressText = when {
        dailyGoalMinutes <= 0 -> "No daily goal set"
        todayMinutes >= dailyGoalMinutes -> "Today feels complete"
        isRestDay -> "Rest day noted"
        else -> "$todayMinutes of $dailyGoalMinutes minutes today"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gentle Intention", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ZenTextPrimary)
            Text(progressText, fontSize = 11.sp, color = ZenTextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Off", 10 to "10m", 15 to "15m", 20 to "20m", 30 to "30m").forEach { (minutes, label) ->
                    FilterChip(
                        selected = dailyGoalMinutes == minutes,
                        onClick = { onGoalChange(minutes) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = currentTheme.primary)
                    )
                }
            }
            if (todayMinutes == 0 && !isRestDay) {
                OutlinedButton(onClick = onUseRestDay, modifier = Modifier.fillMaxWidth()) {
                    Text("Mark today as a rest day")
                }
            }
        }
    }
}

@Composable
private fun InsightsCard(analytics: JournalAnalytics, defaultExpanded: Boolean = false) {
    val currentTheme = LocalZenTheme.current
    var expanded by remember { mutableStateOf(defaultExpanded) }
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
