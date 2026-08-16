package com.samadhi.meditationtimer.ui.screens.history

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.ui.components.MarkdownRenderer
import com.samadhi.meditationtimer.ui.components.moodOptions
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenDanger
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenOverrunBlue
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val stats by viewModel.stats.collectAsState()
    var sessionToDelete by remember { mutableStateOf<MeditationSession?>(null) }

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        // Moonlit Backdrop
        Image(
            painter = painterResource(id = R.drawable.bg_zen_bamboo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.20f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060911).copy(alpha = 0.85f),
                            Color(0xFF0A101D).copy(alpha = 0.70f),
                            Color(0xFF060911).copy(alpha = 0.92f)
                        )
                    )
                )
        )

        FallingLeavesEffect(
            leafCount = 14,
            ambientAlpha = 0.26f
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Practice Journey",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = ZenTextPrimary
            )
            Text(
                text = "Reflect on your consistency and insights",
                style = MaterialTheme.typography.bodyMedium,
                color = ZenTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Statistics Grid Card
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
                        // Total Sessions
                        StatItem(
                            title = "Sessions",
                            value = "${stats.totalSessionsCount}",
                            icon = Icons.Default.SelfImprovement,
                            tint = ZenAmber
                        )
                        // Total Practice Time
                        StatItem(
                            title = "Total Time",
                            value = formatHoursMinutes(stats.totalPracticeMinutes),
                            icon = Icons.Default.Timelapse,
                            tint = ZenTeal
                        )
                        // Current Streak
                        StatItem(
                            title = "Streak",
                            value = "${stats.currentStreakDays}d",
                            icon = Icons.Default.LocalFireDepartment,
                            tint = ZenAmber
                        )
                        // Longest Streak
                        StatItem(
                            title = "Best",
                            value = "${stats.longestStreakDays}d",
                            icon = Icons.Default.CalendarMonth,
                            tint = ZenTextSecondary
                        )
                    }
                }
            }

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
                            text = "No meditation records yet.\nBegin your first practice today.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = ZenTextMuted),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    onDeleteClick = { sessionToDelete = session }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        }

        // Delete Confirmation Dialog
        sessionToDelete?.let { session ->
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text(text = "Delete Practice Record", color = ZenTextPrimary) },
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
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ZenTextPrimary
        )
        Text(
            text = title,
            fontSize = 11.sp,
            color = ZenTextSecondary
        )
    }
}

@Composable
private fun SessionCard(
    session: MeditationSession,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • h:mm a")
    val dateString = Instant.ofEpochMilli(session.completedTimeEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)

    val moodOption = moodOptions.find { it.level == session.rating }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = dateString,
                        fontSize = 12.sp,
                        color = ZenTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = NotificationHelper.formatSeconds(session.actualDurationSec),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZenAmber
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
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (moodOption != null) {
                        Icon(
                            imageVector = moodOption.icon,
                            contentDescription = moodOption.title,
                            tint = ZenAmber,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ZenSurfaceVariant)
                                .padding(4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Session",
                            tint = ZenTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Rendered Markdown Notes (if present)
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
