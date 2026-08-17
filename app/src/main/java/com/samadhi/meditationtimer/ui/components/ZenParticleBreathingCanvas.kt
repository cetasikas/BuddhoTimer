package com.samadhi.meditationtimer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class BreathLeaf(
    val index: Int,
    val angle: Float,
    val normalizedRadius: Float,
    val baseRadiusScale: Float,
    val leafSize: Float,
    val initialRotation: Float,
    val baseAlpha: Float,
    val color: Color,
    val flutterSpeed: Float,
    val flutterPhase: Float
)

private fun easeInOutCubic(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return if (t < 0.5f) {
        4f * t * t * t
    } else {
        1f - (-2f * t + 2f).let { it * it * it } / 2f
    }
}

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier,
    particleCount: Int = 140
) {
    val currentTheme = LocalZenTheme.current

    // Generate leaf particle field with phyllotaxis spiral distribution
    val leaves = remember(particleCount, currentTheme.id) {
        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat() // ~137.5 deg
        val palette = currentTheme.particlePalette

        List(particleCount) { i ->
            val angle = i.toFloat() * goldenAngle
            val r = sqrt(i.toFloat() / particleCount.toFloat())
            // Generously sized leaves for striking, tactile organic presence
            val size = when {
                i % 12 == 0 -> Random.nextFloat() * 8.0f + 26.0f // Hero blooming leaves
                i % 4 == 0 -> Random.nextFloat() * 6.0f + 20.0f
                else -> Random.nextFloat() * 5.0f + 16.0f
            }
            val alpha = (0.50f + 0.50f * (1.0f - r * 0.25f)).coerceIn(0.35f, 0.95f)
            val color = palette[i % palette.size]
            val rot = Math.toDegrees(angle.toDouble()).toFloat()

            BreathLeaf(
                index = i,
                angle = angle,
                normalizedRadius = r,
                baseRadiusScale = Random.nextFloat() * 0.12f + 0.94f,
                leafSize = size,
                initialRotation = rot,
                baseAlpha = alpha,
                color = color,
                flutterSpeed = Random.nextFloat() * 1.5f + 1.2f,
                flutterPhase = Random.nextFloat() * 6.28f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "leafBreathingClock")
    val shimmerTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerRadian"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val easedProgress = easeInOutCubic(phaseProgress)
    val (expansionFactor, phaseGlowColor) = when (currentPhase) {
        BreathPhase.INHALE -> {
            // Expand organically from sanctuary core (0.24) to full blooming perimeter (1.0)
            val expansion = 0.24f + (0.76f * easedProgress)
            Pair(expansion, currentTheme.primaryLight)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = (sin((shimmerTime * 2f).toDouble()) * 0.03).toFloat()
            Pair(1.0f + suspensionPulse, currentTheme.primaryLight)
        }
        BreathPhase.EXHALE -> {
            // Converge gently back into stillness
            val contraction = 1.0f - (0.76f * easedProgress)
            Pair(contraction, currentTheme.overrunColor)
        }
        BreathPhase.HOLD_EMPTY -> {
            val restingPulse = (sin((shimmerTime * 1.5f).toDouble()) * 0.02).toFloat()
            Pair(0.24f + restingPulse, currentTheme.primaryDark)
        }
    }

    val leafPath = remember { Path() }
    val detailPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * 0.84f
        if (maxRadius <= 0f) return@Canvas

        // 1. Serene Radial Atmosphere Aura
        val currentAtmosphereRadius = (maxRadius * expansionFactor * 1.20f).coerceAtLeast(40f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseGlowColor.copy(alpha = (0.22f * expansionFactor).coerceIn(0f, 1f)),
                    currentTheme.primaryDark.copy(alpha = (0.10f * expansionFactor).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = center,
                radius = currentAtmosphereRadius
            ),
            radius = currentAtmosphereRadius,
            center = center
        )

        // 2. Luminous Central Sanctuary Core
        val coreRadius = (maxRadius * 0.18f * corePulse).coerceAtLeast(18f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    currentTheme.primaryLight.copy(alpha = 0.40f),
                    currentTheme.primary.copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = center,
                radius = coreRadius
            ),
            radius = coreRadius,
            center = center
        )

        // 3. Render Field of Blooming / Breathing Leaves
        leaves.forEach { leaf ->
            val flutter = (sin((shimmerTime * leaf.flutterSpeed + leaf.flutterPhase).toDouble()) * 3.0).toFloat()
            val dynamicR = maxRadius * leaf.normalizedRadius * leaf.baseRadiusScale * expansionFactor + flutter
            val currentAngle = (leaf.angle.toDouble() + (shimmerTime.toDouble() * 0.05 * (1.0 - leaf.normalizedRadius.toDouble()))).toFloat()

            val leafX = (center.x.toDouble() + cos(currentAngle.toDouble()) * dynamicR.toDouble()).toFloat()
            val leafY = (center.y.toDouble() + sin(currentAngle.toDouble()) * dynamicR.toDouble()).toFloat()

            val shimmerAlpha = (sin((shimmerTime * leaf.flutterSpeed * 1.4f + leaf.flutterPhase).toDouble()) * 0.12).toFloat()
            val leafAlpha = (leaf.baseAlpha + shimmerAlpha).coerceIn(0.25f, 1.0f)
            val effectiveColor = leaf.color.copy(alpha = leafAlpha)

            // Dynamic rotation pointing radially outward + fluttering sway
            val rotationDeg = leaf.initialRotation + 90f + (sin((shimmerTime * 1.5f + leaf.flutterPhase).toDouble()) * 18f).toFloat()

            drawBloomingLeaf(
                particleType = currentTheme.particleType,
                shapePath = leafPath,
                detailPath = detailPath,
                center = Offset(leafX, leafY),
                size = leaf.leafSize * (0.85f + 0.25f * expansionFactor),
                rotation = rotationDeg,
                color = effectiveColor,
                glowColor = phaseGlowColor
            )
        }
    }
}

