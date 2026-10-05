package com.samadhi.meditationtimer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Spa
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.MindfulPrompts
import com.samadhi.meditationtimer.service.UsageStatsHelper
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenDanger
import com.samadhi.meditationtimer.ui.theme.ZenSage
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

/**
 * Ambient, TimeAware-inspired banner shown on the Timer screen.
 * Displays today's screen time and a gentle rotating mindful question.
 * Tapping opens the dedicated ScreenTimeScreen.
 */
@Composable
fun ScreenTimeBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme = LocalZenTheme.current

    var hasPermission by remember { mutableStateOf(false) }
    var todayScreenTimeMs by remember { mutableStateOf<Long?>(null) }
    var dailyPrompt by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        hasPermission = UsageStatsHelper.hasUsageAccessPermission(context)
        if (hasPermission) {
            todayScreenTimeMs = UsageStatsHelper.getTodayScreenTimeMs(context)
        }
        dailyPrompt = MindfulPrompts.getDailyPrompt()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ZenSurface.copy(alpha = 0.78f))
            .border(1.dp, ZenBorder.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator Dot or Icon
                if (hasPermission && todayScreenTimeMs != null) {
                    val severity = UsageStatsHelper.getUsageSeverity(todayScreenTimeMs!!)
                    val dotColor = when (severity) {
                        0 -> ZenSage
                        1 -> ZenAmber
                        else -> ZenDanger
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = UsageStatsHelper.formatDuration(todayScreenTimeMs!!),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = ZenTextPrimary
                            )
                            Text(
                                text = " · ${UsageStatsHelper.getZenUsageLabel(todayScreenTimeMs!!)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = dotColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = currentTheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (dailyPrompt.isNotEmpty()) dailyPrompt else "Take one breath before you scroll",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontStyle = FontStyle.Italic
                        ),
                        color = ZenTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Screen Time",
                tint = ZenTextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
