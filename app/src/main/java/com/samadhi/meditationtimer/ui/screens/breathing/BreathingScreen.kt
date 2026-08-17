package com.samadhi.meditationtimer.ui.screens.breathing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.domain.model.BreathingPattern
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.service.SoundHelper
import com.samadhi.meditationtimer.ui.components.ZenParticleBreathingCanvas
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private fun android.content.Context.findActivity(): android.app.Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun BreathingScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundHelper = remember { SoundHelper(context) }
    val prefs = remember { context.getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE) }
    val keepScreenAwake = remember { prefs.getBoolean("keep_screen_awake", true) }
    val patternId = remember { prefs.getString("breathing_pattern_id", "box") ?: "box" }
    val targetMinutes = remember { prefs.getInt("breathing_duration_min", 5) }
    val targetTotalSec = targetMinutes * 60

    val selectedPattern = remember {
        BreathingPattern.allPatterns.find { it.id == patternId } ?: BreathingPattern.BOX_BREATHING
    }

    // Keep Screen Awake based on user preference
    androidx.compose.runtime.DisposableEffect(keepScreenAwake) {
        val activity = context.findActivity()
        if (keepScreenAwake) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var isRunning by remember { mutableStateOf(true) }
    var currentPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var phaseSecondsRemaining by remember { mutableIntStateOf(selectedPattern.inhaleSec) }
    var totalElapsedSec by remember { mutableIntStateOf(0) }
    var completedCycles by remember { mutableIntStateOf(0) }

    // Smooth sub-second progress for fluid blooming transition
    var phaseProgress by remember { mutableFloatStateOf(0.0f) }

    // High precision, hardware-synced breath cycle ticker (100% stutter-free)
    LaunchedEffect(isRunning, selectedPattern) {
        if (!isRunning) return@LaunchedEffect

        currentPhase = BreathPhase.INHALE
        phaseSecondsRemaining = selectedPattern.inhaleSec
        phaseProgress = 0.0f
        soundHelper.triggerGentleVibration()

        var phaseStartTimeMs = System.currentTimeMillis()

        while (isActive && isRunning) {
            withFrameNanos { _ ->
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
            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. MINIMAL IMMERSIVE TOP BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Current Pattern & Cycle Indicator Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ZenSurface.copy(alpha = 0.75f))
                        .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPattern.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = currentTheme.primaryLight
                    )
                    Text(
                        text = " • Cycle $completedCycles",
                        fontSize = 12.sp,
                        color = ZenTextSecondary
                    )
                }

                // Close / Finish Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ZenSurface.copy(alpha = 0.75f))
                        .border(1.dp, ZenBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Breathwork",
                        tint = ZenTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- 2. CENTRAL LIVING BLOOMING LEAF VISUALIZER ---
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
                    leafCount = 220
                )
            }

            // --- 3. MINIMAL PHASE & COUNTDOWN INSTRUCTIONS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Minimalist Countdown
                Text(
                    text = "$phaseSecondsRemaining",
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                AnimatedContent(
                    targetState = currentPhase,
                    transitionSpec = {
                        (fadeIn(tween(350)) + slideInVertically(tween(350)) { 8 })
                            .togetherWith(fadeOut(tween(250)) + slideOutVertically(tween(250)) { -8 })
                    },
                    label = "phaseTextFlow"
                ) { phase ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = phase.displayName.uppercase(),
                            fontSize = 17.sp,
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = phase.instruction,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = ZenTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time Elapsed vs Target
                Text(
                    text = "${NotificationHelper.formatSeconds(totalElapsedSec)} / ${NotificationHelper.formatSeconds(targetTotalSec)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ZenTextMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 4. MINIMAL CONTROL BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = {
                        totalElapsedSec = 0
                        completedCycles = 0
                        currentPhase = BreathPhase.INHALE
                        phaseSecondsRemaining = selectedPattern.inhaleSec
                        phaseProgress = 0f
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ZenSurface.copy(alpha = 0.75f))
                        .border(1.dp, ZenBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = ZenTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Pause / Resume Button
                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) ZenSurfaceVariant else currentTheme.primary,
                        contentColor = if (isRunning) ZenTextPrimary else ZenObsidian
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Resume",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Pause" else "Resume",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }

                // Finish Button
                Button(
                    onClick = onClose,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZenSurface.copy(alpha = 0.85f),
                        contentColor = currentTheme.primaryLight
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary.copy(alpha = 0.5f)))
                ) {
                    Text("Finish", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
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
            if (pattern.holdFullSec > 0) BreathPhase.HOLD_FULL
            else BreathPhase.EXHALE
        }
        BreathPhase.HOLD_FULL -> BreathPhase.EXHALE
        BreathPhase.EXHALE -> {
            if (pattern.holdEmptySec > 0) BreathPhase.HOLD_EMPTY
            else BreathPhase.INHALE
        }
        BreathPhase.HOLD_EMPTY -> BreathPhase.INHALE
    }
}
