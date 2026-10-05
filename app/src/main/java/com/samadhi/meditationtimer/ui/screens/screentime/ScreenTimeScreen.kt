package com.samadhi.meditationtimer.ui.screens.screentime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samadhi.meditationtimer.service.AppUsage
import com.samadhi.meditationtimer.service.DayUsage
import com.samadhi.meditationtimer.service.UsageStatsHelper
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenDanger
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSage
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@Composable
fun ScreenTimeScreen(
    onNavigateBack: () -> Unit,
    onOpenTimer: () -> Unit = {},
    viewModel: ScreenTimeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTheme = LocalZenTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh stats when user returns to screen (e.g. from granting permission)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FallingLeavesEffect(
            leafCount = 12,
            ambientAlpha = 0.25f
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ZenSurface.copy(alpha = 0.8f))
                                .border(1.dp, ZenBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ZenTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Mindful Screen Time",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = ZenTextPrimary
                            )
                            Text(
                                text = "AWARENESS • NOT GUILT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.6.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = currentTheme.primaryLight
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ZenSurface.copy(alpha = 0.6f))
                            .border(1.dp, ZenBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Rotating Mindful Pause Prompt Card (Inspired by TimeAware)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.85f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(currentTheme.primary.copy(alpha = 0.35f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = currentTheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "BEFORE YOU SCROLL",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.4.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = currentTheme.primaryLight
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "\"${uiState.todayMindfulPrompt}\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 15.5.sp,
                                lineHeight = 22.sp
                            ),
                            color = ZenTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A gentle inquiry to bring intention before automatic habits take over.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = ZenTextSecondary
                        )
                    }
                }
            }

            // Permission Request Card (shown if permission is not yet granted)
            if (!uiState.hasPermission) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = ZenSurfaceVariant.copy(alpha = 0.85f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = SolidColor(ZenAmber.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(ZenAmber.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = ZenAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = "Enable Usage Access",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ZenTextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "To show your screen time alongside your meditation practice, BuddhoTimer needs Usage Access. All data stays 100% local on your phone — no accounts, no tracking, complete privacy.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                                color = ZenTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { viewModel.openUsageAccessSettings() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentTheme.primary,
                                    contentColor = ZenObsidian
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Grant Usage Access in Settings",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Today's Screen Time Summary (TimeAware Core Visual)
                item {
                    val severity = UsageStatsHelper.getUsageSeverity(uiState.todayScreenTimeMs)
                    val zenLabel = UsageStatsHelper.getZenUsageLabel(uiState.todayScreenTimeMs)
                    val statusColor = when (severity) {
                        0 -> ZenSage
                        1 -> ZenAmber
                        2 -> ZenDanger.copy(alpha = 0.9f)
                        else -> ZenDanger
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.9f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = SolidColor(statusColor.copy(alpha = 0.45f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TODAY ON SCREEN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ZenTextMuted
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = UsageStatsHelper.formatDuration(uiState.todayScreenTimeMs),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp,
                                    fontSize = 44.sp
                                ),
                                color = ZenTextPrimary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Zen status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(statusColor.copy(alpha = 0.16f))
                                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = zenLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = statusColor
                                )
                            }
                        }
                    }
                }

                // Mindful Practice vs Screen Time Comparison Card
                item {
                    val totalMeditationSec = uiState.sessionStats.totalDurationSec
                    val totalMeditationMins = totalMeditationSec / 60
                    val screenTimeMins = uiState.todayScreenTimeMs / 60_000

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.85f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        tint = currentTheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Mindfulness Ratio",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = ZenTextPrimary
                                    )
                                }

                                if (screenTimeMins > 0 && totalMeditationMins > 0) {
                                    val ratio = ((totalMeditationMins.toFloat() / (screenTimeMins + totalMeditationMins).coerceAtLeast(1L)) * 100).toInt()
                                    Text(
                                        text = "$ratio% Mindful",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = currentTheme.primaryLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Meditation block
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ZenSurfaceVariant)
                                        .border(1.dp, ZenBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Practice Sits",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = ZenTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${uiState.sessionStats.totalSessionsCount} sessions",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = currentTheme.primary
                                        )
                                        Text(
                                            text = "${totalMeditationMins}m total",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = ZenTextMuted
                                        )
                                    }
                                }

                                // Screen time block
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ZenSurfaceVariant)
                                        .border(1.dp, ZenBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Phone Time Today",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = ZenTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = UsageStatsHelper.formatDuration(uiState.todayScreenTimeMs),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ZenTextPrimary
                                        )
                                        Text(
                                            text = "across active apps",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = ZenTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Top Apps Breakdown (TimeAware "Quiet Receipt" Aesthetic)
                if (uiState.topApps.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.85f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Top Apps Today",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = ZenTextPrimary
                                    )
                                    Text(
                                        text = "RECEIPT VIEW",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.2.sp,
                                            fontSize = 10.sp
                                        ),
                                        color = ZenTextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val maxUsageMs = uiState.topApps.maxOfOrNull { it.durationMs }?.coerceAtLeast(1L) ?: 1L

                                uiState.topApps.forEachIndexed { index, app ->
                                    val fraction = (app.durationMs.toFloat() / maxUsageMs).coerceIn(0.04f, 1f)
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${app.appName}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.5.sp
                                                ),
                                                color = ZenTextPrimary
                                            )
                                            Text(
                                                text = UsageStatsHelper.formatDuration(app.durationMs),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = currentTheme.primaryLight
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(5.dp))

                                        // Minimal receipt bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(ZenSurfaceVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        if (index == 0) currentTheme.primary
                                                        else currentTheme.primary.copy(alpha = 0.55f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7-Day Weekly Rhythm (Past 7 Days)
                if (uiState.weeklyDays.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.85f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "7-Day Rhythm",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    ),
                                    color = ZenTextPrimary
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                val maxDayMs = uiState.weeklyDays.maxOfOrNull { it.durationMs }?.coerceAtLeast(3_600_000L) ?: 3_600_000L

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    uiState.weeklyDays.forEach { day ->
                                        val barHeightFraction = (day.durationMs.toFloat() / maxDayMs).coerceIn(0.06f, 1f)
                                        val daySeverity = UsageStatsHelper.getUsageSeverity(day.durationMs)
                                        val barColor = when (daySeverity) {
                                            0 -> ZenSage
                                            1 -> ZenAmber
                                            else -> ZenDanger
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.55f)
                                                    .height((70 * barHeightFraction).dp)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(barColor.copy(alpha = 0.85f))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = day.dayName.take(3),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (day.dayName == "Today") FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (day.dayName == "Today") currentTheme.primaryLight else ZenTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Practice Action
                item {
                    Button(
                        onClick = onOpenTimer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentTheme.primary,
                            contentColor = ZenObsidian
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Take a Mindful Breathwork Break",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
