package com.samadhi.meditationtimer.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.service.UsageStatsHelper
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenSage
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

/**
 * Screen Time vs. Meditation Practice Insights Card for the History Screen.
 * Inspired by TimeAware's balance philosophy.
 */
@Composable
fun ScreenTimeInsightsCard(
    weeklyMeditationMinutes: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme = LocalZenTheme.current

    var hasPermission by remember { mutableStateOf(false) }
    var todayScreenTimeMs by remember { mutableStateOf<Long?>(null) }
    var weeklyScreenTimeTotalHours by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        hasPermission = UsageStatsHelper.hasUsageAccessPermission(context)
        if (hasPermission) {
            todayScreenTimeMs = UsageStatsHelper.getTodayScreenTimeMs(context)
            val weeklyDays = UsageStatsHelper.getWeeklyScreenTime(context)
            val totalMs = weeklyDays.sumOf { it.durationMs }
            weeklyScreenTimeTotalHours = totalMs / 3_600_000f
        }
    }

    if (!hasPermission) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.85f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(currentTheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = currentTheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Mindfulness vs. Screen Time",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp
                            ),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = "Past 7 days awareness balance",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = ZenTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Meditation column
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
                            text = "Meditation",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = ZenTextSecondary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${weeklyMeditationMinutes}m",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = currentTheme.primary
                        )
                        Text(
                            text = "stillness practice",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = ZenTextMuted
                        )
                    }
                }

                // Screen time column
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
                            text = "Screen Time",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = ZenTextSecondary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val formattedWeekly = String.format(java.util.Locale.US, "%.1fh", weeklyScreenTimeTotalHours)
                        Text(
                            text = formattedWeekly,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = "past 7 days",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = ZenTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "🌿 Awareness is the first step toward reclaiming your quiet attention.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = currentTheme.primaryLight
                )
            )
        }
    }
}
