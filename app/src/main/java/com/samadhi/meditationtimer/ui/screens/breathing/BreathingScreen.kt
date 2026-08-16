package com.samadhi.meditationtimer.ui.screens.breathing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.domain.model.BreathingPattern
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.service.SoundHelper
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.components.ZenParticleBreathingCanvas
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenGoldGlow
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenOverrunBlue
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreathingScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundHelper = remember { SoundHelper(context) }

    var selectedPattern by remember { mutableStateOf(BreathingPattern.BOX_BREATHING) }
    var isRunning by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var phaseSecondsRemaining by remember { mutableIntStateOf(selectedPattern.inhaleSec) }
    var totalElapsedSec by remember { mutableIntStateOf(0) }
    var completedCycles by remember { mutableIntStateOf(0) }
    var leafBurstTrigger by remember { mutableIntStateOf(0) }

    // Smooth sub-second progress for fluid particle transition
    var phaseProgress by remember { mutableFloatStateOf(0.0f) }

    // High precision breath cycle ticker
    LaunchedEffect(isRunning, selectedPattern) {
        if (!isRunning) return@LaunchedEffect

        currentPhase = BreathPhase.INHALE
        phaseSecondsRemaining = selectedPattern.inhaleSec
        phaseProgress = 0.0f
        soundHelper.triggerGentleVibration()

        var phaseStartTimeMs = System.currentTimeMillis()

        while (isActive && isRunning) {
            val totalPhaseSec = getPhaseDuration(currentPhase, selectedPattern).coerceAtLeast(1)
            val totalPhaseMs = totalPhaseSec * 1000L
            val elapsedInPhaseMs = System.currentTimeMillis() - phaseStartTimeMs

            phaseProgress = (elapsedInPhaseMs.toFloat() / totalPhaseMs.toFloat()).coerceIn(0.0f, 1.0f)
            val secRemaining = (totalPhaseSec - (elapsedInPhaseMs / 1000L).toInt()).coerceAtLeast(1)
            phaseSecondsRemaining = secRemaining

            if (elapsedInPhaseMs >= totalPhaseMs) {
                val nextPhase = getNextPhase(currentPhase, selectedPattern)
                if (nextPhase == BreathPhase.INHALE) {
                    completedCycles++
                }
                currentPhase = nextPhase
                phaseStartTimeMs = System.currentTimeMillis()
                phaseSecondsRemaining = getPhaseDuration(nextPhase, selectedPattern)
                phaseProgress = 0.0f
                soundHelper.triggerGentleVibration()
            }

            delay(16L) // ~60 FPS smooth interpolation
        }
    }

    // Elapsed total time ticker
    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isActive && isRunning) {
            delay(1000L)
            totalElapsedSec++
        }
    }

    val currentTheme = com.samadhi.meditationtimer.ui.theme.LocalZenTheme.current
    val phaseColor = when (currentPhase) {
        BreathPhase.INHALE -> currentTheme.primary
        BreathPhase.HOLD_FULL -> currentTheme.primaryLight
        BreathPhase.EXHALE -> currentTheme.overrunColor
        BreathPhase.HOLD_EMPTY -> ZenAmber
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ZenObsidian)
    ) {
        // 1. Dynamic Moonlit Backdrop
        Image(
            painter = painterResource(id = currentTheme.backgroundDrawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.18f
        )

        // Moonlit Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060911).copy(alpha = 0.85f),
                            Color(0xFF0A101D).copy(alpha = 0.70f),
                            Color(0xFF060911).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // 2. Subtle Falling Leaves Effect
        FallingLeavesEffect(
            leafCount = 14,
            triggerBurst = leafBurstTrigger,
            ambientAlpha = 0.25f
        )

        // 3. Main Screen Flow
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Row (With proper spacing so Close button never overlaps title)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(currentTheme.primary.copy(alpha = 0.18f))
                            .border(1.dp, currentTheme.primary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = currentTheme.primaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ZEN BREATHING",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = selectedPattern.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ZenTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ZenSurface)
                        .border(1.dp, ZenBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Breathing",
                        tint = ZenTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pattern Selection Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BreathingPattern.allPatterns.forEach { pattern ->
                    val isSelected = selectedPattern.id == pattern.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedPattern = pattern
                            currentPhase = BreathPhase.INHALE
                            phaseSecondsRemaining = pattern.inhaleSec
                            phaseProgress = 0f
                            leafBurstTrigger++
                        },
                        label = {
                            Text(
                                text = pattern.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ZenSurface.copy(alpha = 0.7f),
                            labelColor = ZenTextSecondary,
                            selectedContainerColor = currentTheme.primary,
                            selectedLabelColor = ZenObsidian
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) currentTheme.primaryLight else ZenBorder,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Central Zen Particle Breath Visualizer (Miniature Leaves / Petals Breathing Field)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ZenParticleBreathingCanvas(
                    currentPhase = currentPhase,
                    phaseProgress = phaseProgress,
                    modifier = Modifier.fillMaxSize(),
                    particleCount = 220
                )

                // High-Contrast Sanctuary Breath Phase Typography Floating in Particle Field
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(ZenObsidian.copy(alpha = 0.85f))
                        .border(1.5.dp, phaseColor.copy(alpha = 0.70f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 28.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // High-Contrast Floating Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(phaseColor.copy(alpha = 0.22f))
                                .border(1.dp, phaseColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = currentPhase.displayName.uppercase(),
                                fontSize = 13.sp,
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = phaseColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$phaseSecondsRemaining",
                            fontSize = 62.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = ZenTextPrimary
                        )

                        Text(
                            text = currentPhase.instruction,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ZenTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Stats Card (Cycles & Calm Time)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.85f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ZenBorder, ZenBorder))),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COMPLETED CYCLES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = ZenTextSecondary
                        )
                        Text(
                            text = "$completedCycles",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryLight
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CALM TIME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = ZenTextSecondary
                        )
                        Text(
                            text = NotificationHelper.formatSeconds(totalElapsedSec),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        totalElapsedSec = 0
                        completedCycles = 0
                        currentPhase = BreathPhase.INHALE
                        phaseSecondsRemaining = selectedPattern.inhaleSec
                        phaseProgress = 0f
                        leafBurstTrigger++
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ZenSurface)
                        .border(1.dp, ZenBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Session",
                        tint = ZenTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Button(
                    onClick = {
                        isRunning = !isRunning
                        leafBurstTrigger++
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) ZenSurfaceVariant else currentTheme.primary,
                        contentColor = if (isRunning) ZenTextPrimary else ZenObsidian
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isRunning -> "Pause Practice"
                            totalElapsedSec == 0 -> "Begin Breathing Practice"
                            else -> "Resume Breathing"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun getPhaseDuration(phase: BreathPhase, pattern: BreathingPattern): Int {
    return when (phase) {
        BreathPhase.INHALE -> pattern.inhaleSec
        BreathPhase.HOLD_FULL -> pattern.holdFullSec
        BreathPhase.EXHALE -> pattern.exhaleSec
        BreathPhase.HOLD_EMPTY -> pattern.holdEmptySec
    }
}

private fun getNextPhase(current: BreathPhase, pattern: BreathingPattern): BreathPhase {
    return when (current) {
        BreathPhase.INHALE -> {
            if (pattern.holdFullSec > 0) BreathPhase.HOLD_FULL else BreathPhase.EXHALE
        }
        BreathPhase.HOLD_FULL -> BreathPhase.EXHALE
        BreathPhase.EXHALE -> {
            if (pattern.holdEmptySec > 0) BreathPhase.HOLD_EMPTY else BreathPhase.INHALE
        }
        BreathPhase.HOLD_EMPTY -> BreathPhase.INHALE
    }
}
