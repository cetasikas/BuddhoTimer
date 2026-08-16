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

/**
 * Individual miniature leaf/petal particle in the breathing canvas.
 */
private data class LeafBreathDot(
    val index: Int,
    val angle: Float,
    val normalizedRadius: Float,
    val baseRadiusScale: Float,
    val leafSize: Float,
    val leafRotation: Float,
    val baseAlpha: Float,
    val color: Color,
    val jitterSpeed: Float,
    val jitterPhase: Float
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
    particleCount: Int = 220
) {
    val currentTheme = LocalZenTheme.current

    // Generate leaf particle field with phyllotaxis spiral distribution
    val particles = remember(particleCount, currentTheme.id) {
        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat() // ~137.5 deg
        val palette = currentTheme.particlePalette

        List(particleCount) { i ->
            val angle = i.toFloat() * goldenAngle
            val r = sqrt(i.toFloat() / particleCount.toFloat())
            val size = when {
                i % 18 == 0 -> Random.nextFloat() * 4.0f + 12.0f // Hero accent leaves
                i % 5 == 0 -> Random.nextFloat() * 3.0f + 9.0f
                else -> Random.nextFloat() * 2.5f + 6.5f
            }
            val alpha = (0.45f + 0.55f * (1.0f - r * 0.30f)).coerceIn(0.25f, 0.95f)
            val color = palette[i % palette.size]
            val rot = Math.toDegrees(angle.toDouble()).toFloat() + (Random.nextFloat() - 0.5f) * 40f

            LeafBreathDot(
                index = i,
                angle = angle,
                normalizedRadius = r,
                baseRadiusScale = Random.nextFloat() * 0.15f + 0.92f,
                leafSize = size,
                leafRotation = rot,
                baseAlpha = alpha,
                color = color,
                jitterSpeed = Random.nextFloat() * 1.5f + 1.0f,
                jitterPhase = Random.nextFloat() * 6.28f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "leafShimmer")
    val shimmerTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerRadian"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val easedProgress = easeInOutCubic(phaseProgress)
    val (expansionFactor, phaseGlowColor) = when (currentPhase) {
        BreathPhase.INHALE -> {
            val expansion = 0.28f + (0.72f * easedProgress)
            Pair(expansion, currentTheme.primaryLight)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = (sin((shimmerTime * 2f).toDouble()) * 0.025).toFloat()
            Pair(1.0f + suspensionPulse, currentTheme.primaryLight)
        }
        BreathPhase.EXHALE -> {
            val contraction = 1.0f - (0.72f * easedProgress)
            Pair(contraction, currentTheme.overrunColor)
        }
        BreathPhase.HOLD_EMPTY -> {
            val restingPulse = (sin((shimmerTime * 1.5f).toDouble()) * 0.015).toFloat()
            Pair(0.28f + restingPulse, currentTheme.primaryDark)
        }
    }

    val leafPath = remember { Path() }
    val detailPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * 0.88f
        if (maxRadius <= 0f) return@Canvas

        // 1. Serene Radial Atmosphere Aura
        val currentAtmosphereRadius = (maxRadius * expansionFactor * 1.15f).coerceAtLeast(30f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseGlowColor.copy(alpha = (0.20f * expansionFactor).coerceIn(0f, 1f)),
                    currentTheme.primaryDark.copy(alpha = (0.08f * expansionFactor).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = center,
                radius = currentAtmosphereRadius
            ),
            radius = currentAtmosphereRadius,
            center = center
        )

        // 2. Luminous Central Sanctuary Core
        val coreRadius = (maxRadius * 0.20f * corePulse).coerceAtLeast(15f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    currentTheme.primaryLight.copy(alpha = 0.35f),
                    currentTheme.primary.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = coreRadius
            ),
            radius = coreRadius,
            center = center
        )

        // 3. Render Field of Miniature Breathing Leaves/Petals
        particles.forEach { dot ->
            val jitter = (sin((shimmerTime * dot.jitterSpeed + dot.jitterPhase).toDouble()) * 2.5).toFloat()
            val dynamicR = maxRadius * dot.normalizedRadius * dot.baseRadiusScale * expansionFactor + jitter
            val currentAngle = (dot.angle.toDouble() + (shimmerTime.toDouble() * 0.04 * (1.0 - dot.normalizedRadius.toDouble()))).toFloat()

            val leafX = (center.x.toDouble() + cos(currentAngle.toDouble()) * dynamicR.toDouble()).toFloat()
            val leafY = (center.y.toDouble() + sin(currentAngle.toDouble()) * dynamicR.toDouble()).toFloat()

            val shimmerAlpha = (sin((shimmerTime * dot.jitterSpeed * 1.5f + dot.jitterPhase).toDouble()) * 0.15).toFloat()
            val leafAlpha = (dot.baseAlpha + shimmerAlpha).coerceIn(0.18f, 1.0f)
            val effectiveColor = dot.color.copy(alpha = leafAlpha)

            val leafCenter = Offset(leafX, leafY)
            val rotationDeg = dot.leafRotation + (shimmerTime * 5f)

            drawMiniatureLeaf(
                particleType = currentTheme.particleType,
                shapePath = leafPath,
                detailPath = detailPath,
                center = leafCenter,
                size = dot.leafSize,
                rotation = rotationDeg,
                color = effectiveColor,
                glowColor = phaseGlowColor
            )
        }
    }
}

