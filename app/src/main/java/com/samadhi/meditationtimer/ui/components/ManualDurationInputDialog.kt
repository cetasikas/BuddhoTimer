package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import kotlinx.coroutines.delay

@Composable
fun ManualDurationInputDialog(
    initialMinutes: Int,
    minMinutes: Int = 1,
    maxMinutes: Int = 180,
    title: String = "Set Custom Duration",
    subtitle: String = "Enter practice duration in minutes",
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialMinutes.toString(),
                selection = TextRange(0, initialMinutes.toString().length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val currentTheme = LocalZenTheme.current

    val currentInt = textValue.text.toIntOrNull()
    val isValid = currentInt != null && currentInt in minMinutes..maxMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = currentTheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = currentTheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZenTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large centered numeric input
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.text.length <= 3 && input.text.all { it.isDigit() }) {
                            textValue = input
                        }
                    },
                    modifier = Modifier
                        .width(148.dp)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primary
                    ),
                    suffix = {
                        Text("min", color = ZenTextSecondary, fontSize = 14.sp)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid && currentInt != null) {
                                onConfirm(currentInt)
                                onDismiss()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentTheme.primary,
                        unfocusedBorderColor = currentTheme.border,
                        focusedContainerColor = currentTheme.surface,
                        unfocusedContainerColor = currentTheme.surface,
                        cursorColor = currentTheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isValid) "Range: $minMinutes to $maxMinutes minutes" else "Please enter between $minMinutes and $maxMinutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isValid) ZenTextMuted else Color(0xFFEF5350)
                )

                // Quick Increment Pills (-5, +5, +15)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(-5, 5, 15).forEach { step ->
                        val label = if (step > 0) "+$step" else "$step"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(currentTheme.surface)
                                .border(1.dp, currentTheme.border, RoundedCornerShape(12.dp))
                                .clickable {
                                    val currentOrBase = textValue.text.toIntOrNull() ?: initialMinutes
                                    val newVal = (currentOrBase + step).coerceIn(minMinutes, maxMinutes)
                                    textValue = TextFieldValue(newVal.toString(), TextRange(newVal.toString().length))
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = currentTheme.textSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && currentInt != null) {
                        onConfirm(currentInt)
                        onDismiss()
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentTheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Set Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel", color = ZenTextSecondary)
            }
        }
    )

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }
}
