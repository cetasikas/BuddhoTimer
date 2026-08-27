package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted

data class MoodOption(
    val level: Int,
    val title: String,
    val icon: ImageVector
)

val moodOptions = listOf(
    MoodOption(1, "Restless", Icons.Default.Grain),
    MoodOption(2, "Wandering", Icons.Default.CloudQueue),
    MoodOption(3, "Settled", Icons.Default.SelfImprovement),
    MoodOption(4, "Deep", Icons.Default.Spa),
    MoodOption(5, "Radiant", Icons.Default.Brightness5)
)

@Composable
fun MoodSelector(
    selectedMood: Int,
    onMoodSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        moodOptions.forEach { mood ->
            val isSelected = mood.level == selectedMood
            val bg = if (isSelected) currentTheme.primary.copy(alpha = 0.22f) else currentTheme.surfaceVariant
            val border = if (isSelected) currentTheme.primary else currentTheme.border
            val tint = if (isSelected) currentTheme.primary else currentTheme.textSecondary
            val textColor = if (isSelected) currentTheme.primary else currentTheme.textSecondary

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(if (isSelected) 1.5.dp else 1.dp, border, RoundedCornerShape(12.dp))
                    .clickable { onMoodSelected(mood.level) }
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = mood.icon,
                    contentDescription = mood.title,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mood.title,
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    softWrap = false,
                    color = textColor
                )
            }
        }
    }
}
