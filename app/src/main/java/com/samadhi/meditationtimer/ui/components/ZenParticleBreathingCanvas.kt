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
import com.samadhi.meditationtimer.ui.theme.ZenThemeId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private fun easeInOutCubic(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return if (t < 0.5f) {
        4f * t * t * t
    } else {
        1f - (-2f * t + 2f).let { it * it * it } / 2f
    }
}

private data class BreathDotParticle(
    val index: Int,
    val angle: Float,
    val homeRadiusRatio: Float,
    val expandRadiusRatio: Float,
    val size: Float,
    val glow: Float,
    val opacity: Float,
    val speed: Float,
    val wobble: Float,
    val phase: Float,
    val isLeafBlade: Boolean,
    val leafRotationOffset: Float,
    val color: Color
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier,
    particleCount: Int = 340
) {
    val currentTheme = LocalZenTheme.current

    // Generate 340 breathing dot & leaf particles with organic radial distribution
    val particles = remember(particleCount, currentTheme.id) {
        val palette = currentTheme.particlePalette
        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat() // ~137.5 deg phyllotaxis

        List(particleCount) { i ->
            val angle = i * goldenAngle + (Random.nextFloat() - 0.5f) * 0.25f
            val isLeaf = (i % 5 == 0) // 20% organic leaves, 80% luminous stardust dots
            val col = palette[i % palette.size]

            val homeR = (Random.nextFloat().pow(0.5f)) * 0.14f
            val expandR = (0.24f + (Random.nextFloat().pow(0.6f)) * 0.76f)

            val size = if (isLeaf) {
                Random.nextFloat() * 6.0f + 14.0f
            } else {
                Random.nextFloat() * 2.6f + 1.6f
            }

            val glow = if (isLeaf) 5.0f else (Random.nextFloat() * 5.0f + 3.0f)
            val op = (Random.nextFloat() * 0.35f + 0.55f).coerceIn(0.35f, 0.95f)

            BreathDotParticle(
                index = i,
                angle = angle,
                homeRadiusRatio = homeR,
                expandRadiusRatio = expandR,
                size = size,
                glow = glow,
                opacity = op,
                speed = Random.nextFloat() * 0.08f + 0.05f,
                wobble = Random.nextFloat() * 2.2f + 1.0f,
                phase = Random.nextFloat() * 6.28318f,
                isLeafBlade = isLeaf,
                leafRotationOffset = Random.nextFloat() * 360f,
                color = col
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particleBreathingClock")
    val animClockSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animClockSec"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val easedProgress = easeInOutCubic(phaseProgress)
    val (expansionFactor, phaseGlowColor) = when (currentPhase) {
        BreathPhase.INHALE -> {
            val expansion = 0.22f + (0.78f * easedProgress)
            Pair(expansion, currentTheme.primaryLight)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = sin(phaseProgress * PI.toFloat()) * 0.030f
            Pair(1.0f + suspensionPulse, currentTheme.primaryLight)
        }
        BreathPhase.EXHALE -> {
            val contraction = 1.0f - (0.78f * easedProgress)
            Pair(contraction, currentTheme.overrunColor)
        }
        BreathPhase.HOLD_EMPTY -> {
            val restingPulse = sin(phaseProgress * PI.toFloat()) * 0.015f
            Pair(0.22f + restingPulse, currentTheme.primaryDark)
        }
    }

    val leafPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * 0.86f
        if (maxRadius <= 0f) return@Canvas

        // 1. Atmosphere Radial Aura
        val auraRadius = maxRadius * (0.32f + 0.70f * expansionFactor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseGlowColor.copy(alpha = (0.24f * expansionFactor).coerceIn(0f, 0.42f)),
                    currentTheme.primaryDark.copy(alpha = (0.09f * expansionFactor).coerceIn(0f, 0.20f)),
                    Color.Transparent
                ),
                center = center,
                radius = auraRadius
            ),
            radius = auraRadius,
            center = center
        )

        // 2. Central Sanctuary Core Glow
        val coreR = maxRadius * 0.12f * corePulse * (0.75f + 0.35f * expansionFactor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    currentTheme.primaryLight.copy(alpha = 0.75f),
                    currentTheme.primary.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = center,
                radius = coreR
            ),
            radius = coreR,
            center = center
        )

        // 3. Render "A Thousand Dots, Breathing" Field with Organic Flutter & Multi-Pass Glow
        particles.forEach { p ->
            val homeR = p.homeRadiusRatio * maxRadius
            val expandR = p.expandRadiusRatio * maxRadius
            val r = homeR + (expandR - homeR) * expansionFactor

            // Multi-frequency harmonic organic wobble
            val wx = sin(animClockSec * p.speed * 2.2f + p.phase) * p.wobble * (1.2f + 1.8f * expansionFactor)
            val wy = cos(animClockSec * p.speed * 1.8f + p.phase * 1.3f) * p.wobble * (1.2f + 1.8f * expansionFactor)
            val angleDrift = sin(animClockSec * p.speed * 0.5f + p.phase) * 0.08f

            val curAngle = p.angle + angleDrift
            val pX = center.x + cos(curAngle) * r + wx
            val pY = center.y + sin(curAngle) * r + wy

            val dynamicAlpha = p.opacity * (0.50f + expansionFactor * 0.50f)

            if (p.isLeafBlade) {
                // Organic Theme-Specific Leaf Particle
                val rot = Math.toDegrees(curAngle.toDouble()).toFloat() + 90f + sin(animClockSec * 1.5f + p.phase) * 14f
                val leafSize = p.size * (0.80f + 0.35f * expansionFactor)

                drawThemeLeaf(
                    themeId = currentTheme.id,
                    path = leafPath,
                    center = Offset(pX, pY),
                    size = leafSize,
                    rotation = rot,
                    color = p.color.copy(alpha = dynamicAlpha.coerceIn(0.2f, 0.95f)),
                    highlightColor = currentTheme.primaryLight.copy(alpha = (dynamicAlpha * 0.75f).coerceIn(0.1f, 0.9f))
                )
            } else {
                // Luminous Breathing Dot (Glow pass + Crisp core pass)
                // Pass 1: Diffused Glow Halo
                drawCircle(
                    color = p.color.copy(alpha = (dynamicAlpha * 0.08f).coerceIn(0f, 0.25f)),
                    radius = p.size + p.glow * (0.8f + 0.4f * expansionFactor),
                    center = Offset(pX, pY)
                )

                // Pass 2: Brilliant Core Dot
                drawCircle(
                    color = p.color.copy(alpha = dynamicAlpha.coerceIn(0.25f, 1.0f)),
                    radius = p.size * (0.85f + 0.25f * expansionFactor),
                    center = Offset(pX, pY)
                )
            }
        }
    }
}

