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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.TimerStatus
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

    LaunchedEffect(timerStatus) {
        if (timerStatus is TimerStatus.Completed) {
            onSessionCompleted()
        } else if (timerStatus is TimerStatus.Idle) {
            onSessionCancelled()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (config.tapToBlackoutEnabled) {
                    isScreenBlackedOut = !isScreenBlackedOut
                }
            }
    ) {
        if (isScreenBlackedOut) {
            // Pure AMOLED Stealth Blackout (100% Black)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "•",
                    fontSize = 14.sp,
                    color = Color(0xFF222222),
                    modifier = Modifier.alpha(0.3f)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Top Navigation Controls (Clean Minimal Cancel Action)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.cancelSession()
                            onSessionCancelled()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ZenSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Practice",
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(20.dp)
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
                                    text = "${status.remainingSeconds}",
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.ExtraLight,
                                    color = currentTheme.primaryLight
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

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

                        val totalTarget = config.targetDurationSeconds
                        val progress = if (totalTarget > 0) {
                            (totalTarget - remainingSec).toFloat() / totalTarget.toFloat()
                        } else 1.0f

                        // Standard Rich Interactive Session View
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
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isPaused -> "PAUSED"
                                        isOverrun -> "EXTENDED TIME"
                                        config.isOpenEnded -> "OPEN PRACTICE"
                                        else -> "MEDITATING"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = if (isOverrun) currentTheme.overrunColor else currentTheme.primaryLight
                                )
                            }

                            Spacer(modifier = Modifier.height(36.dp))

                        // Circular Progress Display with Breath Pacing Aura & Interval Notches
                        CircularTimerProgress(
                            progress = progress,
                            isOverrun = isOverrun,
                            isPaused = isPaused,
                            size = 300.dp,
                            strokeWidth = 10.dp,
                            intervalMinutes = config.mindfulnessIntervalMinutes,
                            totalDurationMinutes = config.targetDurationMinutes
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isOverrun) {
                                    val overrunTime = elapsedSec - totalTarget
                                    Text(
                                        text = "+${NotificationHelper.formatSeconds(overrunTime)}",
                                        fontSize = 52.sp,
                                        fontWeight = FontWeight.Light,
                                        color = currentTheme.overrunColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Total: ${NotificationHelper.formatSeconds(elapsedSec)}",
                                        fontSize = 14.sp,
                                        color = ZenTextSecondary
                                    )
                                } else if (config.isOpenEnded) {
                                    Text(
                                        text = NotificationHelper.formatSeconds(elapsedSec),
                                        fontSize = 62.sp,
                                        fontWeight = FontWeight.Light,
                                        color = ZenTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Elapsed Time",
                                        fontSize = 13.sp,
                                        color = ZenTextSecondary
                                    )
                                } else {
                                    Text(
                                        text = NotificationHelper.formatSeconds(remainingSec),
                                        fontSize = 64.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = (-1).sp,
                                        color = ZenTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Elapsed: ${NotificationHelper.formatSeconds(elapsedSec)}",
                                        fontSize = 13.sp,
                                        color = ZenTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // Action Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pause / Resume Button
                            IconButton(
                                onClick = {
                                    leavesBurstTrigger++
                                    if (isPaused) viewModel.resumeSession() else viewModel.pauseSession()
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(ZenSurface)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    tint = currentTheme.primaryLight,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Complete / Finish Button
                            Button(
                                onClick = { viewModel.finishSession() },
                                modifier = Modifier
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(28.dp)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverrun) currentTheme.overrunColor else currentTheme.primary,
                                    contentColor = ZenObsidian
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Complete",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Fallback / Loading
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

