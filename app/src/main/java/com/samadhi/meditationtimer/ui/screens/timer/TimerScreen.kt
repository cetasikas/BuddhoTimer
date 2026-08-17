package com.samadhi.meditationtimer.ui.screens.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.samadhi.meditationtimer.domain.model.BreathingPattern
import com.samadhi.meditationtimer.ui.components.ZenBranchSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onStartSession: () -> Unit,
    onOpenWisdom: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBreathing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val breathingPatternId by viewModel.selectedBreathingPatternId.collectAsState()
    val breathingDurationMin by viewModel.breathingDurationMinutes.collectAsState()
    val currentTheme = com.samadhi.meditationtimer.ui.theme.LocalZenTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var leafBurstTrigger by remember { mutableIntStateOf(0) }

    val selectedPattern = BreathingPattern.allPatterns.find { it.id == breathingPatternId } ?: BreathingPattern.BOX_BREATHING

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        FallingLeavesEffect(
            leafCount = 16,
            triggerBurst = leafBurstTrigger,
            ambientAlpha = 0.35f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. TOP HEADER (BRANDING & DONATE) ---
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
                            .background(currentTheme.primary.copy(alpha = 0.18f))
                            .border(1.dp, currentTheme.primary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = currentTheme.primaryLight,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BUDDHO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontSize = 14.sp
                            ),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = if (config.isBoxBreathingMode) "Living Breathwork" else "Mindful Stillness",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = currentTheme.primaryLight
                        )
                    }
                }

                // Support / Donate Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ZenSurface.copy(alpha = 0.65f))
                        .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://ko-fi.com/hackysackapp")
                            ).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
                            val strokeColor = currentTheme.primaryLight
                            val strokeW = 1.3f
                            val cup = Path().apply {
                                moveTo(size.width * 0.18f, size.height * 0.34f)
                                lineTo(size.width * 0.76f, size.height * 0.34f)
                                lineTo(size.width * 0.70f, size.height * 0.82f)
                                quadraticTo(size.width * 0.47f, size.height * 0.90f, size.width * 0.24f, size.height * 0.82f)
                                close()
                            }
                            drawPath(cup, strokeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
                            val handle = Path().apply {
                                moveTo(size.width * 0.74f, size.height * 0.42f)
                                quadraticTo(size.width * 0.98f, size.height * 0.54f, size.width * 0.70f, size.height * 0.68f)
                            }
                            drawPath(handle, strokeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
                            drawLine(strokeColor, Offset(size.width * 0.36f, size.height * 0.24f), Offset(size.width * 0.36f, size.height * 0.12f), strokeWidth = strokeW)
                            drawLine(strokeColor, Offset(size.width * 0.56f, size.height * 0.24f), Offset(size.width * 0.56f, size.height * 0.12f), strokeWidth = strokeW)
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Donate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            color = currentTheme.primaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 2. DYNAMIC SETUP CONTENT (MEDITATION VS BOX BREATHING) ---
            AnimatedContent(
                targetState = config.isBoxBreathingMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "PracticeModeSetup"
            ) { isBoxMode ->
                if (!isBoxMode) {
                    // === MEDITATION TIMER SETUP ===
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Duration Display
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${config.targetDurationMinutes}",
                                fontSize = 68.sp,
                                fontWeight = FontWeight.ExtraLight,
                                letterSpacing = (-2).sp,
                                color = ZenTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = if (config.targetDurationMinutes == 1) "MINUTE" else "MINUTES",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 3.5.sp,
                                color = currentTheme.primaryLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Organic Zen Branch Slider
                        ZenBranchSlider(
                            value = config.targetDurationMinutes.toFloat(),
                            onValueChange = { newMin ->
                                viewModel.updateDurationMinutes(newMin.toInt())
                            },
                            valueRange = 1f..120f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )

                        // 2-Row Presets
                        val row1Presets = listOf(5, 10, 15, 20)
                        val row2Presets = listOf(25, 30, 45, 60)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row1Presets.forEach { duration ->
                                    val isSelected = config.targetDurationMinutes == duration
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) currentTheme.primary.copy(alpha = 0.22f) else ZenSurface.copy(alpha = 0.55f))
                                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primaryLight else ZenBorder, RoundedCornerShape(14.dp))
                                            .clickable {
                                                viewModel.updateDurationMinutes(duration)
                                                leafBurstTrigger++
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${duration}m",
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) currentTheme.primaryLight else ZenTextSecondary
                                        )
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row2Presets.forEach { duration ->
                                    val isSelected = config.targetDurationMinutes == duration
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) currentTheme.primary.copy(alpha = 0.22f) else ZenSurface.copy(alpha = 0.55f))
                                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primaryLight else ZenBorder, RoundedCornerShape(14.dp))
                                            .clickable {
                                                viewModel.updateDurationMinutes(duration)
                                                leafBurstTrigger++
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${duration}m",
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) currentTheme.primaryLight else ZenTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Continue Past Target Time Toggle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ZenSurface.copy(alpha = 0.60f))
                                .border(1.dp, ZenBorder.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Continue Past Target Time",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = ZenTextPrimary
                                    )
                                    Text(
                                        text = "Timer keeps counting up after reaching target time",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = ZenTextSecondary
                                    )
                                }
                                Switch(
                                    checked = config.isOverrunEnabled,
                                    onCheckedChange = {
                                        viewModel.toggleOverrun(it)
                                        leafBurstTrigger++
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = currentTheme.primary,
                                        checkedTrackColor = currentTheme.primary.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // === BOX BREATHING SETUP ===
                    val cycleDurationSec = selectedPattern.totalCycleSec
                    val estimatedCycles = (breathingDurationMin * 60) / cycleDurationSec.coerceAtLeast(1)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Duration & Cycles Display
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$breathingDurationMin",
                                fontSize = 68.sp,
                                fontWeight = FontWeight.ExtraLight,
                                letterSpacing = (-2).sp,
                                color = ZenTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "$estimatedCycles BREATH CYCLES",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 3.sp,
                                color = currentTheme.primaryLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Duration Slider (1 to 30 min)
                        ZenBranchSlider(
                            value = breathingDurationMin.toFloat(),
                            onValueChange = { newMin ->
                                viewModel.updateBreathingDurationMinutes(newMin.toInt())
                            },
                            valueRange = 1f..30f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )

                        // Quick Breathwork Duration Presets
                        val breathPresets = listOf(2, 5, 10, 15, 20)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            breathPresets.forEach { dur ->
                                val isSelected = breathingDurationMin == dur
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) currentTheme.primary.copy(alpha = 0.22f) else ZenSurface.copy(alpha = 0.55f))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primaryLight else ZenBorder, RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.updateBreathingDurationMinutes(dur)
                                            leafBurstTrigger++
                                        }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dur}m",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) currentTheme.primaryLight else ZenTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Breathing Pattern Cards
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BreathingPattern.allPatterns.forEach { pattern ->
                                val isSelected = selectedPattern.id == pattern.id
                                val cadenceText = when (pattern.id) {
                                    "box" -> "4s In • 4s Hold • 4s Out • 4s Hold"
                                    "478" -> "4s In • 7s Hold • 8s Out"
                                    "resonant" -> "5s Inhale • 5s Exhale"
                                    else -> "3s In • 3s Hold • 3s Out • 3s Hold"
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.updateBreathingPattern(pattern.id)
                                            leafBurstTrigger++
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) currentTheme.primary.copy(alpha = 0.18f) else ZenSurface.copy(alpha = 0.60f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) currentTheme.primaryLight else ZenBorder),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pattern.name,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) currentTheme.primaryLight else ZenTextPrimary
                                            )
                                            Text(
                                                text = cadenceText,
                                                fontSize = 11.sp,
                                                color = if (isSelected) currentTheme.primaryLight.copy(alpha = 0.85f) else ZenTextSecondary
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.SelfImprovement,
                                                contentDescription = null,
                                                tint = currentTheme.primaryLight,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 3. BOTTOM CONTROLS (MODE TOGGLE NEAR BOTTOM + PRIMARY ACTION BUTTON) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Practice Mode Toggle Near Bottom
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(ZenSurface.copy(alpha = 0.85f))
                        .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Meditation Option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (!config.isBoxBreathingMode) currentTheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (!config.isBoxBreathingMode) currentTheme.primaryLight.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(18.dp))
                            .clickable {
                                if (config.isBoxBreathingMode) {
                                    viewModel.toggleBoxBreathingMode(false)
                                    leafBurstTrigger++
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = if (!config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Meditation",
                                fontSize = 12.sp,
                                fontWeight = if (!config.isBoxBreathingMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextSecondary
                            )
                        }
                    }

                    // Box Breathing Option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (config.isBoxBreathingMode) currentTheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (config.isBoxBreathingMode) currentTheme.primaryLight.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(18.dp))
                            .clickable {
                                if (!config.isBoxBreathingMode) {
                                    viewModel.toggleBoxBreathingMode(true)
                                    leafBurstTrigger++
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Air,
                                contentDescription = null,
                                tint = if (config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Box Breathing",
                                fontSize = 12.sp,
                                fontWeight = if (config.isBoxBreathingMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextSecondary
                            )
                        }
                    }
                }

                // Primary Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = currentTheme.primary)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    currentTheme.primaryDark,
                                    currentTheme.primary,
                                    currentTheme.primaryLight
                                )
                            )
                        )
                        .border(1.dp, currentTheme.primaryLight.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .clickable {
                            leafBurstTrigger++
                            if (config.isBoxBreathingMode) {
                                onOpenBreathing()
                            } else {
                                viewModel.startSession()
                                onStartSession()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (config.isBoxBreathingMode) Icons.Outlined.Air else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF041014),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (config.isBoxBreathingMode) "Begin Box Breathing" else "Begin Practice",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF041014)
                        )
                    }
                }
            }
        }
    }
}
