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
    var isOledZenMode by remember { mutableStateOf(false) }
    var leavesBurstTrigger by remember { mutableIntStateOf(0) }

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
            .background(if (isOledZenMode) Color.Black else ZenObsidian)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isOledZenMode) {
                    isOledZenMode = false
                }
            }
    ) {
        if (!isOledZenMode) {
            // Dynamic Moonlit Backdrop
            Image(
                painter = painterResource(id = currentTheme.backgroundDrawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.16f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF060911).copy(alpha = 0.85f),
                                Color(0xFF0A101D).copy(alpha = 0.70f),
                                Color(0xFF060911).copy(alpha = 0.92f)
                            )
                        )
                    )
            )

        val currentProgress = when (val status = timerStatus) {
            is TimerStatus.Running -> {
                if (status.isOverrun) 1.0f
                else if (config.targetDurationSeconds > 0) (status.elapsedSeconds.toFloat() / config.targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
                else 0.5f
            }
            is TimerStatus.Paused -> {
                if (status.isOverrun) 1.0f
                else if (config.targetDurationSeconds > 0) (status.elapsedSeconds.toFloat() / config.targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
                else 0.5f
            }
            else -> 0f
        }

        if (config.showLeavesDuringMeditation) {
            FallingLeavesEffect(
                leafCount = config.meditationLeafCount,
                triggerBurst = leavesBurstTrigger,
                ambientAlpha = 0.32f,
                isGlowing = true,
                pileUpAtBottom = true,
                sessionProgressRatio = currentProgress
            )
        }
    }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
        // Top Navigation Controls (Cancel & OLED Dim Mode)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // OLED Mode Toggle
            IconButton(
                onClick = { isOledZenMode = !isOledZenMode },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isOledZenMode) Color(0xFF111111) else ZenSurface)
            ) {
                Icon(
                    imageVector = if (isOledZenMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isOledZenMode) "Exit OLED Mode" else "OLED Deep Black Mode",
                    tint = if (isOledZenMode) ZenTealLight else ZenTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Cancel Action
            IconButton(
                onClick = {
                    viewModel.cancelSession()
                    onSessionCancelled()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isOledZenMode) Color(0xFF111111) else ZenSurface)
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

                if (isOledZenMode) {
                    // Minimalist OLED Deep Black Ambient Clock
                    val pulseTransition = rememberInfiniteTransition(label = "oled_pulse")
                    val pulseAlpha by pulseTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.alpha(if (isPaused) 0.5f else pulseAlpha)
                    ) {
                        Text(
                            text = when {
                                isPaused -> "PAUSED"
                                isOverrun -> "+${NotificationHelper.formatSeconds(elapsedSec - totalTarget)}"
                                config.isOpenEnded -> NotificationHelper.formatSeconds(elapsedSec)
                                else -> NotificationHelper.formatSeconds(remainingSec)
                            },
                            fontSize = 84.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = if (isOverrun) currentTheme.overrunColor.copy(alpha = 0.9f) else currentTheme.primaryLight.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isOverrun) "Samadhi Overrun • Tap anywhere to view" else "Tap anywhere to wake controls",
                            fontSize = 12.sp,
                            color = Color(0xFF555555)
                        )
                    }
                } else {
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
                                    isOverrun -> "SAMADHI OVERRUN"
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

                        // Circular Progress Display
                        CircularTimerProgress(
                            progress = progress,
                            isOverrun = isOverrun,
                            isPaused = isPaused,
                            size = 300.dp,
                            strokeWidth = 10.dp
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
            }

            else -> {
                // Fallback / Loading
                Text(text = "Initializing…", color = ZenTextMuted)
            }
        }
        }
    }
}

