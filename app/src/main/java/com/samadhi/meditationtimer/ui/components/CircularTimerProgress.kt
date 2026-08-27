package com.samadhi.meditationtimer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder

@Composable
fun CircularTimerProgress(
    progress: Float, // 0.0f to 1.0f
    isOverrun: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 8.dp,
    intervalMinutes: Int = 0,
    totalDurationMinutes: Int = 0,
    content: @Composable () -> Unit
) {
    val currentTheme = LocalZenTheme.current

    val activeColor = if (isOverrun) currentTheme.overrunColor else currentTheme.primary
    val trackBorderColor = currentTheme.border
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (isPaused) 0 else 1000, easing = LinearEasing),
        label = "smoothProgressArc"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val canvasSize = this.size
            val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
            val radius = (canvasSize.minDimension - strokeWidth.toPx()) / 2f

            // 1. Track Background Ring
            drawCircle(
                color = trackBorderColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth.toPx())
            )

            // 3. Active Progress Arc
            val sweepAngle = if (isOverrun) {
                360f
            } else {
                animatedProgress * 360f
            }

            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // 4. Interval Milestone Notches (if interval bell is active)
            if (intervalMinutes > 0 && totalDurationMinutes > intervalMinutes) {
                val totalSteps = totalDurationMinutes / intervalMinutes
                val stepAngle = (intervalMinutes.toFloat() / totalDurationMinutes.toFloat()) * 360f

                for (step in 1 until totalSteps) {
                    val angleDeg = -90f + (step * stepAngle)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val notchX = center.x + (radius * kotlin.math.cos(angleRad)).toFloat()
                    val notchY = center.y + (radius * kotlin.math.sin(angleRad)).toFloat()

                    val isPassed = (step * stepAngle) <= sweepAngle

                    // Outer glowing milestone dot
                    drawCircle(
                        color = if (isPassed) currentTheme.primaryLight else Color.White.copy(alpha = 0.5f),
                        radius = 4.5f,
                        center = Offset(notchX, notchY)
                    )
                    // Inner bright core
                    drawCircle(
                        color = if (isPassed) Color.White else trackBorderColor,
                        radius = 2.2f,
                        center = Offset(notchX, notchY)
                    )
                }
            }
        }

        content()
    }
}
