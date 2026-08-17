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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
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

private data class BreathLeafEntity(
    val index: Int,
    val angle: Float,
    val homeRadiusRatio: Float,
    val expandRadiusRatio: Float,
    val size: Float,
    val baseGlow: Float,
    val opacity: Float,
    val speed: Float,
    val wobble: Float,
    val flutterSpeed: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier,
    leafCount: Int = 220
) {
    val currentTheme = LocalZenTheme.current

    // Generate 220 breathing leaves distributed in a living phyllotaxis spiral
    val leaves = remember(leafCount, currentTheme.id) {
        val palette = currentTheme.particlePalette
        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat() // ~137.5 deg phyllotaxis

        List(leafCount) { i ->
            val angle = i * goldenAngle + (Random.nextFloat() - 0.5f) * 0.18f
            val col = palette[i % palette.size]

            val homeR = (Random.nextFloat().pow(0.5f)) * 0.14f
            val expandR = (0.22f + (Random.nextFloat().pow(0.55f)) * 0.78f)

            // Varied sizes with hero blooming leaves
            val size = when {
                i % 10 == 0 -> Random.nextFloat() * 6.0f + 24.0f // Hero leaves
                i % 3 == 0 -> Random.nextFloat() * 5.0f + 19.0f
                else -> Random.nextFloat() * 4.0f + 15.0f
            }

            val glow = Random.nextFloat() * 3.0f + 6.0f
            val op = (Random.nextFloat() * 0.20f + 0.80f).coerceIn(0.75f, 1.0f)

            BreathLeafEntity(
                index = i,
                angle = angle,
                homeRadiusRatio = homeR,
                expandRadiusRatio = expandR,
                size = size,
                baseGlow = glow,
                opacity = op,
                speed = Random.nextFloat() * 0.08f + 0.05f,
                wobble = Random.nextFloat() * 2.2f + 1.2f,
                flutterSpeed = Random.nextFloat() * 1.5f + 1.2f,
                phase = Random.nextFloat() * 6.28318f,
                color = col
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "leafBreathingClock")
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
    val (expansionFactor, phaseGlowColor, glowPulseIntensity) = when (currentPhase) {
        BreathPhase.INHALE -> {
            val expansion = 0.22f + (0.78f * easedProgress)
            val glow = 0.35f + (0.65f * easedProgress)
            Triple(expansion, currentTheme.primaryLight, glow)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = sin(phaseProgress * PI.toFloat()) * 0.030f
            val glowPulse = 1.0f + sin(phaseProgress * PI.toFloat()) * 0.22f
            Triple(1.0f + suspensionPulse, currentTheme.primaryLight, glowPulse)
        }
        BreathPhase.EXHALE -> {
            val contraction = 1.0f - (0.78f * easedProgress)
            val glow = 1.0f - (0.65f * easedProgress)
            Triple(contraction, currentTheme.overrunColor, glow)
        }
        BreathPhase.HOLD_EMPTY -> {
            val restingPulse = sin(phaseProgress * PI.toFloat()) * 0.015f
            val glow = 0.35f + sin(phaseProgress * PI.toFloat()) * 0.10f
            Triple(0.22f + restingPulse, currentTheme.primaryDark, glow)
        }
    }

    val leafPath = remember { Path() }
    val spinePath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * 0.86f
        if (maxRadius <= 0f) return@Canvas

        // 1. Atmosphere Radial Aura (Pulsating in sync with breathing)
        val auraRadius = maxRadius * (0.34f + 0.70f * expansionFactor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseGlowColor.copy(alpha = (0.32f * glowPulseIntensity).coerceIn(0f, 0.55f)),
                    currentTheme.primaryDark.copy(alpha = (0.14f * glowPulseIntensity).coerceIn(0f, 0.30f)),
                    Color.Transparent
                ),
                center = center,
                radius = auraRadius
            ),
            radius = auraRadius,
            center = center
        )

        // 2. Central Sanctuary Core Glow (Pulsating)
        val coreR = maxRadius * 0.13f * corePulse * (0.75f + 0.35f * expansionFactor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    currentTheme.primaryLight.copy(alpha = (0.88f * glowPulseIntensity).coerceIn(0.4f, 1.0f)),
                    currentTheme.primary.copy(alpha = (0.45f * glowPulseIntensity).coerceIn(0.2f, 0.7f)),
                    Color.Transparent
                ),
                center = center,
                radius = coreR
            ),
            radius = coreR,
            center = center
        )

        // 3. Render Living Field of 220 Breathing Leaves with Pulsating Glow Halos
        leaves.forEach { leaf ->
            val homeR = leaf.homeRadiusRatio * maxRadius
            val expandR = leaf.expandRadiusRatio * maxRadius
            val r = homeR + (expandR - homeR) * expansionFactor

            // Multi-frequency harmonic organic wobble
            val wx = sin(animClockSec * leaf.speed * 2.2f + leaf.phase) * leaf.wobble * (1.2f + 1.8f * expansionFactor)
            val wy = cos(animClockSec * leaf.speed * 1.8f + leaf.phase * 1.3f) * leaf.wobble * (1.2f + 1.8f * expansionFactor)
            val angleDrift = sin(animClockSec * leaf.speed * 0.5f + leaf.phase) * 0.08f

            val curAngle = leaf.angle + angleDrift
            val pX = center.x + cos(curAngle) * r + wx
            val pY = center.y + sin(curAngle) * r + wy

            val dynamicAlpha = leaf.opacity * (0.60f + expansionFactor * 0.40f)
            val flutter = sin(animClockSec * leaf.flutterSpeed + leaf.phase) * (10f + 8f * expansionFactor)
            val rot = Math.toDegrees(curAngle.toDouble()).toFloat() + 90f + flutter

            val currentLeafSize = leaf.size * (0.85f + 0.30f * expansionFactor)

            drawGlowingLeaf(
                themeId = currentTheme.id,
                path = leafPath,
                spinePath = spinePath,
                center = Offset(pX, pY),
                size = currentLeafSize,
                rotation = rot,
                color = leaf.color.copy(alpha = dynamicAlpha.coerceIn(0.50f, 1.0f)),
                highlightColor = currentTheme.primaryLight,
                glowIntensity = glowPulseIntensity,
                dynamicAlpha = dynamicAlpha
            )
        }
    }
}

