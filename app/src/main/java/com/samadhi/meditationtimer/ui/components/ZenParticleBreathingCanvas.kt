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
import com.samadhi.meditationtimer.domain.model.BreathPhase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Data structure representing an individual glowing particle in the breath field.
 */
private data class BreathDot(
    val index: Int,
    val angle: Float,
    val normalizedRadius: Float, // 0.0 (center) to 1.0 (outer boundary)
    val baseRadiusScale: Float,
    val dotSize: Float,
    val baseAlpha: Float,
    val color: Color,
    val jitterSpeed: Float,
    val jitterPhase: Float
)

/**
 * Organic easeInOutCubic easing function.
 */
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
    phaseProgress: Float, // 0.0f (start of phase) to 1.0f (end of phase)
    modifier: Modifier = Modifier,
    particleCount: Int = 450
) {
    // Generate particle field with golden ratio spiral (phyllotaxis) for organic natural balance
    val particles = remember(particleCount) {
        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat() // ~2.39996 rad (~137.5 deg)
        val palette = listOf(
            Color(0xFFFFF7DB), // Pure Warm Luminous Light
            Color(0xFFFFE082), // Glowing Soft Gold
            Color(0xFFF5BA42), // Zen Amber Gold
            Color(0xFFE5A93B), // Plum Village Warm Amber
            Color(0xFF64FFDA)  // Serene Celestial Jade Sparkle
        )

        List(particleCount) { i ->
            val angle = i.toFloat() * goldenAngle
            // Non-linear distribution for dense center with gentle atmospheric fade to edges
            val r = sqrt(i.toFloat() / particleCount.toFloat())
            val size = when {
                i % 25 == 0 -> Random.nextFloat() * 2.2f + 3.0f // Accent star dots
                i % 7 == 0 -> Random.nextFloat() * 1.5f + 2.2f
                else -> Random.nextFloat() * 1.0f + 1.2f
            }
            val alpha = (0.35f + 0.65f * (1.0f - r * 0.35f)).coerceIn(0.2f, 1.0f)
            val color = if (i % 30 == 0) palette[4] else palette[i % 4]

            BreathDot(
                index = i,
                angle = angle,
                normalizedRadius = r,
                baseRadiusScale = Random.nextFloat() * 0.15f + 0.92f,
                dotSize = size,
                baseAlpha = alpha,
                color = color,
                jitterSpeed = Random.nextFloat() * 1.5f + 1.0f,
                jitterPhase = Random.nextFloat() * 6.28f
            )
        }
    }

    // Organic shimmering transition
    val infiniteTransition = rememberInfiniteTransition(label = "particleShimmer")
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

    // Calculate breath expansion factor based on phase and easeInOutCubic
    val easedProgress = easeInOutCubic(phaseProgress)
    val (expansionFactor, phaseGlowColor) = when (currentPhase) {
        BreathPhase.INHALE -> {
            // Expand from core (0.28) to full bloom (1.0)
            val expansion = 0.28f + (0.72f * easedProgress)
            Pair(expansion, Color(0xFFFFD580))
        }
        BreathPhase.HOLD_FULL -> {
            // Suspended at full bloom with gentle organic harmonic breathing pulse
            val suspensionPulse = (sin((shimmerTime * 2f).toDouble()) * 0.025).toFloat()
            Pair(1.0f + suspensionPulse, Color(0xFFFFF0B3))
        }
        BreathPhase.EXHALE -> {
            // Gently converge from full bloom (1.0) back to core (0.28)
            val contraction = 1.0f - (0.72f * easedProgress)
            Pair(contraction, Color(0xFF81D4FA))
        }
        BreathPhase.HOLD_EMPTY -> {
            // Quiet resting center
            val restingPulse = (sin((shimmerTime * 1.5f).toDouble()) * 0.015).toFloat()
            Pair(0.28f + restingPulse, Color(0xFFFFCC80))
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * 0.88f
        if (maxRadius <= 0f) return@Canvas

        // 1. Serene Moonlit Atmospheric Background Radial Aura
        val currentAtmosphereRadius = (maxRadius * expansionFactor * 1.15f).coerceAtLeast(30f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseGlowColor.copy(alpha = (0.22f * expansionFactor).coerceIn(0f, 1f)),
                    Color(0xFFE5A93B).copy(alpha = (0.08f * expansionFactor).coerceIn(0f, 1f)),
                    Color(0xFF0E172A).copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = center,
                radius = currentAtmosphereRadius
            ),
            radius = currentAtmosphereRadius,
            center = center
        )

        // 2. Luminous Central Sanctuary Core
        val coreRadius = (maxRadius * 0.22f * corePulse).coerceAtLeast(15f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF9E6).copy(alpha = 0.50f),
                    Color(0xFFFFD580).copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = center,
                radius = coreRadius
            ),
            radius = coreRadius,
            center = center
        )

        // 3. Render Field of Glowing Warm Gold Particles
        particles.forEach { dot ->
            val jitter = (sin((shimmerTime * dot.jitterSpeed + dot.jitterPhase).toDouble()) * 2.5).toFloat()
            // Expansion physics
            val dynamicR = maxRadius * dot.normalizedRadius * dot.baseRadiusScale * expansionFactor + jitter
            val currentAngle = (dot.angle.toDouble() + (shimmerTime.toDouble() * 0.04 * (1.0 - dot.normalizedRadius.toDouble()))).toFloat()

            val dotX = (center.x.toDouble() + cos(currentAngle.toDouble()) * dynamicR.toDouble()).toFloat()
            val dotY = (center.y.toDouble() + sin(currentAngle.toDouble()) * dynamicR.toDouble()).toFloat()

            // Subtle depth luminescence
            val shimmerAlpha = (sin((shimmerTime * dot.jitterSpeed * 1.5f + dot.jitterPhase).toDouble()) * 0.15).toFloat()
            val dotAlpha = (dot.baseAlpha + shimmerAlpha).coerceIn(0.15f, 1.0f)
            val effectiveColor = dot.color.copy(alpha = dotAlpha)

            // Render soft glow halo for larger accent particles
            if (dot.dotSize > 2.0f) {
                drawCircle(
                    color = effectiveColor.copy(alpha = dotAlpha * 0.35f),
                    radius = dot.dotSize * 2.2f,
                    center = Offset(dotX, dotY)
                )
            }

            // Render sharp luminous particle core
            drawCircle(
                color = effectiveColor,
                radius = dot.dotSize,
                center = Offset(dotX, dotY)
            )
        }
    }
}
