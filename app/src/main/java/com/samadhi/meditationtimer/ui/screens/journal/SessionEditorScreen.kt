package com.samadhi.meditationtimer.ui.screens.journal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.model.TimerStatus
import com.samadhi.meditationtimer.data.wisdom.MeditationGuides
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.components.ManualDurationInputDialog
import com.samadhi.meditationtimer.ui.components.SessionReflectionEditor
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What the shared editor should open for. */
sealed interface EditorRequest {
    data class PostSession(val status: TimerStatus.Completed) : EditorRequest
    data class Edit(val session: MeditationSession) : EditorRequest
    object ManualNew : EditorRequest
}

/**
 * One editor for every path that captures a meditation: the celebratory
 * post-timer reflection, editing an existing journal entry, and manually
 * logging a sit performed away from the app.
 */
@Composable
fun SessionEditorScreen(
    request: EditorRequest,
    onSave: (MeditationSession) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    val context = LocalContext.current

    val postStatus = (request as? EditorRequest.PostSession)?.status
    val existing = (request as? EditorRequest.Edit)?.session

    var selectedMood by remember { mutableIntStateOf(existing?.rating ?: if (postStatus != null) 3 else 0) }
    var notesValue by remember {
        mutableStateOf(TextFieldValue(existing?.notesMarkdown ?: ""))
    }
    var currentWisdomQuote by remember { mutableStateOf(MeditationGuides.wisdomQuotes.random()) }

    // Editable details (date / time / duration). Post-session values are derived.
    val initialDurationMin = existing?.let { it.actualDurationSec / 60 }
        ?: postStatus.let { it?.actualDurationSec?.div(60) }
        ?: 10
    var durationMinutes by remember { mutableIntStateOf(initialDurationMin.coerceAtLeast(1)) }
    var completedAtMs by remember {
        mutableLongStateOf(existing?.completedTimeEpochMs ?: postStatus?.completedTimeEpochMs ?: System.currentTimeMillis())
    }
    var showDurationDialog by remember { mutableStateOf(false) }

    val canEditDetails = postStatus == null
    val scrollState = rememberScrollState()

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zoned = Instant.ofEpochMilli(completedAtMs).atZone(ZoneId.systemDefault())

    fun buildSession(): MeditationSession {
        if (postStatus != null) {
            return MeditationSession(
                startTimeEpochMs = postStatus.startTimeEpochMs,
                completedTimeEpochMs = postStatus.completedTimeEpochMs,
                targetDurationSec = postStatus.targetDurationSec,
                actualDurationSec = postStatus.actualDurationSec,
                overrunSec = postStatus.overrunSec,
                rating = selectedMood,
                notesMarkdown = notesValue.text,
                completedSuccessfully = true,
                isManual = false
            )
        }
        val durationSec = durationMinutes * 60
        return if (existing != null) {
            existing.copy(
                completedTimeEpochMs = completedAtMs,
                startTimeEpochMs = completedAtMs - durationSec * 1000L,
                actualDurationSec = durationSec,
                rating = selectedMood,
                notesMarkdown = notesValue.text
            )
        } else {
            MeditationSession(
                startTimeEpochMs = completedAtMs - durationSec * 1000L,
                completedTimeEpochMs = completedAtMs,
                targetDurationSec = durationSec,
                actualDurationSec = durationSec,
                overrunSec = 0,
                rating = selectedMood,
                notesMarkdown = notesValue.text,
                completedSuccessfully = true,
                isManual = true
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        if (postStatus != null) {
            val isMeditationSession = !postStatus.isBoxBreathing
            val accumulatedLeafCount = if (isMeditationSession) (postStatus.actualDurationSec / 12).coerceIn(0, 160) else 0
            FallingLeavesEffect(
                leafCount = if (isMeditationSession) 14 else 12,
                ambientAlpha = if (isMeditationSession) 0.35f else 0.28f,
                pileUpAtBottom = isMeditationSession,
                initialSettledLeavesCount = accumulatedLeafCount,
                elapsedMeditationSeconds = if (isMeditationSession) postStatus.actualDurationSec else 0
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorHeader(postStatus != null, existing != null)

            if (postStatus != null) {
                PostSessionStatsCard(postStatus)
                WisdomCard(currentWisdomQuote) {
                    currentWisdomQuote = MeditationGuides.wisdomQuotes.random()
                }
            } else {
                DetailsCard(
                    dateText = zoned.format(dateFormatter),
                    timeText = zoned.format(timeFormatter),
                    durationMinutes = durationMinutes,
                    editable = canEditDetails,
                    onPickDate = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val updated = zoned.withYear(year).withMonth(month + 1).withDayOfMonth(day)
                                completedAtMs = updated.toInstant().toEpochMilli()
                            },
                            zoned.year,
                            zoned.monthValue - 1,
                            zoned.dayOfMonth
                        ).show()
                    },
                    onPickTime = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val updated = zoned.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                                completedAtMs = updated.toInstant().toEpochMilli()
                            },
                            zoned.hour,
                            zoned.minute,
                            false
                        ).show()
                    },
                    onEditDuration = { showDurationDialog = true }
                )
            }

            SessionReflectionEditor(
                selectedMood = selectedMood,
                onMoodSelected = { selectedMood = it },
                notesValue = notesValue,
                onNotesValueChange = { notesValue = it },
                onSave = { onSave(buildSession()) },
                onDiscard = onDiscard,
                saveLabel = if (postStatus != null) "Save Reflection" else "Save Entry",
                discardLabel = if (postStatus != null) "Discard" else "Cancel"
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDurationDialog) {
        ManualDurationInputDialog(
            initialMinutes = durationMinutes,
            minMinutes = 1,
            maxMinutes = 600,
            title = "Practice Duration",
            subtitle = "How long did you meditate?",
            onDismiss = { showDurationDialog = false },
            onConfirm = {
                durationMinutes = it
                showDurationDialog = false
            }
        )
    }
}

