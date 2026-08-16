package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenSurface

@Composable
fun MarkdownToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ZenSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {
                onValueChange(wrapOrInsert(textFieldValue, "## ", ""))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Title,
                contentDescription = "Heading",
                tint = currentTheme.primaryLight,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = {
                onValueChange(wrapOrInsert(textFieldValue, "**", "**"))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatBold,
                contentDescription = "Bold",
                tint = currentTheme.primaryLight,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = {
                onValueChange(wrapOrInsert(textFieldValue, "*", "*"))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                tint = currentTheme.primaryLight,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = {
                onValueChange(wrapOrInsert(textFieldValue, "> ", ""))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = "Quote",
                tint = currentTheme.primaryLight,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = {
                onValueChange(wrapOrInsert(textFieldValue, "- ", ""))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatListBulleted,
                contentDescription = "List",
                tint = currentTheme.primaryLight,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = {
                onValueChange(wrapOrInsert(textFieldValue, "`", "`"))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "Code",
                tint = currentTheme.primaryLight,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun wrapOrInsert(
    value: TextFieldValue,
    prefix: String,
    suffix: String
): TextFieldValue {
    val text = value.text
    val selection = value.selection

    val start = selection.start
    val end = selection.end

    val selectedText = text.substring(start, end)
    val newText = text.replaceRange(start, end, "$prefix$selectedText$suffix")
    val newCursorPos = if (start == end) {
        start + prefix.length
    } else {
        start + prefix.length + selectedText.length + suffix.length
    }

    return TextFieldValue(
        text = newText,
        selection = TextRange(newCursorPos)
    )
}