private fun DrawScope.drawGlowingLeaf(
    themeId: ZenThemeId,
    path: Path,
    spinePath: Path,
    center: Offset,
    size: Float,
    rotation: Float,
    color: Color,
    highlightColor: Color,
    glowIntensity: Float,
    dynamicAlpha: Float
) {
    rotate(degrees = rotation, pivot = center) {
        path.reset()
        spinePath.reset()
        val halfW = size * 0.38f
        val halfH = size * 0.86f

        when (themeId) {
            ZenThemeId.BAMBOO_TEAL -> {
                // Pointed Bamboo / Bodhi Leaf
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

                spinePath.moveTo(center.x, center.y - halfH * 0.82f)
                spinePath.lineTo(center.x, center.y + halfH * 0.82f)
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

                spinePath.moveTo(center.x, center.y - halfH * 0.78f)
                spinePath.quadraticTo(center.x + halfW * 0.15f, center.y, center.x, center.y + halfH * 0.78f)
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

                spinePath.moveTo(center.x, topY + halfH * 0.35f)
                spinePath.lineTo(center.x, center.y + halfH * 0.80f)
            }
        }

        // Pass 1: Wide Pulsating Glow Halo (Swells and dims with the breath)
        val outerGlowAlpha = (0.28f * glowIntensity * dynamicAlpha).coerceIn(0f, 0.50f)
        drawPath(
            path = path,
            color = highlightColor.copy(alpha = outerGlowAlpha),
            style = Stroke(width = 7.0f * (0.8f + 0.4f * glowIntensity), cap = StrokeCap.Round)
        )

        // Pass 2: Inner Radiant Bloom
        val innerGlowAlpha = (0.50f * glowIntensity * dynamicAlpha).coerceIn(0.1f, 0.75f)
        drawPath(
            path = path,
            color = highlightColor.copy(alpha = innerGlowAlpha),
            style = Stroke(width = 3.2f * (0.8f + 0.3f * glowIntensity), cap = StrokeCap.Round)
        )

        // Pass 3: Solid Leaf Body Fill
        drawPath(path = path, color = color)

        // Pass 4: Crisp Outer Rim Outline
        val rimAlpha = (0.85f * dynamicAlpha).coerceIn(0.4f, 1.0f)
        drawPath(
            path = path,
            color = highlightColor.copy(alpha = rimAlpha),
            style = Stroke(width = 1.3f)
        )

        // Pass 5: Center Spine Vein Detail
        drawPath(
            path = spinePath,
            color = highlightColor.copy(alpha = (rimAlpha * 0.65f)),
            style = Stroke(width = 1.1f, cap = StrokeCap.Round)
        )
    }
}



