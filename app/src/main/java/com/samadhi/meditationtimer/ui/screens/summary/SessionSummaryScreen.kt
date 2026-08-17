package com.samadhi.meditationtimer.ui.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SelfImprovement
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

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        // Dynamic Moonlit Backdrop
        Image(
            painter = painterResource(id = currentTheme.backgroundDrawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.20f
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

        val sessionDurationSec = completedStatus?.actualDurationSec ?: 0
        val sessionLeavesCount = if (sessionDurationSec <= 0) 24 else ((sessionDurationSec / 7).coerceIn(12, 110))

        var isBlowingAway by remember { mutableStateOf(false) }
        var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        FallingLeavesEffect(
            leafCount = 14,
            ambientAlpha = 0.32f,
            isGlowing = true,
            pileUpAtBottom = true,
            initialSettledLeavesCount = sessionLeavesCount,
            isBlowingAway = isBlowingAway,
            onBlowAwayComplete = {
                pendingExitAction?.invoke()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Icon & Header
        Icon(
            imageVector = Icons.Default.SelfImprovement,
            contentDescription = null,
            tint = currentTheme.primaryLight,
            modifier = Modifier.size(42.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Practice Complete",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = ZenTextPrimary
        )
        Text(
            text = "May the peace of this session carry throughout your day",
            style = MaterialTheme.typography.bodyMedium,
            color = ZenTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.80f)),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Actual Time
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = NotificationHelper.formatSeconds(completedStatus.actualDurationSec),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryLight
                    )
                    Text(
                        text = "Total Practice",
                        fontSize = 12.sp,
                        color = ZenTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(ZenBorder)
                )

                // Overrun or Target
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (completedStatus.overrunSec > 0) {
                        Text(
                            text = "+${NotificationHelper.formatSeconds(completedStatus.overrunSec)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryLight
                        )
                        Text(
                            text = "Samadhi Overrun",
                            fontSize = 12.sp,
                            color = currentTheme.primary
                        )
                    } else {
                        Text(
                            text = if (completedStatus.targetDurationSec > 0) "${completedStatus.targetDurationSec / 60}m" else "Open",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZenTextPrimary
                        )
                        Text(
                            text = "Target Duration",
                            fontSize = 12.sp,
                            color = ZenTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mood / Depth Rating Selector
        Text(
            text = "STATE OF MIND & CLARITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = currentTheme.primaryLight,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        MoodSelector(
            selectedMood = selectedMood,
            onMoodSelected = { selectedMood = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Journal & Meditation Reflection Notes
        Text(
            text = "MEDITATION REFLECTION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = currentTheme.primaryLight,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.80f)),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Markdown formatting toolbar
                MarkdownToolbar(
                    textFieldValue = notesValue,
                    onValueChange = { notesValue = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                BasicTextField(
                    value = notesValue,
                    onValueChange = { notesValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    textStyle = TextStyle(
                        color = ZenTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(currentTheme.primaryLight),
                    decorationBox = { innerTextField ->
                        Box {
                            if (notesValue.text.isEmpty()) {
                                Text(
                                    text = "Record insights, stillness, challenges, or mental state...",
                                    color = ZenTextMuted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Actions: Save Reflection & Discard
        Button(
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
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = currentTheme.primary,
                contentColor = Color(0xFF041014)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save Reflection",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(ZenBorder))
        ) {
            Text(text = "Discard", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
