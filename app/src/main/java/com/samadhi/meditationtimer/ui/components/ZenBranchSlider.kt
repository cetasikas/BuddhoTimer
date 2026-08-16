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
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType

@Composable
fun ZenBranchSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 1f..120f,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    val thumbPath = remember { Path() }

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
            val thumbX = (fraction * width).coerceIn(14f, width - 14f)

            // 1. INACTIVE STEM (Ahead of thumb): Organic Dormant Slate/Wood Branch
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(thumbX, centerY),
                end = Offset(width, centerY),
                strokeWidth = 8.0f,
                cap = StrokeCap.Round
            )
            // Subtle branch grain line
            drawLine(
                color = Color(0xFF334155),
                start = Offset(thumbX + 4f, centerY - 1.5f),
                end = Offset(width - 2f, centerY - 1.5f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )

            // Inactive Branch Nodes
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

            // 2. ACTIVE STEM (Behind thumb): Living Organic Stem with Theme Gradient
            if (thumbX > 4f) {
                val activeBrush = Brush.horizontalGradient(
                    colors = listOf(
                        currentTheme.primaryDark,
                        currentTheme.primary,
                        currentTheme.primaryLight
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
                    color = currentTheme.primaryLight.copy(alpha = 0.55f),
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
                            color = currentTheme.primaryLight,
                            start = Offset(nodeX, centerY - 7f),
                            end = Offset(nodeX, centerY + 7f),
                            strokeWidth = 3.0f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 3. THUMB: Theme-adaptive Zen Sprout Pebble
            // Glowing Halo
            drawCircle(
                color = currentTheme.primary.copy(alpha = 0.30f),
                radius = 20.0f,
                center = Offset(thumbX, centerY)
            )

            // Pebble Core
            drawCircle(
                color = Color(0xFF0C1322),
                radius = 12.0f,
                center = Offset(thumbX, centerY)
            )
            // Pebble Outer Theme Ring
            drawCircle(
                color = currentTheme.primaryLight,
                radius = 12.0f,
                center = Offset(thumbX, centerY),
                style = Stroke(width = 2.8f)
            )

            // Sprout silhouette matching theme
            thumbPath.reset()
            when (currentTheme.particleType) {
                ParticleType.BAMBOO_LEAF -> {
                    // Sprouted Bodhi Leaf
                    thumbPath.moveTo(thumbX, centerY - 5f)
                    thumbPath.cubicTo(
                        thumbX + 7f, centerY - 14f,
                        thumbX + 11f, centerY - 17f,
                        thumbX + 13f, centerY - 23f
                    )
                    thumbPath.cubicTo(
                        thumbX + 4f, centerY - 20f,
                        thumbX + 1f, centerY - 13f,
                        thumbX, centerY - 5f
                    )
                }
                ParticleType.LOTUS_PETAL -> {
                    // Lotus Bud
                    thumbPath.moveTo(thumbX, centerY - 5f)
                    thumbPath.cubicTo(
                        thumbX + 8f, centerY - 13f,
                        thumbX + 7f, centerY - 20f,
                        thumbX, centerY - 24f
                    )
                    thumbPath.cubicTo(
                        thumbX - 7f, centerY - 20f,
                        thumbX - 8f, centerY - 13f,
                        thumbX, centerY - 5f
                    )
                }
                ParticleType.SAKURA_PETAL -> {
                    // Sakura Blossom Bud
                    thumbPath.moveTo(thumbX, centerY - 5f)
                    thumbPath.cubicTo(
                        thumbX + 8f, centerY - 14f,
                        thumbX + 6f, centerY - 22f,
                        thumbX + 2f, centerY - 24f
                    )
                    thumbPath.quadraticTo(
                        thumbX, centerY - 20f,
                        thumbX - 2f, centerY - 24f
                    )
                    thumbPath.cubicTo(
                        thumbX - 6f, centerY - 22f,
                        thumbX - 8f, centerY - 14f,
                        thumbX, centerY - 5f
                    )
                }
            }
            thumbPath.close()

            drawPath(
                path = thumbPath,
                color = currentTheme.primaryLight
            )
        }
    }
}
