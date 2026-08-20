package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Official BuddhoTimer App Emblem:
 * An authentic Japanese Sumi-e styled Zen Ensō brush circle enclosing a glowing sacred Bodhi leaf.
 */
@Composable
fun BuddhaOutlineLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = Color(0xFFFFE082),
    strokeWidth: Float = 2.0f,
    showEnsoCircle: Boolean = true
) {
    BuddhoEnsoLogo(
        modifier = modifier,
        size = size,
        tint = tint,
        strokeWidth = strokeWidth,
        showEnsoCircle = showEnsoCircle
    )
}

@Composable
fun BuddhoEnsoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = Color(0xFFFFE082),
    strokeWidth: Float = 2.0f,
    showEnsoCircle: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = Stroke(
            width = strokeWidth * (w / 48f).coerceAtLeast(1.0f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // 1. Dynamic Sumi-e Style Zen Ensō Circle
        if (showEnsoCircle) {
            val r = (w.coerceAtMost(h) / 2f) - (stroke.width * 1.4f)
            val ensoPath = Path().apply {
                arcTo(
                    rect = Rect(cx - r, cy - r, cx + r, cy + r),
                    startAngleDegrees = 32f,
                    sweepAngleDegrees = 312f,
                    forceMoveTo = true
                )
            }
            drawPath(
                path = ensoPath,
                color = tint,
                style = stroke
            )
        }

        // 2. Central Sacred Bodhi Leaf (Heart shape with elegant slender drip tip)
        val leafH = h * 0.44f
        val leafW = w * 0.28f
        val halfW = leafW / 2f
        val halfH = leafH / 2f

        // Center slightly offset for visual balance inside the Enso ring
        val leafCenterY = cy - h * 0.02f

        val leafPath = Path().apply {
            // Extended delicate drip-tip at the top
            moveTo(cx, leafCenterY - halfH * 1.08f)
            // Right lobe into broad heart base
            cubicTo(
                cx + halfW * 1.32f, leafCenterY - halfH * 0.35f,
                cx + halfW * 1.15f, leafCenterY + halfH * 0.48f,
                cx, leafCenterY + halfH * 0.92f
            )
            // Left lobe back up to drip-tip
            cubicTo(
                cx - halfW * 1.15f, leafCenterY + halfH * 0.48f,
                cx - halfW * 1.32f, leafCenterY - halfH * 0.35f,
                cx, leafCenterY - halfH * 1.08f
            )
            close()
        }

        // Filled subtle glow for leaf body
        drawPath(
            path = leafPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    tint.copy(alpha = 0.35f),
                    tint.copy(alpha = 0.15f)
                ),
                startY = leafCenterY - halfH,
                endY = leafCenterY + halfH
            )
        )

        // Outline contour of Bodhi leaf
        drawPath(
            path = leafPath,
            color = tint,
            style = Stroke(
                width = stroke.width * 0.85f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 3. Central Spine & Radiating Lateral Veins
        val veinStroke = Stroke(
            width = (stroke.width * 0.60f).coerceAtLeast(0.8f),
            cap = StrokeCap.Round
        )

        val spinePath = Path().apply {
            moveTo(cx, leafCenterY - halfH * 0.90f)
            lineTo(cx, leafCenterY + halfH * 0.85f)
        }
        drawPath(spinePath, tint.copy(alpha = 0.85f), style = veinStroke)

        // Lateral veins (3 pairs radiating delicately)
        val branches = 3
        for (i in 1..branches) {
            val fy = leafCenterY - halfH * 0.45f + (halfH * 1.05f) * (i / (branches + 1f))
            val lateralVeins = Path().apply {
                // Right vein
                moveTo(cx, fy)
                lineTo(cx + halfW * 0.52f, fy - halfH * 0.14f)
                // Left vein
                moveTo(cx, fy)
                lineTo(cx - halfW * 0.52f, fy - halfH * 0.14f)
            }
            drawPath(lateralVeins, tint.copy(alpha = 0.70f), style = veinStroke)
        }
    }
}