private fun DrawScope.drawBloomingLeaf(
    particleType: ParticleType,
    shapePath: Path,
    detailPath: Path,
    center: Offset,
    size: Float,
    rotation: Float,
    color: Color,
    glowColor: Color
) {
    rotate(degrees = rotation, pivot = center) {
        shapePath.reset()
        detailPath.reset()

        val halfW = size * 0.42f
        val halfH = size * 0.88f

        when (particleType) {
            ParticleType.BAMBOO_LEAF -> {
                // Pointed Bodhi / Bamboo Leaf
                shapePath.moveTo(center.x, center.y - halfH)
                shapePath.cubicTo(
                    center.x + halfW * 1.15f, center.y - halfH * 0.32f,
                    center.x + halfW * 0.85f, center.y + halfH * 0.38f,
                    center.x, center.y + halfH
                )
                shapePath.cubicTo(
                    center.x - halfW * 0.85f, center.y + halfH * 0.38f,
                    center.x - halfW * 1.15f, center.y - halfH * 0.32f,
                    center.x, center.y - halfH
                )
                shapePath.close()

                detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                detailPath.lineTo(center.x, center.y + halfH * 0.85f)
            }
            ParticleType.LOTUS_PETAL -> {
                // Broad Cupped Lotus Petal
                shapePath.moveTo(center.x, center.y - halfH * 0.95f)
                shapePath.cubicTo(
                    center.x + halfW * 1.35f, center.y - halfH * 0.35f,
                    center.x + halfW * 1.15f, center.y + halfH * 0.45f,
                    center.x, center.y + halfH
                )
                shapePath.cubicTo(
                    center.x - halfW * 1.15f, center.y + halfH * 0.45f,
                    center.x - halfW * 1.35f, center.y - halfH * 0.35f,
                    center.x, center.y - halfH * 0.95f
                )
                shapePath.close()

                detailPath.moveTo(center.x, center.y - halfH * 0.80f)
                detailPath.quadraticTo(center.x + halfW * 0.2f, center.y, center.x, center.y + halfH * 0.80f)
            }
            ParticleType.SAKURA_PETAL -> {
                // Notched Cherry Blossom Petal
                val topY = center.y - halfH * 0.95f
                shapePath.moveTo(center.x, center.y + halfH)
                shapePath.cubicTo(
                    center.x - halfW * 1.25f, center.y + halfH * 0.35f,
                    center.x - halfW * 1.15f, center.y - halfH * 0.45f,
                    center.x - halfW * 0.58f, topY
                )
                shapePath.quadraticTo(
                    center.x, topY + halfH * 0.22f,
                    center.x + halfW * 0.58f, topY
                )
                shapePath.cubicTo(
                    center.x + halfW * 1.15f, center.y - halfH * 0.45f,
                    center.x + halfW * 1.25f, center.y + halfH * 0.35f,
                    center.x, center.y + halfH
                )
                shapePath.close()
            }
        }

        // Fill leaf body
        drawPath(path = shapePath, color = color)

        // Vein / Texture line
        drawPath(
            path = detailPath,
            color = Color(0xFF041014).copy(alpha = (0.30f * color.alpha).coerceIn(0.10f, 0.45f)),
            style = Stroke(width = 1.0f)
        )
    }
}
