package com.samadhi.meditationtimer.ui.screens.breathing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.domain.model.BreathingPattern
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.service.SoundHelper
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenAmberDark
import com.samadhi.meditationtimer.ui.theme.ZenAmberGlow
import com.samadhi.meditationtimer.ui.theme.ZenBorder
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
    var isRunning by remember { mutableStateOf(true) }
    var currentPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var phaseSecondsRemaining by remember { mutableIntStateOf(selectedPattern.inhaleSec) }
    var totalElapsedSec by remember { mutableIntStateOf(0) }
    var completedCycles by remember { mutableIntStateOf(0) }

    // Breathing timer ticker loop
    LaunchedEffect(isRunning, selectedPattern) {
        if (!isRunning) return@LaunchedEffect

        // Reset phase to inhale upon pattern switch
        currentPhase = BreathPhase.INHALE
        phaseSecondsRemaining = selectedPattern.inhaleSec
        soundHelper.triggerGentleVibration()

        while (isActive && isRunning) {
            delay(1000L)
            totalElapsedSec++
            phaseSecondsRemaining--

            if (phaseSecondsRemaining <= 0) {
                // Advance to next valid phase in pattern
                val nextPhase = getNextPhase(currentPhase, selectedPattern)
                if (nextPhase == BreathPhase.INHALE) {
                    completedCycles++
                }
                currentPhase = nextPhase
                phaseSecondsRemaining = getPhaseDuration(nextPhase, selectedPattern)
                soundHelper.triggerGentleVibration()
            }
        }
    }

    // Dynamic scale target: Inhale -> 1.0f, Hold Full -> 1.0f, Exhale -> 0.35f, Hold Empty -> 0.35f
    val targetScale = when (currentPhase) {
        BreathPhase.INHALE -> 1.0f
        BreathPhase.HOLD_FULL -> 1.0f
        BreathPhase.EXHALE -> 0.35f
        BreathPhase.HOLD_EMPTY -> 0.35f
    }

    val currentPhaseDurationMs = (getPhaseDuration(currentPhase, selectedPattern) * 1000).coerceAtLeast(1000)
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = if (currentPhase == BreathPhase.HOLD_FULL || currentPhase == BreathPhase.HOLD_EMPTY) 400 else currentPhaseDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "breathScale"
    )

    val phaseColor = when (currentPhase) {
        BreathPhase.INHALE -> ZenTeal
        BreathPhase.HOLD_FULL -> ZenAmber
        BreathPhase.EXHALE -> ZenOverrunBlue
        BreathPhase.HOLD_EMPTY -> ZenAmberDark
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZenObsidian)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = ZenTeal,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Box Breathing",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = "Vagus Nerve & Panic Relief",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenTextSecondary
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ZenSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Breathing",
                    tint = ZenTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pattern Selection Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BreathingPattern.allPatterns.forEach { pattern ->
                val isSelected = selectedPattern.id == pattern.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedPattern = pattern
                        currentPhase = BreathPhase.INHALE
                        phaseSecondsRemaining = pattern.inhaleSec
                    },
                    label = { Text(pattern.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ZenSurface,
                        labelColor = ZenTextSecondary,
                        selectedContainerColor = ZenTeal,
                        selectedLabelColor = ZenObsidian
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) ZenTeal else ZenBorder,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Animated Breathing Visualizer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Soothing Canvas rings expanding and contracting
            Canvas(modifier = Modifier.size(280.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f - 20.dp.toPx()
                val currentRadius = maxRadius * animatedScale

                // 1. Soft Outer Radial Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            phaseColor.copy(alpha = 0.25f),
                            phaseColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = (currentRadius * 1.35f).coerceAtLeast(10f)
                    ),
                    radius = currentRadius * 1.35f,
                    center = center
                )

                // 2. Base Guide Ring
                drawCircle(
                    color = ZenBorder.copy(alpha = 0.5f),
                    radius = maxRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3. Inner Breathing Ring
                drawCircle(
                    color = phaseColor,
                    radius = currentRadius.coerceAtLeast(20f),
                    center = center,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Center Phase Text & Countdown
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentPhase.displayName.uppercase(),
                    fontSize = 24.sp,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$phaseSecondsRemaining",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Light,
                    color = ZenTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentPhase.instruction,
                    fontSize = 12.sp,
                    color = ZenTextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Stats Card (Cycles & Total Time)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ZenSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ZenBorder, ZenBorder))),
            shape = RoundedCornerShape(14.dp)
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
                        letterSpacing = 1.sp,
                        color = ZenTextSecondary
                    )
                    Text(
                        text = "$completedCycles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenTeal
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL CALM TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ZenTextSecondary
                    )
                    Text(
                        text = NotificationHelper.formatSeconds(totalElapsedSec),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Play / Pause & Reset Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    totalElapsedSec = 0
                    completedCycles = 0
                    currentPhase = BreathPhase.INHALE
                    phaseSecondsRemaining = selectedPattern.inhaleSec
                },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ZenSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Session",
                    tint = ZenTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) ZenSurfaceVariant else ZenTeal,
                    contentColor = if (isRunning) ZenTextPrimary else ZenObsidian
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Resume",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Pause Exercise" else "Resume Breathing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
