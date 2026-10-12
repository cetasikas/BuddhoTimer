package com.samadhi.meditationtimer.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.TimeBucket
import com.samadhi.meditationtimer.domain.model.WeekBucket
import com.samadhi.meditationtimer.ui.components.moodOptions
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

private val BarAreaHeight: Dp = 96.dp

@Composable
fun WeeklyTrendChart(weeks: List<WeekBucket>, modifier: Modifier = Modifier) {
    val currentTheme = LocalZenTheme.current
    val maxMinutes = weeks.maxOfOrNull { it.minutes }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            weeks.forEach { week ->
                val fraction = (week.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.height(BarAreaHeight),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val barHeight = (BarAreaHeight * fraction).coerceAtLeast(2.dp)
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(
                                    if (week.minutes > 0L) currentTheme.primary
                                    else ZenSurfaceVariant
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = week.label,
                        fontSize = 8.5.sp,
                        maxLines = 1,
                        color = ZenTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun MoodDistributionChart(counts: Map<Int, Int>, modifier: Modifier = Modifier) {
    val currentTheme = LocalZenTheme.current
    val maxCount = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        moodOptions.forEach { mood ->
            val count = counts[mood.level] ?: 0
            val fraction = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = mood.icon,
                    contentDescription = mood.title,
                    tint = currentTheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mood.title,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = ZenTextSecondary,
                    modifier = Modifier.width(72.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(ZenSurfaceVariant)
                ) {
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(currentTheme.primary)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$count",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZenTextPrimary,
                    modifier = Modifier.width(22.dp)
                )
            }
        }
    }
}

@Composable
fun TimeOfDayChart(buckets: List<TimeBucket>, modifier: Modifier = Modifier) {
    val currentTheme = LocalZenTheme.current
    val maxMinutes = buckets.maxOfOrNull { it.minutes }?.coerceAtLeast(1L) ?: 1L

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        buckets.forEach { bucket ->
            val fraction = (bucket.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.height(BarAreaHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val barHeight = (BarAreaHeight * fraction).coerceAtLeast(2.dp)
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (bucket.minutes > 0L) currentTheme.primaryLight else ZenSurfaceVariant)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bucket.label,
                    fontSize = 8.5.sp,
                    maxLines = 1,
                    color = ZenTextMuted
                )
            }
        }
    }
}

@Composable
fun ActivityStrip(days: List<Boolean>, modifier: Modifier = Modifier) {
    val currentTheme = LocalZenTheme.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        days.forEach { practiced ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (practiced) currentTheme.primary else ZenSurfaceVariant)
            )
        }
    }
}

@Composable
fun ChartSectionTitle(text: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = text.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = LocalZenTheme.current.primary
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = subtitle, fontSize = 10.sp, color = ZenTextMuted)
        }
    }
}