private fun DrawScope.drawThemeLeaf(
    themeId: ZenThemeId,
    path: Path,
    center: Offset,
    size: Float,
    rotation: Float,
    color: Color,
    highlightColor: Color
) {
    rotate(degrees = rotation, pivot = center) {
        path.reset()
        val halfW = size * 0.38f
        val halfH = size * 0.86f

        when (themeId) {
            ZenThemeId.BAMBOO_TEAL -> {
                // Pointed Bamboo Leaf
                val bHalfW = size * 0.28f
                path.moveTo(center.x, center.y - halfH)
                path.cubicTo(
                    center.x + bHalfW * 1.15f, center.y - halfH * 0.35f,
                    center.x + bHalfW * 0.85f, center.y + halfH * 0.35f,
                    center.x, center.y + halfH
                )
                path.cubicTo(
                    center.x - bHalfW * 0.85f, center.y + halfH * 0.35f,
                    center.x - bHalfW * 1.15f, center.y - halfH * 0.35f,
                    center.x, center.y - halfH
                )
                path.close()
            }
            ZenThemeId.SACRED_LOTUS -> {
                // Broad Cupped Lotus Petal
                path.moveTo(center.x, center.y - halfH * 0.95f)
                path.cubicTo(
                    center.x + halfW * 1.35f, center.y - halfH * 0.35f,
                    center.x + halfW * 1.15f, center.y + halfH * 0.45f,
                    center.x, center.y + halfH
                )
                path.cubicTo(
                    center.x - halfW * 1.15f, center.y + halfH * 0.45f,
                    center.x - halfW * 1.35f, center.y - halfH * 0.35f,
                    center.x, center.y - halfH * 0.95f
                )
                path.close()
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                // Notched Cherry Blossom Petal
                val topY = center.y - halfH * 0.95f
                path.moveTo(center.x, center.y + halfH)
                path.cubicTo(
                    center.x - halfW * 1.20f, center.y + halfH * 0.35f,
                    center.x - halfW * 1.10f, center.y - halfH * 0.45f,
                    center.x - halfW * 0.55f, topY
                )
                path.quadraticTo(
                    center.x, topY + halfH * 0.20f,
                    center.x + halfW * 0.55f, topY
                )
                path.cubicTo(
                    center.x + halfW * 1.10f, center.y - halfH * 0.45f,
                    center.x + halfW * 1.20f, center.y + halfH * 0.35f,
                    center.x, center.y + halfH
                )
                path.close()
            }
        }

        drawPath(path = path, color = color)
        drawPath(path = path, color = highlightColor, style = Stroke(width = 1.0f))
    }
}