private fun DrawScope.drawMiniatureLeaf(
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

        val halfW = size * 0.40f
        val halfH = size * 0.85f

        when (particleType) {
            ParticleType.BAMBOO_LEAF -> {
                // Miniature Bodhi / Bamboo Leaf
                shapePath.moveTo(center.x, center.y - halfH)
                shapePath.cubicTo(
                    center.x + halfW * 1.1f, center.y - halfH * 0.3f,
                    center.x + halfW * 0.8f, center.y + halfH * 0.4f,
                    center.x, center.y + halfH
                )
                shapePath.cubicTo(
                    center.x - halfW * 0.8f, center.y + halfH * 0.4f,
                    center.x - halfW * 1.1f, center.y - halfH * 0.3f,
                    center.x, center.y - halfH
                )
                shapePath.close()

                detailPath.moveTo(center.x, center.y - halfH * 0.8f)
                detailPath.lineTo(center.x, center.y + halfH * 0.8f)
            }
            ParticleType.LOTUS_PETAL -> {
                // Miniature Lotus Petal
                shapePath.moveTo(center.x, center.y - halfH * 0.9f)
                shapePath.cubicTo(
                    center.x + halfW * 1.3f, center.y - halfH * 0.35f,
                    center.x + halfW * 1.1f, center.y + halfH * 0.45f,
                    center.x, center.y + halfH
                )
                shapePath.cubicTo(
                    center.x - halfW * 1.1f, center.y + halfH * 0.45f,
                    center.x - halfW * 1.3f, center.y - halfH * 0.35f,
                    center.x, center.y - halfH * 0.9f
                )
                shapePath.close()

                detailPath.moveTo(center.x, center.y - halfH * 0.75f)
                detailPath.lineTo(center.x, center.y + halfH * 0.75f)
            }
            ParticleType.SAKURA_PETAL -> {
                // Miniature Sakura Petal
                val topY = center.y - halfH * 0.9f
                shapePath.moveTo(center.x, center.y + halfH)
                shapePath.cubicTo(
                    center.x - halfW * 1.2f, center.y + halfH * 0.35f,
                    center.x - halfW * 1.1f, center.y - halfH * 0.45f,
                    center.x - halfW * 0.55f, topY
                )
                shapePath.quadraticTo(
                    center.x, topY + halfH * 0.22f,
                    center.x + halfW * 0.55f, topY
                )
                shapePath.cubicTo(
                    center.x + halfW * 1.1f, center.y - halfH * 0.45f,
                    center.x + halfW * 1.2f, center.y + halfH * 0.35f,
                    center.x, center.y + halfH
                )
                shapePath.close()
            }
        }

        // Fill miniature leaf body
        drawPath(path = shapePath, color = color)

        // Vein / Crease
        drawPath(
            path = detailPath,
            color = Color(0xFF041014).copy(alpha = (0.28f * color.alpha).coerceIn(0.10f, 0.40f)),
            style = Stroke(width = 0.8f)
        )
    }
}
