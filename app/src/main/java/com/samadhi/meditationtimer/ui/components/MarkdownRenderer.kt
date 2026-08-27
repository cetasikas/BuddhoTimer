package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    val lines = markdown.lines()

    Column(modifier = modifier) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trimEnd()

            when {
                line.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("### ")),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = currentTheme.primary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("## ")),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = currentTheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = currentTheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                }
                line.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(currentTheme.surfaceVariant, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .background(currentTheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(line.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = currentTheme.textSecondary
                            ),
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .size(6.dp)
                                .background(currentTheme.primary, CircleShape)
                        )
                        Text(
                            text = parseInlineMarkdown(line.substring(2)),
                            style = MaterialTheme.typography.bodyMedium.copy(color = currentTheme.textPrimary)
                        )
                    }
                }
                line.startsWith("```") -> {
                    val codeContent = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeContent.append(lines[i]).append("\n")
                        i++
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(currentTheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = codeContent.toString().trimEnd(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = currentTheme.textPrimary
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge.copy(color = currentTheme.textPrimary),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            i++
        }
    }
}

/**
 * Parses inline Markdown syntax: **bold**, *italic*, `code`
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var index = 0

    while (index < text.length) {
        if (text.startsWith("**", index)) {
            val endIndex = text.indexOf("**", index + 2)
            if (endIndex != -1) {
                val boldText = text.substring(index + 2, endIndex)
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(boldText)
                builder.pop()
                index = endIndex + 2
                continue
            }
        } else if (text.startsWith("*", index)) {
            val endIndex = text.indexOf("*", index + 1)
            if (endIndex != -1) {
                val italicText = text.substring(index + 1, endIndex)
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(italicText)
                builder.pop()
                index = endIndex + 1
                continue
            }
        } else if (text.startsWith("`", index)) {
            val endIndex = text.indexOf("`", index + 1)
            if (endIndex != -1) {
                val codeText = text.substring(index + 1, endIndex)
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace
                    )
                )
                builder.append(codeText)
                builder.pop()
                index = endIndex + 1
                continue
            }
        }

        builder.append(text[index])
        index++
    }

    return builder.toAnnotatedString()
}
