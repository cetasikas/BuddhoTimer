package com.samadhi.meditationtimer.ui.screens.screentime

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samadhi.meditationtimer.data.ScreenTimePreferences
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
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@Composable
fun ScreenTimeScreen(
    onNavigateBack: () -> Unit,
    onOpenTimer: () -> Unit = {},
    viewModel: ScreenTimeViewModel = viewModel(
        viewModelStoreOwner = findViewModelStoreOwner(LocalContext.current)
    ),
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
                                text = "AWARENESS \u00B7 NOT GUILT",
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

            // First-run tracking question, before any permission ask.
            if (!uiState.trackingPromptAnswered) {
                item {
                    TrackingPromptCard(
                        onEnable = { viewModel.enableTracking() },
                        onDismiss = { viewModel.dismissTrackingPrompt() }
                    )
                }
            }

            // Permission Request Card (shown once the tracking prompt is answered)
            if (!uiState.hasPermission && uiState.trackingPromptAnswered) {
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
                                text = "To show your screen time alongside your meditation practice, BuddhoTimer needs Usage Access. All data stays 100% local on your phone \u2014 no accounts, no tracking, complete privacy.",
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
            } else if (uiState.hasPermission) {
                if (!uiState.isLoading) {
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
                    val goalMinutes = uiState.goalMinutes
                    val goalMs = goalMinutes * 60_000L

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

                            // Goal glance: remaining time against the chosen intention.
                            if (goalMinutes > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val remainingMs = goalMs - uiState.todayScreenTimeMs
                                Text(
                                    text = if (remainingMs > 0) {
                                        "${UsageStatsHelper.formatDuration(remainingMs)} left of your " +
                                            "${ScreenTimePreferences.presetLabel(goalMinutes)} intention"
                                    } else {
                                        "Your ${ScreenTimePreferences.presetLabel(goalMinutes)} intention is met"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = ZenTextMuted
                                )
                            }
                        }
                    }
                }

                // Optional daily goal picker, feeding every chart below.
                item {
                    DailyGoalSection(
                        goalMinutes = uiState.goalMinutes,
                        onSelect = { minutes -> viewModel.setGoalMinutes(minutes) }
                    )
                }

                // Mindful Practice vs Screen Time Comparison Card
                item {
                    val totalMeditationMins = uiState.todayPracticeMinutes
                    val screenTimeMins = uiState.todayScreenTimeMs / 60_000L

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

                                if (screenTimeMins > 0L) {
                                    val sumMins = (screenTimeMins + totalMeditationMins).coerceAtLeast(1L)
                                    val ratio = ((totalMeditationMins.toFloat() / sumMins.toFloat()) * 100).toInt()
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
                                            text = "Practice Today",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = ZenTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${uiState.todayPracticeCount} sessions",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = currentTheme.primary
                                        )
                                        Text(
                                            text = "${totalMeditationMins}m today",
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

                // Hour by hour, today
                item {
                    HourlyUsageSection(
                        hourly = uiState.hourlyToday,
                        goalMinutes = uiState.goalMinutes
                    )
                }

                // Past week, horizontal bars with goal markers
                item {
                    WeeklyBarSection(
                        days = uiState.weeklyDays,
                        goalMinutes = uiState.goalMinutes
                    )
                }

                // Unlocks: wakes that ended with the keyguard dismissed
                item {
                    UnlocksWeekSection(
                        days = uiState.weeklyDays,
                        unlocksAvailable = uiState.unlocksAvailable
                    )
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
                                val todayTotalMs = uiState.todayScreenTimeMs.coerceAtLeast(1L)

                                uiState.topApps.forEachIndexed { index, app ->
                                    val fraction = (app.durationMs.toFloat() / maxUsageMs).coerceIn(0.04f, 1f)
                                    val sharePercent =
                                        ((app.durationMs * 100.0) / todayTotalMs).toInt().coerceIn(0, 100)
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AppIcon(
                                                packageName = app.packageName,
                                                size = 30.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${index + 1}. ${app.appName}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 13.5.sp
                                                    ),
                                                    color = ZenTextPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (app.openCount > 0) {
                                                        "$sharePercent% of screen time \u00B7 ${app.openCount} opens"
                                                    } else {
                                                        "$sharePercent% of your screen time"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp
                                                    ),
                                                    color = ZenTextMuted
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
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

                // Calendar month, kept above the final action.
                item {
                    MonthlyCalendarSection(
                        monthDays = uiState.monthDays,
                        goalMinutes = uiState.goalMinutes
                    )
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
            } else {
                item {
                    LoadingStatsCard()
                }
            }
            }
        }
    }
}

@Composable
private fun LoadingStatsCard() {
    val currentTheme = LocalZenTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ZenSurfaceVariant.copy(alpha = 0.9f))
            .padding(vertical = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = currentTheme.primary,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Reading today's screen time\u2026",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = ZenTextMuted
            )
        }
    }
}

/** Walks up wrapped contexts to the nearest [ViewModelStoreOwner] (the hosting Activity). */
private tailrec fun findViewModelStoreOwner(context: Context): ViewModelStoreOwner =
    when (context) {
        is ViewModelStoreOwner -> context
        is ContextWrapper -> findViewModelStoreOwner(context.baseContext)
        else -> error("Context is not a ViewModelStoreOwner")
    }

/**
 * Renders an installed app's launcher icon, falling back to a neutral square
 * when the package cannot be resolved or fails to rasterise.
 */
@Composable
private fun AppIcon(
    packageName: String,
    size: Dp
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(ZenSurfaceVariant)
        )
    }
}
