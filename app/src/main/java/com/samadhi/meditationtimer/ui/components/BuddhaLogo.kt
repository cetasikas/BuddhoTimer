package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BuddhaOutlineLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = Color.White,
    strokeWidth: Float = 2.0f,
    showEnsoCircle: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = Stroke(
            width = strokeWidth * (w / 48f).coerceAtLeast(0.8f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // 1. Outer Minimal Enso Circle
        if (showEnsoCircle) {
            val ensoPath = Path().apply {
                val r = (w.coerceAtMost(h) / 2f) - (stroke.width * 1.2f)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                    startAngleDegrees = 20f,
                    sweepAngleDegrees = 335f,
                    forceMoveTo = true
                )
            }
            drawPath(ensoPath, tint, style = stroke)
        }

        // Scale factors mapping from 100x100 coordinate grid
        val sx = w / 100f
        val sy = h / 100f

        // 2. Ushnisha (Topknot Dome)
        val ushnisha = Path().apply {
            moveTo(45f * sx, 24f * sy)
            quadraticTo(50f * sx, 16f * sy, 55f * sx, 24f * sy)
        }
        drawPath(ushnisha, tint, style = stroke)

        // 3. Head & Face Outline
        val head = Path().apply {
            moveTo(38f * sx, 32f * sy)
            quadraticTo(38f * sx, 22f * sy, 50f * sx, 22f * sy)
            quadraticTo(62f * sx, 22f * sy, 62f * sx, 32f * sy)
            quadraticTo(62f * sx, 42f * sy, 50f * sx, 44f * sy)
            quadraticTo(38f * sx, 42f * sy, 38f * sx, 32f * sy)
        }
        drawPath(head, tint, style = stroke)

        // 4. Elongated Ears
        val leftEar = Path().apply {
            moveTo(38f * sx, 30f * sy)
            quadraticTo(34f * sx, 35f * sy, 35f * sx, 40f * sy)
        }
        drawPath(leftEar, tint, style = stroke)

        val rightEar = Path().apply {
            moveTo(62f * sx, 30f * sy)
            quadraticTo(66f * sx, 35f * sy, 65f * sx, 40f * sy)
        }
        drawPath(rightEar, tint, style = stroke)

        // 5. Serene Downward Eyes / Calm Brow
        val leftEye = Path().apply {
            moveTo(43f * sx, 34f * sy)
            quadraticTo(46f * sx, 36f * sy, 48f * sx, 34.5f * sy)
        }
        drawPath(leftEye, tint, style = stroke)

        val rightEye = Path().apply {
            moveTo(52f * sx, 34.5f * sy)
            quadraticTo(54f * sx, 36f * sy, 57f * sx, 34f * sy)
        }
        drawPath(rightEye, tint, style = stroke)

        // 6. Shoulders & Torso
        val shoulders = Path().apply {
            // Left Shoulder & Arm down to hands
            moveTo(42f * sx, 43f * sy)
            quadraticTo(32f * sx, 46f * sy, 31f * sx, 56f * sy)
            quadraticTo(34f * sx, 64f * sy, 45f * sx, 62f * sy)

            // Right Shoulder & Arm down to hands
            moveTo(58f * sx, 43f * sy)
            quadraticTo(68f * sx, 46f * sy, 69f * sx, 56f * sy)
            quadraticTo(66f * sx, 64f * sy, 55f * sx, 62f * sy)
        }
        drawPath(shoulders, tint, style = stroke)

        // 7. Prayer / Anjali Mudra Hands at Chest
        val hands = Path().apply {
            moveTo(47f * sx, 58f * sy)
            lineTo(50f * sx, 47f * sy)
            lineTo(53f * sx, 58f * sy)
            quadraticTo(50f * sx, 61f * sy, 47f * sx, 58f * sy)
        }
        drawPath(hands, tint, style = stroke)

        // 8. Robe Sash Line
        val robe = Path().apply {
            moveTo(60f * sx, 45f * sy)
            quadraticTo(54f * sx, 52f * sy, 40f * sx, 56f * sy)
        }
        drawPath(robe, tint, style = stroke)

        // 9. Folded Lotus Legs & Crossed Knee Base
        val lotusBase = Path().apply {
            moveTo(26f * sx, 67f * sy)
            quadraticTo(24f * sx, 74f * sy, 32f * sx, 77f * sy)
            quadraticTo(50f * sx, 80f * sy, 68f * sx, 77f * sy)
            quadraticTo(76f * sx, 74f * sy, 74f * sx, 67f * sy)
            quadraticTo(66f * sx, 62f * sy, 50f * sx, 68f * sy)
            quadraticTo(34f * sx, 62f * sy, 26f * sx, 67f * sy)
        }
        drawPath(lotusBase, tint, style = stroke)

        // Knee fold crease
        val fold = Path().apply {
            moveTo(36f * sx, 73f * sy)
            quadraticTo(50f * sx, 76f * sy, 64f * sx, 73f * sy)
        }
        drawPath(fold, tint, style = stroke)
    }
}
