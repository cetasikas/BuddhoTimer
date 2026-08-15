package com.samadhi.meditationtimer.ui.screens.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@Composable
fun ActiveSessionScreen(
    viewModel: TimerViewModel,
    onSessionCompleted: () -> Unit,
    onSessionCancelled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timerStatus by viewModel.timerStatus.collectAsState()
    val config by viewModel.config.collectAsState()

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
            .background(ZenObsidian)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Cancel Action (Top End)
        IconButton(
            onClick = {
                viewModel.cancelSession()
                onSessionCancelled()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
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

        when (val status = timerStatus) {
            is TimerStatus.Preparing -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "PREPARATION",
                        fontSize = 14.sp,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenAmber
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${status.remainingSeconds}",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Light,
                        color = ZenAmber
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Close your eyes & find stillness",
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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Header Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isOverrun) ZenOverrunBlue.copy(alpha = 0.15f)
                                else ZenAmber.copy(alpha = 0.15f)
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
                            color = if (isOverrun) ZenOverrunBlue else ZenAmber
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
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Light,
                                    color = ZenOverrunBlue
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
                                    fontSize = 50.sp,
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
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.Light,
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
                                tint = ZenAmber,
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
                                containerColor = if (isOverrun) ZenOverrunBlue else ZenAmber,
                                contentColor = ZenObsidian
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finish Session",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
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
