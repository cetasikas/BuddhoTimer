package com.samadhi.meditationtimer.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTealLight
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

private data class TutorialStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val isPermissionStep: Boolean = false
)

private val tutorialSteps = listOf(
    TutorialStep(
        title = "Welcome to Buddho Timer",
        subtitle = "A serene, 100% offline forest meditation companion",
        description = "Designed for quiet stillness and peace. Set your target practice duration easily with the minimal slider or one-tap glowing preset buttons.",
        icon = Icons.Default.SelfImprovement
    ),
    TutorialStep(
        title = "Keep It Going (Samadhi Overrun)",
        subtitle = "Never be interrupted during deep peace",
        description = "Enable Samadhi Overrun to let your meditation continue uninterrupted past target time without startling bells. The timer counts up gently until you finish.",
        icon = Icons.Default.HourglassBottom
    ),
    TutorialStep(
        title = "Box Breathing & Wisdom Guides",
        subtitle = "Mindfulness anchors at your fingertips",
        description = "Tap the 'Zen Box Breathing' pill on the main screen for 4-4-4-4 particle breathwork, or open the Wisdom tab for Ajahn Brahm's step-by-step guides.",
        icon = Icons.Default.Air
    ),
    TutorialStep(
        title = "Recommended Permissions",
        subtitle = "Ensure uninterrupted sits & background stability",
        description = "Grant Do Not Disturb access to auto-silence incoming notifications, and exempt background battery optimization so Android never terminates long sits.",
        icon = Icons.Default.DoNotDisturb,
        isPermissionStep = true
    )
)

@Composable
fun TutorialOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

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
                .border(1.dp, ZenTeal.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(24.dp),
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
                                .width(if (idx == currentStepIndex) 24.dp else 10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (idx == currentStepIndex) ZenTealLight
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

            Spacer(modifier = Modifier.height(20.dp))

            // Glowing Icon Header
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ZenTeal.copy(alpha = 0.35f),
                                ZenSurfaceVariant
                            )
                        )
                    )
                    .border(1.dp, ZenTealLight.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = ZenTealLight,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = step.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ),
                color = ZenTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = step.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp
                ),
                color = ZenTealLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                ),
                color = ZenTextSecondary,
                textAlign = TextAlign.Center
            )

            // Permissions Quick Access (Only on Step 4)
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTealLight),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(ZenTeal))
                    ) {
                        Text("Grant DND", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTealLight),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(ZenTeal))
                    ) {
                        Text("Disable Battery Opt", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZenTeal,
                        contentColor = Color(0xFF041014)
                    )
                ) {
                    Text(
                        text = if (isLastStep) "Begin Practice" else "Next",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
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
