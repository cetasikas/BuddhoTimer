package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

/**
 * Shared mood + reflection capture block used both as the post-session
 * reflection moment and inside the journal entry editor.
 */
@Composable
fun SessionReflectionEditor(
    selectedMood: Int,
    onMoodSelected: (Int) -> Unit,
    notesValue: TextFieldValue,
    onNotesValueChange: (TextFieldValue) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    saveLabel: String = "Save Reflection",
    discardLabel: String = "Discard",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        // Mood / depth selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "STATE OF MIND & CLARITY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = currentTheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            MoodSelector(
                selectedMood = selectedMood,
                onMoodSelected = onMoodSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Journal & reflection notes
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "MEDITATION REFLECTION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = currentTheme.primary
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
                        onValueChange = onNotesValueChange
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    BasicTextField(
                        value = notesValue,
                        onValueChange = onNotesValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        enabled = enabled,
                        textStyle = TextStyle(
                            color = ZenTextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        ),
                        cursorBrush = SolidColor(currentTheme.primaryLight),
                        decorationBox = { innerTextField ->
                            androidx.compose.foundation.layout.Box {
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

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentTheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = saveLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            OutlinedButton(
                onClick = onDiscard,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
                border = BorderStroke(1.dp, ZenBorder)
            ) {
                Text(discardLabel)
            }
        }
    }
}
