package com.samadhi.meditationtimer.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.ui.theme.BambooTealTheme
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.SacredLotusTheme
import com.samadhi.meditationtimer.ui.theme.SakuraTwilightTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import com.samadhi.meditationtimer.ui.theme.ZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenThemeId

import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.AutoAwesome

private data class FeatureHighlight(
    val icon: ImageVector,
    val title: String,
    val desc: String
)

private data class TutorialStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val features: List<FeatureHighlight> = emptyList(),
    val isPermissionStep: Boolean = false
)

private val tutorialSteps = listOf(
    TutorialStep(
        title = "Welcome to Buddho Timer",
        subtitle = "100% offline · Zero ads · Zero tracking",
        description = "A distraction-free meditation timer rooted in Buddhist tradition. No accounts, no cloud — just you and your practice.",
        icon = Icons.Default.Spa,
        features = listOf(
            FeatureHighlight(Icons.Default.SelfImprovement, "Flexible Timer", "Set any duration · continuous timing · prep countdown"),
            FeatureHighlight(Icons.Default.Air, "Box Breathing & Buddho", "Visual breath pacing with the classic forest-tradition anchor"),
            FeatureHighlight(Icons.Default.AutoAwesome, "Buddhist Wisdom Library", "Teachings from Ajahn Brahm, Thich Nhat Hanh & more")
        )
    ),
    TutorialStep(
        title = "Built for Deep Practice",
        subtitle = "Tools that stay out of your way",
        description = "Every feature serves your sit — nothing more.",
        icon = Icons.Default.Tune,
        features = listOf(
            FeatureHighlight(Icons.Default.DarkMode, "Tap-to-Blackout", "Tap the screen during a sit to go pure AMOLED black"),
            FeatureHighlight(Icons.Default.MusicNote, "Interval Bell", "Periodic Tibetan bowl chime to anchor wandering attention"),
            FeatureHighlight(Icons.Default.NotificationsActive, "Daily Reminders", "Optional gentle nudge at a fixed time, random hour, or interval")
        )
    ),
    TutorialStep(
        title = "Two Quick Permissions",
        subtitle = "Recommended for uninterrupted long sits",
        description = "Neither is required, but both help:\n\n• Do Not Disturb — silences calls & notifications during your sit\n• Battery Optimization — prevents Android from stopping your timer after 15–20 minutes",
        icon = Icons.Default.DoNotDisturb,
        isPermissionStep = true
    )
)

@Composable
fun TutorialOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSelectTheme: (ZenThemeId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val currentTheme = LocalZenTheme.current
    val context = LocalContext.current
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = tutorialSteps[currentStepIndex]
    val isLastStep = currentStepIndex == tutorialSteps.size - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04070E).copy(alpha = 0.88f))
            .clickable(enabled = false) {}, // Scrim
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(ZenSurface.copy(alpha = 0.95f))
                .border(1.dp, currentTheme.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Progress & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Step Indicator Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tutorialSteps.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (idx == currentStepIndex) 28.dp else 10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (idx == currentStepIndex) currentTheme.primaryLight
                                    else ZenBorder.copy(alpha = 0.7f)
                                )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tutorial",
                        tint = ZenTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step Hero Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                currentTheme.primary.copy(alpha = 0.35f),
                                ZenSurfaceVariant
                            )
                        )
                    )
                    .border(1.dp, currentTheme.primaryLight.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (currentStepIndex == 0) {
                    BuddhaOutlineLogo(
                        size = 36.dp,
                        tint = currentTheme.primaryLight,
                        strokeWidth = 2.0f,
                        showEnsoCircle = false
                    )
                } else {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = currentTheme.primaryLight,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = step.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = ZenTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = step.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp
                ),
                color = currentTheme.primaryLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp
                ),
                color = ZenTextSecondary,
                textAlign = TextAlign.Center
            )

            // Step Feature Cards (For steps with features list)
            if (step.features.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    step.features.forEach { (icon, title, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ZenSurfaceVariant.copy(alpha = 0.6f))
                                .border(1.dp, ZenBorder.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = currentTheme.primaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZenTextPrimary
                                )
                                Text(
                                    text = desc,
                                    fontSize = 10.5.sp,
                                    color = ZenTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Permissions Quick Access (On Permission Step)
            if (step.isPermissionStep) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                    ) {
                        Text(
                            text = "Grant DND",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                    ) {
                        Text(
                            text = "Battery Settings",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Skip", color = ZenTextMuted, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (isLastStep) {
                            onDismiss()
                        } else {
                            currentStepIndex++
                        }
                    },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentTheme.primary,
                        contentColor = Color(0xFF041014)
                    )
                ) {
                    Text(
                        text = if (isLastStep) "Begin Practice" else "Next",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
