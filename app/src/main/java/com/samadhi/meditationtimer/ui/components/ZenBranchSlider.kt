package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTealLight

@Composable
fun ZenBranchSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 1f..120f,
    modifier: Modifier = Modifier
) {
    val leafPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 6.dp)
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val fraction = (offset.x / width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val fraction = (change.position.x / width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val thumbX = fraction * width

            // 1. INACTIVE BRANCH (Ahead of thumb): Organic Dormant Slate Branch
            // Base inactive stem
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(thumbX, centerY),
                end = Offset(width, centerY),
                strokeWidth = 6.0f,
                cap = StrokeCap.Round
            )
            // Subtle branch grain line
            drawLine(
                color = Color(0xFF334155),
                start = Offset(thumbX + 4f, centerY - 1f),
                end = Offset(width - 2f, centerY - 1f),
                strokeWidth = 2.0f,
                cap = StrokeCap.Round
            )

            // Inactive Branch Bamboo Nodes
            val nodeSpacing = width / 6f
            for (i in 1..5) {
                val nodeX = i * nodeSpacing
                if (nodeX > thumbX + 8f) {
                    // Bamboo node ring
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(nodeX, centerY - 5f),
                        end = Offset(nodeX, centerY + 5f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. ACTIVE BRANCH (Behind thumb): Living Bamboo/Branch with Teal Gradient
            if (thumbX > 2f) {
                val activeBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF164E4D),
                        Color(0xFF2EA8A4),
                        Color(0xFF5EEAD4)
                    ),
                    startX = 0f,
                    endX = thumbX
                )

                // Living branch body
                drawLine(
                    brush = activeBrush,
                    start = Offset(0f, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = 7.0f,
                    cap = StrokeCap.Round
                )

                // Luminous top highlight
                drawLine(
                    color = Color(0xFF99F6E4).copy(alpha = 0.5f),
                    start = Offset(4f, centerY - 1.5f),
                    end = Offset(thumbX - 2f, centerY - 1.5f),
                    strokeWidth = 2.0f,
                    cap = StrokeCap.Round
                )

                // Living Branch Nodes
                for (i in 1..5) {
                    val nodeX = i * nodeSpacing
                    if (nodeX < thumbX - 8f) {
                        drawLine(
                            color = Color(0xFF5EEAD4),
                            start = Offset(nodeX, centerY - 5.5f),
                            end = Offset(nodeX, centerY + 5.5f),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 3. THUMB: Minimal Zen Leaf Sprout Bead
            // Glowing Halo
            drawCircle(
                color = Color(0xFF5EEAD4).copy(alpha = 0.35f),
                radius = 16.0f,
                center = Offset(thumbX, centerY)
            )

            // Bead Body
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 9.0f,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = Color(0xFF5EEAD4),
                radius = 9.0f,
                center = Offset(thumbX, centerY),
                style = Stroke(width = 2.2f)
            )

            // Delicate sprouting leaf at thumb
            leafPath.reset()
            leafPath.moveTo(thumbX, centerY - 4f)
            leafPath.cubicTo(
                thumbX + 6f, centerY - 12f,
                thumbX + 9f, centerY - 14f,
                thumbX + 11f, centerY - 18f
            )
            leafPath.cubicTo(
                thumbX + 3f, centerY - 16f,
                thumbX + 1f, centerY - 11f,
                thumbX, centerY - 4f
            )
            leafPath.close()

            drawPath(
                path = leafPath,
                color = Color(0xFF5EEAD4)
            )
        }
    }
}
