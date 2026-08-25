package com.samadhi.meditationtimer.ui.screens.summary

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.model.TimerStatus
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.ui.components.MarkdownToolbar
import com.samadhi.meditationtimer.ui.components.MoodSelector
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenAmberDark
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenOverrunBlue
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect

import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTealLight

@Composable
fun SessionSummaryScreen(
    completedStatus: TimerStatus.Completed?,
    onSaveSession: (MeditationSession) -> Unit,
    onContinueMeditatingCombined: (initialElapsedSec: Int, initialStartTimeMs: Long) -> Unit = { _, _ -> },
    onContinueMeditatingFresh: () -> Unit = {},
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (completedStatus == null) {
        onDiscard()
        return
    }

    val currentTheme = com.samadhi.meditationtimer.ui.theme.LocalZenTheme.current
    var selectedMood by remember { mutableIntStateOf(3) }
    var notesValue by remember { mutableStateOf(TextFieldValue("")) }
    var currentWisdomQuote by remember {
        mutableStateOf(com.samadhi.meditationtimer.data.wisdom.MeditationGuides.wisdomQuotes.random())
    }

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        var isBlowingAway by remember { mutableStateOf(false) }
        var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }
        var showContinueDialog by remember { mutableStateOf(false) }

        // Leaves Effect: Forest floor stacking for meditation sessions; ambient drifting leaves for Box Breathing
        val isMeditationSession = !completedStatus.isBoxBreathing
        val accumulatedLeafCount = if (isMeditationSession) (completedStatus.actualDurationSec / 12).coerceIn(0, 160) else 0
        FallingLeavesEffect(
            leafCount = if (isMeditationSession) 14 else 12,
            ambientAlpha = if (isMeditationSession) 0.35f else 0.28f,
            pileUpAtBottom = isMeditationSession,
            initialSettledLeavesCount = accumulatedLeafCount,
            elapsedMeditationSeconds = if (isMeditationSession) completedStatus.actualDurationSec else 0,
            isBlowingAway = isBlowingAway,
            onBlowAwayComplete = {
                pendingExitAction?.invoke()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon & Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = currentTheme.primaryLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Practice Complete",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = ZenTextPrimary
                )
            }
            Text(
                text = "May the peace of this session carry throughout your day",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = ZenTextSecondary
            )

            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.80f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Actual Time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = NotificationHelper.formatSeconds(completedStatus.actualDurationSec),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryLight
                        )
                        Text(
                            text = "Total Practice",
                            fontSize = 11.sp,
                            color = ZenTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(ZenBorder)
                    )

                    // Overrun or Target
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (completedStatus.overrunSec > 0) {
                            Text(
                                text = "+${NotificationHelper.formatSeconds(completedStatus.overrunSec)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.primaryLight
                            )
                            Text(
                                text = "Extended Time",
                                fontSize = 11.sp,
                                color = currentTheme.primary
                            )
                        } else {
                            Text(
                                text = if (completedStatus.targetDurationSec > 0) "${completedStatus.targetDurationSec / 60}m" else "Open",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZenTextPrimary
                            )
                            Text(
                                text = "Target Duration",
                                fontSize = 11.sp,
                                color = ZenTextSecondary
                            )
                        }
                    }
                }
            }

            // Buddhist Wisdom Reflection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        val quotes = com.samadhi.meditationtimer.data.wisdom.MeditationGuides.wisdomQuotes
                        currentWisdomQuote = quotes.random()
                    },
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.65f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(currentTheme.primary.copy(alpha = 0.35f))),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪷", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "WORDS OF WISDOM",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = currentTheme.primaryLight
                            )
                        }

                        Text(
                            text = "Tap to shuffle",
                            fontSize = 9.5.sp,
                            color = ZenTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "“${currentWisdomQuote.quote}”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 16.sp
                        ),
                        color = ZenTextPrimary
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "— ${currentWisdomQuote.author} (${currentWisdomQuote.sourceOrTradition})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentTheme.primaryLight.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Mood / Depth Rating Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "STATE OF MIND & CLARITY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = currentTheme.primaryLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                MoodSelector(
                    selectedMood = selectedMood,
                    onMoodSelected = { selectedMood = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Journal & Meditation Reflection Notes
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MEDITATION REFLECTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = currentTheme.primaryLight
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.80f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        MarkdownToolbar(
                            textFieldValue = notesValue,
                            onValueChange = { notesValue = it }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        BasicTextField(
                            value = notesValue,
                            onValueChange = { notesValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            textStyle = TextStyle(
                                color = ZenTextPrimary,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp
                            ),
                            cursorBrush = SolidColor(currentTheme.primaryLight),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (notesValue.text.isEmpty()) {
                                        Text(
                                            text = "Record insights, stillness, challenges, or mental state...",
                                            color = ZenTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            // Actions: Continue Meditating, Save Reflection & Discard
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action: Continue Meditating (Triggers choice dialog)
                Button(
                    onClick = {
                        if (!isBlowingAway) {
                            showContinueDialog = true
                        }
                    },
                    enabled = !isBlowingAway,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentTheme.primary,
                        contentColor = ZenObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ZenObsidian,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Continue Meditating",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Save Reflection (Complete & Return to Home)
                OutlinedButton(
                    onClick = {
                        if (!isBlowingAway) {
                            val session = MeditationSession(
                                startTimeEpochMs = completedStatus.startTimeEpochMs,
                                completedTimeEpochMs = completedStatus.completedTimeEpochMs,
                                targetDurationSec = completedStatus.targetDurationSec,
                                actualDurationSec = completedStatus.actualDurationSec,
                                overrunSec = completedStatus.overrunSec,
                                rating = selectedMood,
                                notesMarkdown = notesValue.text,
                                completedSuccessfully = true
                            )
                            pendingExitAction = { onSaveSession(session) }
                            isBlowingAway = true
                        }
                    },
                    enabled = !isBlowingAway,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                    border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = currentTheme.primaryLight,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Reflection & Done",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (!isBlowingAway) {
                            pendingExitAction = { onDiscard() }
                            isBlowingAway = true
                        }
                    },
                    enabled = !isBlowingAway,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
                    border = BorderStroke(1.dp, ZenBorder)
                ) {
                    Text(
                        text = "Discard",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (showContinueDialog) {
            AlertDialog(
                onDismissRequest = { showContinueDialog = false },
                title = {
                    Text(
                        text = "Continue Practice",
                        color = currentTheme.primaryLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "How would you like to continue your meditation session?",
                        color = ZenTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showContinueDialog = false
                            pendingExitAction = {
                                onContinueMeditatingCombined(
                                    completedStatus.actualDurationSec,
                                    completedStatus.startTimeEpochMs
                                )
                            }
                            isBlowingAway = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentTheme.primary,
                            contentColor = ZenObsidian
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to This Session", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                showContinueDialog = false
                                val session = MeditationSession(
                                    startTimeEpochMs = completedStatus.startTimeEpochMs,
                                    completedTimeEpochMs = completedStatus.completedTimeEpochMs,
                                    targetDurationSec = completedStatus.targetDurationSec,
                                    actualDurationSec = completedStatus.actualDurationSec,
                                    overrunSec = completedStatus.overrunSec,
                                    rating = selectedMood,
                                    notesMarkdown = notesValue.text,
                                    completedSuccessfully = true
                                )
                                onSaveSession(session)
                                pendingExitAction = { onContinueMeditatingFresh() }
                                isBlowingAway = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                            border = BorderStroke(1.dp, currentTheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text("Save & Start Fresh")
                        }

                        OutlinedButton(
                            onClick = { showContinueDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
                            border = BorderStroke(1.dp, ZenBorder)
                        ) {
                            Text("Cancel")
                        }
                    }
                },
                containerColor = ZenSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
