package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.samadhi.meditationtimer.ui.theme.ZenSage
import com.samadhi.meditationtimer.ui.theme.ZenSageDark
import com.samadhi.meditationtimer.ui.theme.ZenSageLight

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
            .height(54.dp)
            .padding(horizontal = 4.dp)
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
        Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val thumbX = (fraction * width).coerceIn(12f, width - 12f)

            // 1. INACTIVE BRANCH (Ahead of thumb): Organic Dormant Slate/Wood Branch
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(thumbX, centerY),
                end = Offset(width, centerY),
                strokeWidth = 8.0f,
                cap = StrokeCap.Round
            )
            // Subtle branch grain
            drawLine(
                color = Color(0xFF334155),
                start = Offset(thumbX + 4f, centerY - 1.5f),
                end = Offset(width - 2f, centerY - 1.5f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )

            // Inactive Branch Bamboo Nodes
            val nodeSpacing = width / 6f
            for (i in 1..5) {
                val nodeX = i * nodeSpacing
                if (nodeX > thumbX + 10f) {
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(nodeX, centerY - 6.5f),
                        end = Offset(nodeX, centerY + 6.5f),
                        strokeWidth = 3.0f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. ACTIVE BRANCH (Behind thumb): Living Bamboo/Branch with Calming Sage Gradient
            if (thumbX > 4f) {
                val activeBrush = Brush.horizontalGradient(
                    colors = listOf(
                        ZenSageDark,
                        ZenSage,
                        ZenSageLight
                    ),
                    startX = 0f,
                    endX = thumbX
                )

                // Living branch body
                drawLine(
                    brush = activeBrush,
                    start = Offset(0f, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = 9.5f,
                    cap = StrokeCap.Round
                )

                // Luminous top highlight
                drawLine(
                    color = ZenSageLight.copy(alpha = 0.55f),
                    start = Offset(4f, centerY - 2f),
                    end = Offset(thumbX - 2f, centerY - 2f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                // Living Branch Nodes
                for (i in 1..5) {
                    val nodeX = i * nodeSpacing
                    if (nodeX < thumbX - 10f) {
                        drawLine(
                            color = ZenSageLight,
                            start = Offset(nodeX, centerY - 7f),
                            end = Offset(nodeX, centerY + 7f),
                            strokeWidth = 3.0f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 3. THUMB: Large Comfortable Zen Leaf Sprout Bead
            // Glowing Halo
            drawCircle(
                color = ZenSage.copy(alpha = 0.28f),
                radius = 20.0f,
                center = Offset(thumbX, centerY)
            )

            // Pebble Core
            drawCircle(
                color = Color(0xFF0C1322),
                radius = 12.0f,
                center = Offset(thumbX, centerY)
            )
            // Pebble Sage Ring
            drawCircle(
                color = ZenSageLight,
                radius = 12.0f,
                center = Offset(thumbX, centerY),
                style = Stroke(width = 2.8f)
            )

            // Delicate sprouted Bodhi leaf atop thumb
            leafPath.reset()
            leafPath.moveTo(thumbX, centerY - 5f)
            leafPath.cubicTo(
                thumbX + 7f, centerY - 14f,
                thumbX + 11f, centerY - 17f,
                thumbX + 13f, centerY - 23f
            )
            leafPath.cubicTo(
                thumbX + 4f, centerY - 20f,
                thumbX + 1f, centerY - 13f,
                thumbX, centerY - 5f
            )
            leafPath.close()

            drawPath(
                path = leafPath,
                color = ZenSageLight
            )
        }
    }
}