@Composable
private fun EditorHeader(isPostSession: Boolean, isEditing: Boolean) {
    val currentTheme = LocalZenTheme.current
    val title = when {
        isPostSession -> "Practice Complete"
        isEditing -> "Edit Journal Entry"
        else -> "Log a Meditation"
    }
    val subtitle = when {
        isPostSession -> "May the peace of this session carry throughout your day"
        isEditing -> "Update the details and reflection for this sit"
        else -> "Record a sit you completed away from the timer"
    }
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
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
            color = ZenTextPrimary
        )
    }
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = ZenTextSecondary
    )
}

@Composable
private fun PostSessionStatsCard(status: TimerStatus.Completed) {
    val currentTheme = LocalZenTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.80f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = NotificationHelper.formatSeconds(status.actualDurationSec),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.primary
                )
                Text(text = "Total Practice", fontSize = 11.sp, color = ZenTextSecondary)
            }
            Box(modifier = Modifier.width(1.dp).height(28.dp).background(ZenBorder))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (status.overrunSec > 0) {
                    Text(
                        text = "+${NotificationHelper.formatSeconds(status.overrunSec)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.overrunColor
                    )
                    Text(text = "Extended Time", fontSize = 11.sp, color = currentTheme.overrunColor)
                } else {
                    Text(
                        text = if (status.targetDurationSec > 0) "${status.targetDurationSec / 60}m" else "Open",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenTextPrimary
                    )
                    Text(text = "Target Duration", fontSize = 11.sp, color = ZenTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun WisdomCard(quote: com.samadhi.meditationtimer.data.wisdom.WisdomQuote, onShuffle: () -> Unit) {
    val currentTheme = LocalZenTheme.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onShuffle() },
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
                    Text(text = "\uD83E\uDEB7", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WORDS OF WISDOM",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = currentTheme.primary
                    )
                }
                Text(text = "Tap to shuffle", fontSize = 9.5.sp, color = ZenTextMuted)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\u201C${quote.quote}\u201D",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontStyle = FontStyle.Italic, lineHeight = 16.sp),
                color = ZenTextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "\u2014 ${quote.author} (${quote.sourceOrTradition})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = currentTheme.primary
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(
    dateText: String,
    timeText: String,
    durationMinutes: Int,
    editable: Boolean,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onEditDuration: () -> Unit
) {
    val currentTheme = LocalZenTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.80f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = "PRACTICE DETAILS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = currentTheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                icon = Icons.Default.CalendarMonth,
                label = "Date",
                value = dateText,
                enabled = editable,
                onClick = onPickDate
            )
            DetailRow(
                icon = Icons.Default.Schedule,
                label = "Time",
                value = timeText,
                enabled = editable,
                onClick = onPickTime
            )
            DetailRow(
                icon = Icons.Default.Timelapse,
                label = "Duration",
                value = "$durationMinutes min",
                enabled = editable,
                onClick = onEditDuration
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val currentTheme = LocalZenTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = currentTheme.primaryLight,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, fontSize = 12.5.sp, color = ZenTextSecondary, modifier = Modifier.width(80.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ZenTextPrimary)
    }
}
