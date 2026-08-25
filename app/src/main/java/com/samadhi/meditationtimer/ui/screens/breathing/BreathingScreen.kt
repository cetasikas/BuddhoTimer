package com.samadhi.meditationtimer.ui.screens.breathing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.domain.model.BreathingPattern
import com.samadhi.meditationtimer.domain.model.TimerStatus
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
    onSessionCompleted: (TimerStatus.Completed) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundHelper = remember { SoundHelper(context) }
    val prefs = remember { context.getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE) }
    val keepScreenAwake = remember { prefs.getBoolean("keep_screen_awake", true) }
    val soundBellResName = remember { prefs.getString("sound_bell_res", SoundHelper.SOUND_TIBETAN_BOWL) ?: SoundHelper.SOUND_TIBETAN_BOWL }
    val patternId = remember { prefs.getString("breathing_pattern_id", "box") ?: "box" }
    val targetMinutes = remember { prefs.getInt("breathing_duration_min", 5) }
    var targetTotalSec by remember { mutableIntStateOf(targetMinutes * 60) }

    var selectedPattern by remember {
        mutableStateOf(BreathingPattern.allPatterns.find { it.id == patternId } ?: BreathingPattern.BOX_BREATHING)
    }
    var isTopPatternDropdownOpen by remember { mutableStateOf(false) }

    val tapToBlackout = remember { prefs.getBoolean("tap_blackout", true) }
    val dndManager = remember { com.samadhi.meditationtimer.service.DndManager(context) }
    var isAutoDnd by remember { mutableStateOf(prefs.getBoolean("auto_dnd", false)) }
    var isScreenBlackedOut by remember { mutableStateOf(false) }

    // Optional Do Not Disturb during breathwork session
    androidx.compose.runtime.DisposableEffect(isAutoDnd) {
        if (isAutoDnd && dndManager.isDndAccessGranted) {
            dndManager.enableDnd()
        }
        onDispose {
            if (isAutoDnd && dndManager.isDndAccessGranted) {
                dndManager.restoreDnd()
            }
        }
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

    // Completely Hide/Show System Bars (Status Bar & Navigation Buttons) on Screen Blackout
    androidx.compose.runtime.DisposableEffect(isScreenBlackedOut) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (isScreenBlackedOut) {
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = activity?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val prepSec = remember { prefs.getInt("prep_time_sec", 5) }
    val isCountdownHaptics = remember { prefs.getBoolean("countdown_haptics", true) }
    var isPreparing by remember { mutableStateOf(prepSec > 0) }
    var prepRemaining by remember { mutableIntStateOf(prepSec) }

    var isRunning by remember { mutableStateOf(true) }
    var currentPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var totalElapsedSec by remember { mutableIntStateOf(0) }
    var completedCycles by remember { mutableIntStateOf(0) }

    // Varying-Count Breath Stages (1: 9-Count, 2: 6-Count, 3: 3-Count, 4: Return 6->9)
    var varyingStage by remember { mutableIntStateOf(1) }
    var varyingCyclesInStage by remember { mutableIntStateOf(0) }
    var stage4Step by remember { mutableIntStateOf(0) }

    var phaseSecondsRemaining by remember {
        mutableIntStateOf(getPhaseDuration(BreathPhase.INHALE, selectedPattern, varyingStage, stage4Step))
    }

    // Smooth sub-second progress for fluid blooming transition
    var phaseProgress by remember { mutableFloatStateOf(0.0f) }
    var isSessionComplete by remember { mutableStateOf(false) }

    fun finishSession() {
        val now = System.currentTimeMillis()
        val completedStatus = TimerStatus.Completed(
            targetDurationSec = targetTotalSec,
            actualDurationSec = totalElapsedSec.coerceAtLeast(1),
            overrunSec = 0,
            startTimeEpochMs = now - (totalElapsedSec * 1000L),
            completedTimeEpochMs = now,
            isBoxBreathing = true
        )
        onSessionCompleted(completedStatus)
    }

    // Warm-up Preparation Countdown ticker (Haptic only, zero audio chime)
    LaunchedEffect(isPreparing) {
        if (!isPreparing) return@LaunchedEffect
        while (isActive && isPreparing && prepRemaining > 0) {
            if (isCountdownHaptics) {
                soundHelper.triggerSoftTouchHaptic()
            }
            delay(1000L)
            prepRemaining--
            if (prepRemaining <= 0) {
                isPreparing = false
                if (isCountdownHaptics) {
                    soundHelper.triggerGentleVibration()
                }
                break
            }
        }
    }

    // High precision, hardware-synced breath cycle ticker (100% stutter-free)
    LaunchedEffect(isRunning, selectedPattern, isPreparing, varyingStage, stage4Step, isSessionComplete) {
        if (!isRunning || isPreparing || isSessionComplete) return@LaunchedEffect

        currentPhase = BreathPhase.INHALE
        val initPhaseSec = getPhaseDuration(BreathPhase.INHALE, selectedPattern, varyingStage, stage4Step)
        phaseSecondsRemaining = initPhaseSec
        phaseProgress = 0.0f
        soundHelper.triggerGentleVibration()

        var phaseStartTimeMs = System.currentTimeMillis()

        while (isActive && isRunning && !isPreparing && !isSessionComplete) {
            withFrameNanos { _ ->
                val totalPhaseSec = getPhaseDuration(currentPhase, selectedPattern, varyingStage, stage4Step).coerceAtLeast(1)
                val totalPhaseMs = totalPhaseSec * 1000L
                val elapsedInPhaseMs = System.currentTimeMillis() - phaseStartTimeMs

                phaseProgress = (elapsedInPhaseMs.toFloat() / totalPhaseMs.toFloat()).coerceIn(0.0f, 1.0f)
                val secRemaining = (totalPhaseSec - (elapsedInPhaseMs / 1000L).toInt()).coerceAtLeast(1)
                phaseSecondsRemaining = secRemaining

                if (elapsedInPhaseMs >= totalPhaseMs) {
                    val nextPhase = getNextPhase(currentPhase, selectedPattern)
                    if (currentPhase == BreathPhase.EXHALE && (nextPhase == BreathPhase.INHALE || (selectedPattern.id == "box" && currentPhase == BreathPhase.HOLD_EMPTY))) {
                        completedCycles++
                        if (selectedPattern.id == "varying_count") {
                            varyingCyclesInStage++
                            if (varyingStage in 1..3 && varyingCyclesInStage >= 2) {
                                varyingStage++
                                varyingCyclesInStage = 0
                            } else if (varyingStage == 4) {
                                if (stage4Step == 0) {
                                    stage4Step = 1
                                    varyingCyclesInStage = 0
                                } else {
                                    varyingStage = 1
                                    stage4Step = 0
                                    varyingCyclesInStage = 0
                                }
                            }
                        }
                    }
                    if (currentPhase == BreathPhase.HOLD_EMPTY && nextPhase == BreathPhase.INHALE) {
                        completedCycles++
                    }
                    currentPhase = nextPhase
                    val newPhaseSec = getPhaseDuration(nextPhase, selectedPattern, varyingStage, stage4Step).coerceAtLeast(1)
                    phaseSecondsRemaining = newPhaseSec
                    phaseStartTimeMs = System.currentTimeMillis()
                    phaseProgress = 0.0f
                    soundHelper.triggerGentleVibration()
                }
            }
        }
    }

    // Secondary 1-second cadence ticker for total elapsed time
    LaunchedEffect(isRunning, isPreparing, isSessionComplete) {
        if (isPreparing || isSessionComplete) return@LaunchedEffect
        while (isActive && isRunning && !isPreparing && !isSessionComplete) {
            delay(1000L)
            totalElapsedSec++
            if (targetTotalSec > 0 && totalElapsedSec >= targetTotalSec) {
                val isCompletionBell = prefs.getBoolean("completion_bell_enabled", true)
                if (isCompletionBell) {
                    soundHelper.playChime(soundBellResName)
                }
                soundHelper.triggerHarmonicCompletionHaptic()
                isRunning = false
                isSessionComplete = true
                break
            }
        }
    }

    val currentTheme = com.samadhi.meditationtimer.ui.theme.LocalZenTheme.current
    val isVaryingCount = selectedPattern.id == "varying_count"
    val currentVaryingDuration = if (isVaryingCount) getVaryingStageDuration(varyingStage, stage4Step) else 0

    val displayCountText = if (isSessionComplete) {
        "🪷"
    } else if (isVaryingCount) {
        if (currentPhase == BreathPhase.INHALE) {
            val countUp = (currentVaryingDuration - phaseSecondsRemaining + 1).coerceIn(1, currentVaryingDuration)
            "$countUp"
        } else {
            val countDown = phaseSecondsRemaining.coerceIn(1, currentVaryingDuration)
            "$countDown"
        }
    } else {
        "$phaseSecondsRemaining"
    }

    val activeStageBadge = if (isSessionComplete) {
        "BREATHWORK COMPLETE 🪷"
    } else when (varyingStage) {
        1 -> "Stage 1 • 3-Count (Grounding Breath)"
        2 -> "Stage 2 • 4-Count (Centering Breath)"
        3 -> "Stage 3 • 5-Count (Coherence Breath)"
        4 -> if (stage4Step == 0) "Stage 4 • Return (4-Count)" else "Stage 4 • Return (3-Count)"
        else -> "Stage 1 • 3-Count"
    }

    val activeStageInstruction = if (isSessionComplete) {
        "${completedCycles} Cycles • ${totalElapsedSec / 60}m Practice"
    } else if (isVaryingCount) {
        if (currentPhase == BreathPhase.INHALE) {
            "Inhale steadily\nCount 1 → $currentVaryingDuration"
        } else {
            "Exhale smoothly\nCount $currentVaryingDuration → 1"
        }
    } else {
        currentPhase.instruction
    }

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
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                if (tapToBlackout) {
                    isScreenBlackedOut = !isScreenBlackedOut
                }
            }
    ) {
        if (isScreenBlackedOut) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "•",
                    fontSize = 14.sp,
                    color = currentTheme.primaryLight.copy(alpha = 0.25f)
                )
            }
        } else {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                val isLandscape = maxWidth > maxHeight

                if (isLandscape) {
                    // Landscape Layout: Header top, Ensō on Left, Progress & Controls on Right
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Pattern Selector Dropdown Pill
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ZenSurface.copy(alpha = 0.85f))
                                        .border(1.dp, currentTheme.primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .clickable { isTopPatternDropdownOpen = true }
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
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
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch Pattern",
                                        tint = currentTheme.primaryLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isTopPatternDropdownOpen,
                                    onDismissRequest = { isTopPatternDropdownOpen = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.60f)
                                        .background(ZenSurface.copy(alpha = 0.98f))
                                        .border(1.2.dp, currentTheme.primary.copy(alpha = 0.50f), RoundedCornerShape(16.dp))
                                        .padding(vertical = 4.dp)
                                ) {
                                    data class TopPatternMenuItem(
                                        val pattern: BreathingPattern,
                                        val summary: String
                                    )

                                    val topPatterns = listOf(
                                        TopPatternMenuItem(BreathingPattern.BOX_BREATHING, "Focus & Stress Reset • 4s In • 4s Hold • 4s Out • 4s Hold"),
                                        TopPatternMenuItem(BreathingPattern.VARYING_COUNT, "Anti-Autopilot • 3-Count → 4-Count → 5-Count → Return"),
                                        TopPatternMenuItem(BreathingPattern.RELAX_4_7_8, "Deep Sleep & Relaxation • 4s In • 7s Hold • 8s Out"),
                                        TopPatternMenuItem(BreathingPattern.RESONANT_BREATH, "Heart Coherence • 5s Inhale • 5s Exhale"),
                                        TopPatternMenuItem(BreathingPattern.TRANQUIL_3_3_3_3, "Beginner Friendly • 3s In • 3s Hold • 3s Out • 3s Hold")
                                    )

                                    topPatterns.forEach { item ->
                                        val isSelected = selectedPattern.id == item.pattern.id
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
                                                        Text(
                                                            text = item.pattern.name,
                                                            fontSize = 13.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) currentTheme.primaryLight else ZenTextPrimary
                                                        )
                                                        Text(
                                                            text = item.summary,
                                                            fontSize = 11.sp,
                                                            color = if (isSelected) currentTheme.primaryLight.copy(alpha = 0.85f) else ZenTextSecondary
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedPattern = item.pattern
                                                currentPhase = BreathPhase.INHALE
                                                phaseSecondsRemaining = item.pattern.inhaleSec
                                                phaseProgress = 0f
                                                prefs.edit().putString("breathing_pattern_id", item.pattern.id).apply()
                                                isTopPatternDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                            // Close / Cancel Button
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ZenSurface.copy(alpha = 0.75f))
                                    .border(1.dp, ZenBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit Breathwork",
                                    tint = ZenTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Bottom Landscape Split Row
                        if (isPreparing) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(currentTheme.primary.copy(alpha = 0.18f))
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "PREPARATION",
                                        fontSize = 12.sp,
                                        letterSpacing = 3.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentTheme.primaryLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(ZenSurface.copy(alpha = 0.65f))
                                        .border(2.dp, currentTheme.primaryLight.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$prepRemaining",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.ExtraLight,
                                        color = ZenTextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Settle into your posture and prepare to breathe...",
                                    fontSize = 13.sp,
                                    color = ZenTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Column: Ensō Visualizer
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ZenParticleBreathingCanvas(
                                        currentPhase = currentPhase,
                                        phaseProgress = phaseProgress,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Right Column: Controls & Instructions
                                Column(
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .fillMaxHeight()
                                        .padding(start = 12.dp, end = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = displayCountText,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Light,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    AnimatedContent(
                                        targetState = currentPhase to (if (isVaryingCount) varyingStage else 0),
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + 
                                             slideInVertically(animationSpec = tween(400, easing = LinearOutSlowInEasing)) { 4 })
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) + 
                                                    slideOutVertically(animationSpec = tween(300, easing = FastOutLinearInEasing)) { -4 }
                                                )
                                        },
                                        label = "phaseInstructionFlow"
                                    ) { (phase, _) ->
                                        val itemColor = when (phase) {
                                            BreathPhase.INHALE -> currentTheme.primary
                                            BreathPhase.HOLD_FULL -> currentTheme.primaryLight
                                            BreathPhase.EXHALE -> currentTheme.overrunColor
                                            BreathPhase.HOLD_EMPTY -> ZenAmber
                                        }
                                        val textToDisplay = if (isVaryingCount) {
                                            if (phase == BreathPhase.INHALE) {
                                                "Inhale steadily\nCount 1 → $currentVaryingDuration"
                                            } else {
                                                "Exhale smoothly\nCount $currentVaryingDuration → 1"
                                            }
                                        } else {
                                            phase.instruction
                                        }
                                        Text(
                                            text = textToDisplay,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = itemColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            lineHeight = 20.sp,
                                            letterSpacing = 0.3.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (isVaryingCount) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(currentTheme.primary.copy(alpha = 0.15f))
                                                .border(1.dp, currentTheme.primaryLight.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = activeStageBadge,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = currentTheme.primaryLight
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    Text(
                                        text = "${NotificationHelper.formatSeconds(totalElapsedSec)} / ${NotificationHelper.formatSeconds(targetTotalSec)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ZenTextPrimary.copy(alpha = 0.85f),
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // DND Quick Toggle Button
                                        IconButton(
                                            onClick = {
                                                if (dndManager.isDndAccessGranted) {
                                                    val next = !isAutoDnd
                                                    isAutoDnd = next
                                                    prefs.edit().putBoolean("auto_dnd", next).apply()
                                                    if (next) dndManager.enableDnd() else dndManager.restoreDnd()
                                                } else {
                                                    context.startActivity(
                                                        dndManager.requestDndPermissionIntent().apply {
                                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                        }
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isAutoDnd && dndManager.isDndAccessGranted) currentTheme.primary.copy(alpha = 0.22f)
                                                    else ZenSurface.copy(alpha = 0.75f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isAutoDnd && dndManager.isDndAccessGranted) currentTheme.primaryLight.copy(alpha = 0.6f) else ZenBorder,
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DoNotDisturb,
                                                contentDescription = "Toggle DND",
                                                tint = if (isAutoDnd && dndManager.isDndAccessGranted) currentTheme.primaryLight else ZenTextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                    if (isSessionComplete) {
                                        Button(
                                            onClick = { finishSession() },
                                            modifier = Modifier.height(42.dp),
                                            shape = RoundedCornerShape(21.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = currentTheme.primary,
                                                contentColor = ZenObsidian
                                            )
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = ZenObsidian, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Save & Finish", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                targetTotalSec += 180
                                                isSessionComplete = false
                                                isRunning = true
                                            },
                                            modifier = Modifier.height(42.dp),
                                            shape = RoundedCornerShape(21.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                            border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.7f))
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Keep Breathing", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                totalElapsedSec = 0
                                                completedCycles = 0
                                                currentPhase = BreathPhase.INHALE
                                                phaseSecondsRemaining = selectedPattern.inhaleSec
                                                phaseProgress = 0f
                                            },
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(ZenSurface.copy(alpha = 0.75f))
                                                .border(1.dp, ZenBorder, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Restart",
                                                tint = ZenTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Button(
                                            onClick = { isRunning = !isRunning },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isRunning) ZenSurfaceVariant else currentTheme.primary,
                                                contentColor = if (isRunning) ZenTextPrimary else ZenObsidian
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isRunning) "Pause" else "Resume",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isRunning) "Pause" else "Resume",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Button(
                                            onClick = { finishSession() },
                                            modifier = Modifier.height(42.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ZenSurface.copy(alpha = 0.85f),
                                                contentColor = currentTheme.primaryLight
                                            ),
                                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary.copy(alpha = 0.5f)))
                                        ) {
                                            Text(
                                                text = "Finish",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // --- 1. MINIMAL IMMERSIVE TOP BAR ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Interactive Pattern Selector Dropdown Pill
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ZenSurface.copy(alpha = 0.85f))
                                        .border(1.dp, currentTheme.primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .clickable { isTopPatternDropdownOpen = true }
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
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch Pattern",
                                        tint = currentTheme.primaryLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isTopPatternDropdownOpen,
                                    onDismissRequest = { isTopPatternDropdownOpen = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.90f)
                                        .background(ZenSurface.copy(alpha = 0.98f))
                                        .border(1.2.dp, currentTheme.primary.copy(alpha = 0.50f), RoundedCornerShape(16.dp))
                                        .padding(vertical = 4.dp)
                                ) {
                                    data class TopPatternMenuItem(
                                        val pattern: BreathingPattern,
                                        val summary: String
                                    )

                                    val topPatterns = listOf(
                                        TopPatternMenuItem(BreathingPattern.BOX_BREATHING, "Focus & Stress Reset • 4s In • 4s Hold • 4s Out • 4s Hold"),
                                        TopPatternMenuItem(BreathingPattern.VARYING_COUNT, "Anti-Autopilot • 3-Count → 4-Count → 5-Count → Return"),
                                        TopPatternMenuItem(BreathingPattern.RELAX_4_7_8, "Deep Sleep & Relaxation • 4s In • 7s Hold • 8s Out"),
                                        TopPatternMenuItem(BreathingPattern.RESONANT_BREATH, "Heart Coherence • 5s Inhale • 5s Exhale"),
                                        TopPatternMenuItem(BreathingPattern.TRANQUIL_3_3_3_3, "Beginner Friendly • 3s In • 3s Hold • 3s Out • 3s Hold")
                                    )

                                    topPatterns.forEach { item ->
                                        val isSelected = selectedPattern.id == item.pattern.id
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(vertical = 2.dp)) {
                                                        Text(
                                                            text = item.pattern.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) currentTheme.primaryLight else ZenTextPrimary
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = item.summary,
                                                            fontSize = 11.5.sp,
                                                            color = if (isSelected) currentTheme.primaryLight.copy(alpha = 0.85f) else ZenTextSecondary
                                                        )
                                                    }

                                                    if (isSelected) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(currentTheme.primary.copy(alpha = 0.35f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PlayArrow,
                                                                contentDescription = "Active Pattern",
                                                                tint = currentTheme.primaryLight,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedPattern = item.pattern
                                                currentPhase = BreathPhase.INHALE
                                                phaseSecondsRemaining = item.pattern.inhaleSec
                                                phaseProgress = 0f
                                                prefs.edit().putString("breathing_pattern_id", item.pattern.id).apply()
                                                isTopPatternDropdownOpen = false
                                            },
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) currentTheme.primary.copy(alpha = 0.20f) else Color.Transparent)
                                                .border(
                                                    if (isSelected) 1.2.dp else 0.dp,
                                                    if (isSelected) currentTheme.primaryLight.copy(alpha = 0.70f) else Color.Transparent,
                                                    RoundedCornerShape(10.dp)
                                                )
                                        )
                                    }
                                }
                            }

                            // Close / Cancel Button
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

                        if (isPreparing) {
                            // --- PREPARATION COUNTDOWN VIEW ---
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(currentTheme.primary.copy(alpha = 0.18f))
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "PREPARATION",
                                        fontSize = 12.sp,
                                        letterSpacing = 3.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentTheme.primaryLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(CircleShape)
                                        .background(ZenSurface.copy(alpha = 0.65f))
                                        .border(2.dp, currentTheme.primaryLight.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$prepRemaining",
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.ExtraLight,
                                        color = ZenTextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                Text(
                                    text = "Settle into your posture and prepare to breathe...",
                                    fontSize = 14.sp,
                                    color = ZenTextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(58.dp))
                        } else {
                            // --- 2. CENTRAL LIVING ZEN ENSŌ VISUALIZER ---
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                ZenParticleBreathingCanvas(
                                    currentPhase = currentPhase,
                                    phaseProgress = phaseProgress,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                ) {
                                    Text(
                                        text = displayCountText,
                                        fontSize = 58.sp,
                                        fontWeight = FontWeight.Light,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    AnimatedContent(
                                        targetState = currentPhase to (if (isVaryingCount) varyingStage else 0),
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + 
                                             slideInVertically(animationSpec = tween(400, easing = LinearOutSlowInEasing)) { 4 })
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) + 
                                                    slideOutVertically(animationSpec = tween(300, easing = FastOutLinearInEasing)) { -4 }
                                                )
                                        },
                                        label = "phaseInstructionFlow"
                                    ) { (phase, _) ->
                                        val itemColor = when (phase) {
                                            BreathPhase.INHALE -> currentTheme.primary
                                            BreathPhase.HOLD_FULL -> currentTheme.primaryLight
                                            BreathPhase.EXHALE -> currentTheme.overrunColor
                                            BreathPhase.HOLD_EMPTY -> ZenAmber
                                        }
                                        val textToDisplay = if (isVaryingCount) {
                                            if (phase == BreathPhase.INHALE) {
                                                "Inhale steadily\nCount 1 → $currentVaryingDuration"
                                            } else {
                                                "Exhale smoothly\nCount $currentVaryingDuration → 1"
                                            }
                                        } else {
                                            phase.instruction
                                        }
                                        Text(
                                            text = textToDisplay,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = itemColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            lineHeight = 22.sp,
                                            letterSpacing = 0.3.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }

                            // --- 3. SESSION PROGRESS & STAGE INFO ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isVaryingCount) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(currentTheme.primary.copy(alpha = 0.18f))
                                            .border(1.dp, currentTheme.primaryLight.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = activeStageBadge,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = currentTheme.primaryLight
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                Text(
                                    text = "${NotificationHelper.formatSeconds(totalElapsedSec)} / ${NotificationHelper.formatSeconds(targetTotalSec)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZenTextPrimary.copy(alpha = 0.85f),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isSessionComplete) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { finishSession() },
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = currentTheme.primary,
                                            contentColor = ZenObsidian
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = ZenObsidian, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save & Finish", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            targetTotalSec += 180
                                            isSessionComplete = false
                                            isRunning = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                        border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.7f))
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Keep Breathing", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // DND Quick Toggle Button
                                    IconButton(
                                        onClick = {
                                            if (dndManager.isDndAccessGranted) {
                                                val next = !isAutoDnd
                                                isAutoDnd = next
                                                prefs.edit().putBoolean("auto_dnd", next).apply()
                                                if (next) dndManager.enableDnd() else dndManager.restoreDnd()
                                            } else {
                                                context.startActivity(
                                                    dndManager.requestDndPermissionIntent().apply {
                                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isAutoDnd && dndManager.isDndAccessGranted) currentTheme.primary.copy(alpha = 0.22f)
                                                else ZenSurface.copy(alpha = 0.75f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isAutoDnd && dndManager.isDndAccessGranted) currentTheme.primaryLight.copy(alpha = 0.6f) else ZenBorder,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DoNotDisturb,
                                            contentDescription = "Toggle DND",
                                            tint = if (isAutoDnd && dndManager.isDndAccessGranted) currentTheme.primaryLight else ZenTextMuted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

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
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(ZenSurface.copy(alpha = 0.75f))
                                            .border(1.dp, ZenBorder, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Restart",
                                            tint = ZenTextSecondary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Pause / Resume Button (No text clipping)
                                    Button(
                                        onClick = { isRunning = !isRunning },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRunning) ZenSurfaceVariant else currentTheme.primary,
                                            contentColor = if (isRunning) ZenTextPrimary else ZenObsidian
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isRunning) "Pause" else "Resume",
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isRunning) "Pause" else "Resume",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    // Finish Button
                                    Button(
                                        onClick = { finishSession() },
                                        modifier = Modifier.height(46.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ZenSurface.copy(alpha = 0.85f),
                                            contentColor = currentTheme.primaryLight
                                        ),
                                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary.copy(alpha = 0.5f)))
                                    ) {
                                        Text(
                                            text = "Finish",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getVaryingStageDuration(stage: Int, stage4Step: Int): Int {
    return when (stage) {
        1 -> 3
        2 -> 4
        3 -> 5
        4 -> if (stage4Step == 0) 4 else 3
        else -> 3
    }
}

private fun getPhaseDuration(
    phase: BreathPhase,
    pattern: BreathingPattern,
    varyingStage: Int = 1,
    stage4Step: Int = 0
): Int {
    if (pattern.id == "varying_count") {
        val stageDuration = getVaryingStageDuration(varyingStage, stage4Step)
        return when (phase) {
            BreathPhase.INHALE -> stageDuration
            BreathPhase.EXHALE -> stageDuration
            BreathPhase.HOLD_FULL -> 0
            BreathPhase.HOLD_EMPTY -> 0
        }
    }
    return when (phase) {
        BreathPhase.INHALE -> pattern.inhaleSec
        BreathPhase.HOLD_FULL -> pattern.holdFullSec
        BreathPhase.EXHALE -> pattern.exhaleSec
        BreathPhase.HOLD_EMPTY -> pattern.holdEmptySec
    }
}

private fun getNextPhase(current: BreathPhase, pattern: BreathingPattern): BreathPhase {
    if (pattern.id == "varying_count") {
        return when (current) {
            BreathPhase.INHALE -> BreathPhase.EXHALE
            BreathPhase.EXHALE -> BreathPhase.INHALE
            else -> BreathPhase.INHALE
        }
    }
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
