package com.samadhi.meditationtimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    modifier: Modifier = Modifier,
    onSliding: ((Float) -> Unit)? = null
) {
    val currentTheme = LocalZenTheme.current
    val thumbPath = remember { Path() }
    var isDragging by remember { mutableStateOf(false) }

    val dragHaloScale by animateFloatAsState(
        targetValue = if (isDragging) 1.35f else 1.0f,
        animationSpec = tween(180),
        label = "dragHaloScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp)
            .pointerInput(valueRange) {
                detectTapGestures(
                    onPress = {
                        isDragging = true
                        tryAwaitRelease()
                        isDragging = false
                    }
                ) { offset ->
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val fraction = (offset.x / width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                        onSliding?.invoke(1.2f)
                    }
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        onSliding?.invoke(0.8f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSliding?.invoke(0f)
                    },
                    onDragCancel = {
                        isDragging = false
                        onSliding?.invoke(0f)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val fraction = (change.position.x / width).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                        onSliding?.invoke(dragAmount)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val thumbX = (fraction * width).coerceIn(16f, width - 16f)

            // 1. INACTIVE STEM (Ahead of thumb): Organic Dormant Slate/Wood Branch
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(thumbX, centerY),
                end = Offset(width, centerY),
                strokeWidth = 9.0f,
                cap = StrokeCap.Round
            )
            // Subtle branch grain line
            drawLine(
                color = Color(0xFF334155),
                start = Offset(thumbX + 4f, centerY - 1.5f),
                end = Offset(width - 2f, centerY - 1.5f),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )

            // Inactive Branch Nodes
            val nodeSpacing = width / 6f
            for (i in 1..5) {
                val nodeX = i * nodeSpacing
                if (nodeX > thumbX + 12f) {
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(nodeX, centerY - 7f),
                        end = Offset(nodeX, centerY + 7f),
                        strokeWidth = 3.2f,
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
                    strokeWidth = 11.0f,
                    cap = StrokeCap.Round
                )

                // Luminous top highlight
                drawLine(
                    color = currentTheme.primaryLight.copy(alpha = 0.65f),
                    start = Offset(4f, centerY - 2.5f),
                    end = Offset(thumbX - 2f, centerY - 2.5f),
                    strokeWidth = 3.0f,
                    cap = StrokeCap.Round
                )

                // Living Branch Nodes
                for (i in 1..5) {
                    val nodeX = i * nodeSpacing
                    if (nodeX < thumbX - 12f) {
                        drawLine(
                            color = currentTheme.primaryLight,
                            start = Offset(nodeX, centerY - 8f),
                            end = Offset(nodeX, centerY + 8f),
                            strokeWidth = 3.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 3. THUMB: Theme-adaptive Zen Sprout Pebble with Dynamic Drag Halo
            // Outer Radiant Aura
            drawCircle(
                color = currentTheme.primary.copy(alpha = if (isDragging) 0.45f else 0.28f),
                radius = 28.0f * dragHaloScale,
                center = Offset(thumbX, centerY)
            )

            // Inner Radiant Ring
            drawCircle(
                color = currentTheme.primaryLight.copy(alpha = if (isDragging) 0.65f else 0.38f),
                radius = 20.0f * dragHaloScale,
                center = Offset(thumbX, centerY)
            )

            // Pebble Core
            drawCircle(
                color = Color(0xFF0A101D),
                radius = 16.5f,
                center = Offset(thumbX, centerY)
            )

            // Pebble Outer Theme Ring
            drawCircle(
                color = currentTheme.primaryLight,
                radius = 16.5f,
                center = Offset(thumbX, centerY),
                style = Stroke(width = 3.4f)
            )

            // Sprout silhouette matching theme (Enlarged & Clear)
            thumbPath.reset()
            when (currentTheme.particleType) {
                ParticleType.BAMBOO_LEAF -> {
                    // Sprouted Bamboo Blade
                    thumbPath.moveTo(thumbX, centerY - 6f)
                    thumbPath.cubicTo(
                        thumbX + 9f, centerY - 16f,
                        thumbX + 14f, centerY - 22f,
                        thumbX + 16f, centerY - 30f
                    )
                    thumbPath.cubicTo(
                        thumbX + 5f, centerY - 25f,
                        thumbX + 1f, centerY - 15f,
                        thumbX, centerY - 6f
                    )
                }
                ParticleType.LOTUS_PETAL -> {
                    // Lotus Bud
                    thumbPath.moveTo(thumbX, centerY - 6f)
                    thumbPath.cubicTo(
                        thumbX + 11f, centerY - 16f,
                        thumbX + 10f, centerY - 25f,
                        thumbX, centerY - 31f
                    )
                    thumbPath.cubicTo(
                        thumbX - 10f, centerY - 25f,
                        thumbX - 11f, centerY - 16f,
                        thumbX, centerY - 6f
                    )
                }
                ParticleType.SAKURA_PETAL -> {
                    // Sakura Blossom Bud
                    thumbPath.moveTo(thumbX, centerY - 6f)
                    thumbPath.cubicTo(
                        thumbX + 11f, centerY - 16f,
                        thumbX + 9f, centerY - 27f,
                        thumbX + 3f, centerY - 30f
                    )
                    thumbPath.quadraticTo(
                        thumbX, centerY - 25f,
                        thumbX - 3f, centerY - 30f
                    )
                    thumbPath.cubicTo(
                        thumbX - 9f, centerY - 27f,
                        thumbX - 11f, centerY - 16f,
                        thumbX, centerY - 6f
                    )
                }
            }

            drawPath(
                path = thumbPath,
                color = currentTheme.primaryLight
            )
        }
    }
}
