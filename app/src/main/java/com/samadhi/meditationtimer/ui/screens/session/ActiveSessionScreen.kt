package com.samadhi.meditationtimer.ui.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.TimerStatus
import com.samadhi.meditationtimer.service.MeditationTimerService
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.ui.components.CircularTimerProgress
import com.samadhi.meditationtimer.ui.screens.timer.TimerViewModel
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenAmberDark
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenDanger
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenOverrunBlue
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTealLight
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme

@Composable
fun ActiveSessionScreen(
    viewModel: TimerViewModel,
    onSessionCompleted: () -> Unit,
    onSessionCancelled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timerStatus by viewModel.timerStatus.collectAsState()
    val config by viewModel.config.collectAsState()
    val currentTheme = LocalZenTheme.current
    var isScreenBlackedOut by remember { mutableStateOf(false) }
    var leavesBurstTrigger by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Timer position measured via onGloballyPositioned — used to keep leaves out of the timer circle
    var timerCenterXRatio by remember { mutableFloatStateOf(0.5f) }
    var timerCenterYRatio by remember { mutableFloatStateOf(-1f) }   // negative = not yet measured
    var timerRadiusRatio by remember { mutableFloatStateOf(0f) }
    // Root Box size captured for ratio calculation
    var rootWidthPx by remember { mutableFloatStateOf(1f) }
    var rootHeightPx by remember { mutableFloatStateOf(1f) }

    // Keep Screen Awake based on user setting
    androidx.compose.runtime.DisposableEffect(config.keepScreenAwake) {
        val activity = context.findActivity()
        if (config.keepScreenAwake) {
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

    LaunchedEffect(timerStatus, config.showSessionOverview) {
        if (timerStatus is TimerStatus.Completed) {
            if (config.showSessionOverview) {
                onSessionCompleted()
            }
        } else if (timerStatus is TimerStatus.Idle) {
            onSessionCancelled()
        }
    }

    var isBlowingAway by remember { mutableStateOf(false) }
    var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val elapsedMeditationSec = when (val s = timerStatus) {
        is TimerStatus.Running -> s.elapsedSeconds
        is TimerStatus.Paused -> s.elapsedSeconds
        is TimerStatus.Completed -> s.actualDurationSec
        else -> 0
    }

    val sessionProgress = when (val s = timerStatus) {
        is TimerStatus.Running -> {
            val targetSec = config.targetDurationSeconds
            if (targetSec > 0) (s.elapsedSeconds.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)
            else ((s.elapsedSeconds % 1800).toFloat() / 1800f)
        }
        is TimerStatus.Paused -> {
            val targetSec = config.targetDurationSeconds
            if (targetSec > 0) (s.elapsedSeconds.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)
            else ((s.elapsedSeconds % 1800).toFloat() / 1800f)
        }
        else -> 0.0f
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                rootWidthPx  = coords.size.width.toFloat().coerceAtLeast(1f)
                rootHeightPx = coords.size.height.toFloat().coerceAtLeast(1f)
            }
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (config.tapToBlackoutEnabled) {
                    isScreenBlackedOut = !isScreenBlackedOut
                }
            }
    ) {
        // 1. Ambient Falling Leaves (permanently mounted, restored on resume via elapsedMeditationSeconds)
        if (config.showLeavesDuringMeditation) {
            FallingLeavesEffect(
                leafCount = config.meditationLeafCount.coerceIn(10, 24),
                pileUpAtBottom = true,
                sessionProgressRatio = sessionProgress,
                initialSettledLeavesCount = (elapsedMeditationSec / 12).coerceIn(0, 160),
                elapsedMeditationSeconds = elapsedMeditationSec,
                ambientAlpha = 0.38f,
                isBlowingAway = isBlowingAway,
                onBlowAwayComplete = {
                    pendingExitAction?.invoke()
                },
                timerCenterXRatio = timerCenterXRatio,
                timerCenterYRatio = timerCenterYRatio,
                timerRadiusRatio = timerRadiusRatio
            )
        }

        if (isScreenBlackedOut) {
            // Pure AMOLED Stealth Blackout (100% Black overlay covers screen without unmounting canvas)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        } else {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val isLandscape = maxWidth > maxHeight
                val density = LocalDensity.current
                val fontScale = density.fontScale.coerceIn(0.85f, 2.0f)
                val screenScale = (maxHeight / 720.dp).coerceIn(0.72f, 1.05f)
                val dynamicScale = screenScale.coerceIn(0.72f, 1.05f)
                val textScale = (maxHeight / 720.dp).coerceIn(0.88f, 1.05f)
                val timerSize = if (isLandscape) {
                    (maxHeight * 0.70f).coerceIn(160.dp, 260.dp)
                } else {
                    (maxHeight * 0.38f).coerceIn(190.dp, 300.dp)
                }

                fun adaptiveSp(baseSp: Float, maxFontMultiplier: Float = 1.25f): androidx.compose.ui.unit.TextUnit {
                    val clampedMultiplier = fontScale.coerceIn(0.85f, maxFontMultiplier)
                    val viewportCompression = (maxHeight / 720.dp).coerceIn(0.86f, 1.05f)
                    return (baseSp * viewportCompression * (clampedMultiplier / fontScale)).sp
                }
                val digitFontScaleMultiplier = fontScale.coerceIn(0.85f, 1.18f) / fontScale

                // Top Navigation Controls (Clean Minimal Cancel Action)
                val dndManager = remember { com.samadhi.meditationtimer.service.DndManager(context) }
                var isDndActive by remember { mutableStateOf(config.isAutoDndEnabled && dndManager.isDndAccessGranted) }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!isBlowingAway) {
                                pendingExitAction = {
                                    viewModel.cancelSession()
                                    onSessionCancelled()
                                }
                                if (config.showLeavesDuringMeditation) {
                                    isBlowingAway = true
                                } else {
                                    pendingExitAction?.invoke()
                                }
                            }
                        },
                        enabled = !isBlowingAway,
                        modifier = Modifier
                            .size(if (isLandscape) 36.dp else 44.dp)
                            .clip(CircleShape)
                            .background(ZenSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Practice",
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(if (isLandscape) 16.dp else 20.dp)
                        )
                    }
                }

                when (val status = timerStatus) {
                    is TimerStatus.Preparing -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(currentTheme.primary.copy(alpha = 0.18f))
                                    .border(1.dp, currentTheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 18.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "PREPARATION",
                                    fontSize = 12.sp,
                                    letterSpacing = 3.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentTheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else (20.dp * dynamicScale).coerceIn(10.dp, 28.dp)))

                            Box(
                                modifier = Modifier
                                    .size(if (isLandscape) 110.dp else (150.dp * dynamicScale).coerceIn(100.dp, 160.dp))
                                    .clip(CircleShape)
                                    .background(currentTheme.surfaceVariant)
                                    .border(2.dp, currentTheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${status.remainingSeconds}",
                                    fontSize = if (isLandscape) 52.sp else adaptiveSp(76f, 1.18f),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else (16.dp * dynamicScale).coerceIn(8.dp, 20.dp)))

                            Text(
                                text = "Settle into your posture & breathe gently",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ZenTextSecondary
                            )
                        }
                    }

                    is TimerStatus.Running, is TimerStatus.Paused -> {
                        val isPaused = status is TimerStatus.Paused
                        val remainingSec = if (status is TimerStatus.Running) status.remainingSeconds else (status as TimerStatus.Paused).remainingSeconds
                        val elapsedSec = if (status is TimerStatus.Running) status.elapsedSeconds else (status as TimerStatus.Paused).elapsedSeconds
                        val isOverrun = if (status is TimerStatus.Running) status.isOverrun else (status as TimerStatus.Paused).isOverrun

                        val sessionTotalSec = elapsedSec + remainingSec
                        val progress = if (sessionTotalSec > 0 && !config.isOpenEnded) {
                            (elapsedSec.toFloat() / sessionTotalSec.toFloat()).coerceIn(0f, 1f)
                        } else 1.0f
                        val currentTargetMinutes = if (sessionTotalSec > 0) (sessionTotalSec / 60).coerceAtLeast(1) else config.targetDurationMinutes

                        if (isLandscape) {
                            // Landscape layout: Timer on left, Header badge + Actions on right
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        val pos = coords.positionInRoot()
                                        val w   = coords.size.width.toFloat()
                                        val h   = coords.size.height.toFloat()
                                        val cx  = pos.x + w / 2f
                                        val cy  = pos.y + h / 2f
                                        if (rootWidthPx > 1f && rootHeightPx > 1f) {
                                            timerCenterXRatio = (cx / rootWidthPx).coerceIn(0.1f, 0.9f)
                                            timerCenterYRatio = (cy / rootHeightPx).coerceIn(0.1f, 0.9f)
                                            timerRadiusRatio  = (minOf(w, h) / 2f / rootHeightPx).coerceIn(0.05f, 0.45f)
                                        }
                                    }
                                ) {
                                    CircularTimerProgress(
                                        progress = progress,
                                        isOverrun = isOverrun,
                                        isPaused = isPaused,
                                        size = timerSize,
                                        strokeWidth = 8.dp,
                                        intervalMinutes = config.mindfulnessIntervalMinutes,
                                        totalDurationMinutes = currentTargetMinutes
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (isOverrun) {
                                                val overrunTime = elapsedSec - sessionTotalSec
                                                Text(
                                                    text = "+${NotificationHelper.formatSeconds(overrunTime.coerceAtLeast(0))}",
                                                    fontSize = 40.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = currentTheme.overrunColor
                                                )
                                                Text(
                                                    text = "Total: ${NotificationHelper.formatSeconds(elapsedSec)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ZenTextSecondary
                                                )
                                            } else if (config.isOpenEnded) {
                                                Text(
                                                    text = NotificationHelper.formatSeconds(elapsedSec),
                                                    fontSize = 46.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = "Elapsed Time",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ZenTextSecondary
                                                )
                                            } else {
                                                Text(
                                                    text = NotificationHelper.formatSeconds(remainingSec),
                                                    fontSize = 48.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = (-1).sp,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = "Elapsed: ${NotificationHelper.formatSeconds(elapsedSec)}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = ZenTextSecondary
                                                )
                                                val endTimeMs = remember(remainingSec) {
                                                    System.currentTimeMillis() + (remainingSec * 1000L)
                                                }
                                                val endTimeStr = remember(endTimeMs) {
                                                    val df = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                                    "Ends at " + df.format(java.util.Date(endTimeMs))
                                                }
                                                Text(
                                                    text = endTimeStr,
                                                    fontSize = 10.5.sp,
                                                    color = ZenTextMuted
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(32.dp))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isOverrun) currentTheme.overrunColor.copy(alpha = 0.15f)
                                                else currentTheme.primary.copy(alpha = 0.18f)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isPaused -> "PAUSED"
                                                isOverrun -> "EXTENDED TIME"
                                                config.isOpenEnded -> "OPEN PRACTICE"
                                                else -> "MEDITATING"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp,
                                            color = if (isOverrun) currentTheme.overrunColor else currentTheme.primaryLight
                                        )
                                    }

                                     val isCountDownActive = !config.isOpenEnded && !isOverrun && remainingSec > 0
                                     Spacer(modifier = Modifier.height(10.dp))
                                     Row(
                                         horizontalArrangement = Arrangement.spacedBy(8.dp),
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         if (isCountDownActive) {
                                             OutlinedButton(
                                                 onClick = { viewModel.adjustSessionTime(-300) },
                                                 modifier = Modifier.height(32.dp),
                                                 contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                 shape = RoundedCornerShape(16.dp),
                                                 colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
                                                 border = BorderStroke(1.dp, ZenBorder)
                                             ) {
                                                 Text("-5m", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                             }
                                             OutlinedButton(
                                                 onClick = { viewModel.adjustSessionTime(-60) },
                                                 modifier = Modifier.height(32.dp),
                                                 contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                 shape = RoundedCornerShape(16.dp),
                                                 colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
                                                 border = BorderStroke(1.dp, ZenBorder)
                                             ) {
                                                 Text("-1m", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                             }
                                         }
                                         OutlinedButton(
                                             onClick = { viewModel.adjustSessionTime(60) },
                                             modifier = Modifier.height(32.dp),
                                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                             shape = RoundedCornerShape(16.dp),
                                             colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                             border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.5f))
                                         ) {
                                             Text("+1m", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                         }
                                         OutlinedButton(
                                             onClick = { viewModel.adjustSessionTime(300) },
                                             modifier = Modifier.height(32.dp),
                                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                             shape = RoundedCornerShape(16.dp),
                                             colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                             border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.5f))
                                         ) {
                                             Text("+5m", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                         }
                                     }

                                     if (isCountDownActive) {
                                         Spacer(modifier = Modifier.height(8.dp))
                                         val isOverrunOn = config.isOverrunEnabled
                                         Box(
                                             modifier = Modifier
                                                 .clip(RoundedCornerShape(14.dp))
                                                 .background(
                                                     if (isOverrunOn) currentTheme.primary.copy(alpha = 0.16f)
                                                     else ZenSurface.copy(alpha = 0.6f)
                                                 )
                                                 .border(
                                                     1.dp,
                                                     if (isOverrunOn) currentTheme.primary.copy(alpha = 0.6f)
                                                     else ZenBorder.copy(alpha = 0.8f),
                                                     RoundedCornerShape(14.dp)
                                                 )
                                                 .clickable {
                                                     leavesBurstTrigger++
                                                     viewModel.toggleOverrun(!isOverrunOn)
                                                 }
                                                 .padding(horizontal = 10.dp, vertical = 4.dp)
                                         ) {
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.Center
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.AllInclusive,
                                                     contentDescription = "Toggle Continue Past Target Time",
                                                     tint = if (isOverrunOn) currentTheme.primaryLight else ZenTextMuted,
                                                     modifier = Modifier.size(13.dp)
                                                 )
                                                 Spacer(modifier = Modifier.width(5.dp))
                                                 Text(
                                                     text = if (isOverrunOn) "Continue Past Target: ON" else "Continue Past Target: OFF",
                                                     fontSize = 10.5.sp,
                                                     fontWeight = if (isOverrunOn) FontWeight.SemiBold else FontWeight.Medium,
                                                     color = if (isOverrunOn) currentTheme.primaryLight else ZenTextSecondary
                                                 )
                                             }
                                         }
                                     }

                                     Spacer(modifier = Modifier.height(if (isCountDownActive) 10.dp else 14.dp))

                                    // Landscape Controls with integrated DND
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (dndManager.isDndAccessGranted) {
                                                    val next = !isDndActive
                                                    isDndActive = next
                                                    viewModel.toggleAutoDnd(next)
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
                                                    if (isDndActive) currentTheme.primary.copy(alpha = 0.22f)
                                                    else ZenSurface.copy(alpha = 0.70f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isDndActive) currentTheme.primaryLight.copy(alpha = 0.6f) else ZenBorder,
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DoNotDisturb,
                                                contentDescription = "Toggle DND",
                                                tint = if (isDndActive) currentTheme.primaryLight else ZenTextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                leavesBurstTrigger++
                                                if (isPaused) viewModel.resumeSession() else viewModel.pauseSession()
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(ZenSurface)
                                        ) {
                                            Icon(
                                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = if (isPaused) "Resume" else "Pause",
                                                tint = currentTheme.primaryLight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.finishSession() },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(24.dp)),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOverrun) currentTheme.overrunColor else currentTheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Complete",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Portrait Rich Interactive Session View
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Header Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isOverrun) currentTheme.overrunColor.copy(alpha = 0.15f)
                                            else currentTheme.primary.copy(alpha = 0.18f)
                                        )
                                        .padding(
                                            horizontal = (14.dp * dynamicScale).coerceIn(10.dp, 16.dp),
                                            vertical = (4.dp * dynamicScale).coerceIn(2.dp, 6.dp)
                                        )
                                ) {
                                    Text(
                                        text = when {
                                            isPaused -> "PAUSED"
                                            isOverrun -> "EXTENDED TIME"
                                            config.isOpenEnded -> "OPEN PRACTICE"
                                            else -> "MEDITATING"
                                        },
                                        fontSize = adaptiveSp(13f, 1.25f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (2f * textScale).sp,
                                        color = if (isOverrun) currentTheme.overrunColor else currentTheme.primaryLight
                                    )
                                }

                                Spacer(modifier = Modifier.height((20.dp * dynamicScale).coerceIn(6.dp, 32.dp)))

                                // Circular Progress Display
                                Box(
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        val pos = coords.positionInRoot()
                                        val w   = coords.size.width.toFloat()
                                        val h   = coords.size.height.toFloat()
                                        val cx  = pos.x + w / 2f
                                        val cy  = pos.y + h / 2f
                                        if (rootWidthPx > 1f && rootHeightPx > 1f) {
                                            timerCenterXRatio = (cx / rootWidthPx).coerceIn(0.1f, 0.9f)
                                            timerCenterYRatio = (cy / rootHeightPx).coerceIn(0.1f, 0.9f)
                                            timerRadiusRatio  = (minOf(w, h) / 2f / rootHeightPx).coerceIn(0.05f, 0.45f)
                                        }
                                    }
                                ) {
                                    CircularTimerProgress(
                                        progress = progress,
                                        isOverrun = isOverrun,
                                        isPaused = isPaused,
                                        size = timerSize,
                                        strokeWidth = (9.dp * dynamicScale).coerceIn(7.dp, 10.dp),
                                        intervalMinutes = config.mindfulnessIntervalMinutes,
                                        totalDurationMinutes = currentTargetMinutes
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            if (isOverrun) {
                                                val overrunTime = elapsedSec - sessionTotalSec
                                                Text(
                                                    text = "+${NotificationHelper.formatSeconds(overrunTime.coerceAtLeast(0))}",
                                                    fontSize = ((timerSize.value * 0.23f).coerceIn(52f, 68f) * digitFontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = currentTheme.overrunColor
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Total: ${NotificationHelper.formatSeconds(elapsedSec)}",
                                                    fontSize = adaptiveSp(13.5f, 1.22f),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ZenTextSecondary
                                                )
                                            } else if (config.isOpenEnded) {
                                                Text(
                                                    text = NotificationHelper.formatSeconds(elapsedSec),
                                                    fontSize = ((timerSize.value * 0.25f).coerceIn(56f, 72f) * digitFontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Elapsed Time",
                                                    fontSize = adaptiveSp(13.5f, 1.22f),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ZenTextSecondary
                                                )
                                            } else {
                                                Text(
                                                    text = NotificationHelper.formatSeconds(remainingSec),
                                                    fontSize = ((timerSize.value * 0.25f).coerceIn(56f, 72f) * digitFontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = (-1.5).sp,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Elapsed: ${NotificationHelper.formatSeconds(elapsedSec)}",
                                                    fontSize = adaptiveSp(13.5f, 1.22f),
                                                    fontWeight = FontWeight.Medium,
                                                    color = ZenTextSecondary
                                                )
                                                val endTimeMs = remember(remainingSec) {
                                                    System.currentTimeMillis() + (remainingSec * 1000L)
                                                }
                                                val endTimeStr = remember(endTimeMs) {
                                                    val df = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                                    "Ends at " + df.format(java.util.Date(endTimeMs))
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = endTimeStr,
                                                    fontSize = adaptiveSp(12f, 1.20f),
                                                    color = ZenTextMuted
                                                )
                                            }
                                        }
                                    }
                                }

                                val isCountDownActive = !config.isOpenEnded && !isOverrun && remainingSec > 0
                                Spacer(modifier = Modifier.height((12.dp * dynamicScale).coerceIn(4.dp, 16.dp)))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((8.dp * dynamicScale).coerceIn(4.dp, 10.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCountDownActive) {
                                        OutlinedButton(
                                            onClick = { viewModel.adjustSessionTime(-300) },
                                            modifier = Modifier.height((32.dp * dynamicScale).coerceIn(26.dp, 34.dp)),
                                            contentPadding = PaddingValues(horizontal = (8.dp * dynamicScale).coerceIn(5.dp, 10.dp), vertical = 0.dp),
                                            shape = RoundedCornerShape(17.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
                                            border = BorderStroke(1.dp, ZenBorder)
                                        ) {
                                            Text("-5m", fontSize = adaptiveSp(13f, 1.22f), fontWeight = FontWeight.Medium)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.adjustSessionTime(-60) },
                                            modifier = Modifier.height((32.dp * dynamicScale).coerceIn(26.dp, 34.dp)),
                                            contentPadding = PaddingValues(horizontal = (8.dp * dynamicScale).coerceIn(5.dp, 10.dp), vertical = 0.dp),
                                            shape = RoundedCornerShape(17.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
                                            border = BorderStroke(1.dp, ZenBorder)
                                        ) {
                                            Text("-1m", fontSize = adaptiveSp(13f, 1.22f), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.adjustSessionTime(60) },
                                        modifier = Modifier.height((32.dp * dynamicScale).coerceIn(26.dp, 34.dp)),
                                        contentPadding = PaddingValues(horizontal = (8.dp * dynamicScale).coerceIn(5.dp, 10.dp), vertical = 0.dp),
                                        shape = RoundedCornerShape(17.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                        border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        Text("+1m", fontSize = adaptiveSp(13f, 1.22f), fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.adjustSessionTime(300) },
                                        modifier = Modifier.height((32.dp * dynamicScale).coerceIn(26.dp, 34.dp)),
                                        contentPadding = PaddingValues(horizontal = (8.dp * dynamicScale).coerceIn(5.dp, 10.dp), vertical = 0.dp),
                                        shape = RoundedCornerShape(17.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                        border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        Text("+5m", fontSize = adaptiveSp(13f, 1.22f), fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (isCountDownActive) {
                                    Spacer(modifier = Modifier.height((8.dp * dynamicScale).coerceIn(3.dp, 12.dp)))
                                    val isOverrunOn = config.isOverrunEnabled
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isOverrunOn) currentTheme.primary.copy(alpha = 0.16f)
                                                else ZenSurface.copy(alpha = 0.6f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isOverrunOn) currentTheme.primary.copy(alpha = 0.6f)
                                                else ZenBorder.copy(alpha = 0.8f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                leavesBurstTrigger++
                                                viewModel.toggleOverrun(!isOverrunOn)
                                            }
                                            .padding(
                                                horizontal = (12.dp * dynamicScale).coerceIn(8.dp, 14.dp),
                                                vertical = (4.dp * dynamicScale).coerceIn(2.dp, 6.dp)
                                            )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AllInclusive,
                                                contentDescription = "Toggle Continue Past Target Time",
                                                tint = if (isOverrunOn) currentTheme.primaryLight else ZenTextMuted,
                                                modifier = Modifier.size((13.dp * dynamicScale).coerceIn(11.dp, 15.dp))
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isOverrunOn) "Continue Past Target: ON" else "Continue Past Target: OFF",
                                                fontSize = adaptiveSp(12f, 1.20f),
                                                fontWeight = if (isOverrunOn) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (isOverrunOn) currentTheme.primaryLight else ZenTextSecondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(if (isCountDownActive) (16.dp * dynamicScale).coerceIn(6.dp, 24.dp) else (20.dp * dynamicScale).coerceIn(8.dp, 28.dp)))

                                // Action Controls with integrated DND
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((12.dp * dynamicScale).coerceIn(8.dp, 16.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Subtle DND Mode Quick-Toggle
                                    IconButton(
                                        onClick = {
                                            if (dndManager.isDndAccessGranted) {
                                                val next = !isDndActive
                                                isDndActive = next
                                                viewModel.toggleAutoDnd(next)
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
                                            .size((46.dp * dynamicScale).coerceIn(36.dp, 52.dp))
                                            .clip(CircleShape)
                                            .background(
                                                if (isDndActive) currentTheme.primary.copy(alpha = 0.22f)
                                                else ZenSurface.copy(alpha = 0.70f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isDndActive) currentTheme.primaryLight.copy(alpha = 0.6f) else ZenBorder,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DoNotDisturb,
                                            contentDescription = "Toggle DND",
                                            tint = if (isDndActive) currentTheme.primaryLight else ZenTextMuted,
                                            modifier = Modifier.size((18.dp * dynamicScale).coerceIn(15.dp, 22.dp))
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            leavesBurstTrigger++
                                            if (isPaused) viewModel.resumeSession() else viewModel.pauseSession()
                                        },
                                        modifier = Modifier
                                            .size((56.dp * dynamicScale).coerceIn(44.dp, 64.dp))
                                            .clip(CircleShape)
                                            .background(ZenSurface)
                                    ) {
                                        Icon(
                                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = if (isPaused) "Resume" else "Pause",
                                            tint = currentTheme.primaryLight,
                                            modifier = Modifier.size((24.dp * dynamicScale).coerceIn(18.dp, 28.dp))
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.finishSession() },
                                        modifier = Modifier
                                            .height((48.dp * dynamicScale).coerceIn(38.dp, 56.dp))
                                            .clip(RoundedCornerShape(24.dp)),
                                        contentPadding = PaddingValues(horizontal = (16.dp * dynamicScale).coerceIn(12.dp, 20.dp), vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isOverrun) currentTheme.overrunColor else currentTheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size((18.dp * dynamicScale).coerceIn(14.dp, 20.dp))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Complete",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = adaptiveSp(15f, 1.20f),
                                                maxLines = 1,
                                                softWrap = false,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is TimerStatus.Completed -> {
                        val actualSec = status.actualDurationSec

                        if (isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                CircularTimerProgress(
                                    progress = 1.0f,
                                    isOverrun = false,
                                    isPaused = false,
                                    size = 210.dp,
                                    strokeWidth = 8.dp,
                                    intervalMinutes = 0,
                                    totalDurationMinutes = config.targetDurationMinutes
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = NotificationHelper.formatSeconds(actualSec),
                                            fontSize = 38.sp,
                                            fontWeight = FontWeight.Light,
                                            color = currentTheme.primaryLight
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Completed 🪷",
                                            fontSize = 12.sp,
                                            color = ZenTextSecondary
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(currentTheme.primary.copy(alpha = 0.20f))
                                            .border(1.dp, currentTheme.primaryLight.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "SESSION COMPLETE",
                                            fontSize = 12.sp,
                                            letterSpacing = 2.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = currentTheme.primaryLight
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = {
                                                if (config.showSessionOverview) {
                                                    onSessionCompleted()
                                                } else {
                                                    if (!isBlowingAway) {
                                                        pendingExitAction = {
                                                            MeditationTimerService.resetStatus()
                                                            onSessionCancelled()
                                                        }
                                                        isBlowingAway = true
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(24.dp)),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = currentTheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (config.showSessionOverview) "Save & Reflect" else "Done",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.continueSession(
                                                    initialElapsedSec = actualSec,
                                                    initialStartTimeMs = status.startTimeEpochMs
                                                )
                                            },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(24.dp)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                            border = BorderStroke(1.dp, currentTheme.primary)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Keep Meditating", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(currentTheme.primary.copy(alpha = 0.20f))
                                        .border(1.2.dp, currentTheme.primaryLight.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "SESSION COMPLETE",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        color = currentTheme.primaryLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(30.dp))

                                Box(
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        val pos = coords.positionInRoot()
                                        val w   = coords.size.width.toFloat()
                                        val h   = coords.size.height.toFloat()
                                        val cx  = pos.x + w / 2f
                                        val cy  = pos.y + h / 2f
                                        if (rootWidthPx > 1f && rootHeightPx > 1f) {
                                            timerCenterXRatio = (cx / rootWidthPx).coerceIn(0.1f, 0.9f)
                                            timerCenterYRatio = (cy / rootHeightPx).coerceIn(0.1f, 0.9f)
                                            timerRadiusRatio  = (minOf(w, h) / 2f / rootHeightPx).coerceIn(0.05f, 0.45f)
                                        }
                                    }
                                ) {
                                    CircularTimerProgress(
                                        progress = 1.0f,
                                        isOverrun = false,
                                        isPaused = false,
                                        size = 290.dp,
                                        strokeWidth = 10.dp,
                                        intervalMinutes = 0,
                                        totalDurationMinutes = config.targetDurationMinutes
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = NotificationHelper.formatSeconds(actualSec),
                                                fontSize = 56.sp,
                                                fontWeight = FontWeight.Light,
                                                color = currentTheme.primaryLight
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Total Practice Time",
                                                fontSize = 13.sp,
                                                color = ZenTextSecondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Quick Add Time Chips on Completed Screen
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(1, 5, 10).forEach { mins ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(ZenSurface.copy(alpha = 0.85f))
                                                .border(1.dp, currentTheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                                .clickable {
                                                    viewModel.continueSessionWithTarget(
                                                        targetSec = actualSec + (mins * 60),
                                                        initialElapsedSec = actualSec,
                                                        initialStartTimeMs = status.startTimeEpochMs
                                                    )
                                                }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "+${mins}m",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = currentTheme.primaryLight
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (config.showSessionOverview) {
                                                onSessionCompleted()
                                            } else {
                                                if (!isBlowingAway) {
                                                    pendingExitAction = {
                                                        MeditationTimerService.resetStatus()
                                                        onSessionCancelled()
                                                    }
                                                    isBlowingAway = true
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(26.dp)),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = currentTheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (config.showSessionOverview) "Save & Reflect" else "Done",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.5.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.continueSession(
                                                initialElapsedSec = actualSec,
                                                initialStartTimeMs = status.startTimeEpochMs
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(25.dp)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                        border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.7f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Keep Meditating",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.5.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Text(text = "Initializing…", color = ZenTextMuted)
                    }
                }
            }
        }
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

