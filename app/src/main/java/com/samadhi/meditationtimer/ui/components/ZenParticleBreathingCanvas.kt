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

private data class LotusStardust(
    val baseAngle: Float,
    val baseDistance: Float,
    val speed: Float,
    val size: Float,
    val phase: Float,
    val color: Color
)

private data class SakuraPetalParticle(
    val spiralAngle: Float,
    val spiralRadiusRatio: Float,
    val size: Float,
    val baseRotation: Float,
    val rotSpeed: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier,
    particleCount: Int = 140
) {
    val currentTheme = LocalZenTheme.current

    // Sacred Lotus particles (for dispersion on exhale and coalescence on inhale)
    val lotusParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(72) { i ->
            LotusStardust(
                baseAngle = Random.nextFloat() * 6.28318f,
                baseDistance = Random.nextFloat() * 0.85f + 0.15f,
                speed = Random.nextFloat() * 0.8f + 0.6f,
                size = Random.nextFloat() * 4.5f + 2.5f,
                phase = Random.nextFloat() * 6.28318f,
                color = palette[i % palette.size]
            )
        }
    }

    // Sakura orbiting particles
    val sakuraParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(48) { i ->
            val angle = i * 0.45f
            val rRatio = sqrt(i.toFloat() / 48f)
            SakuraPetalParticle(
                spiralAngle = angle,
                spiralRadiusRatio = rRatio,
                size = Random.nextFloat() * 6f + 14f,
                baseRotation = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 1.8f,
                phase = Random.nextFloat() * 6.28318f,
                color = palette[i % palette.size]
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
    val (expansionFactor, phaseGlowColor) = when (currentPhase) {
        BreathPhase.INHALE -> {
            val expansion = 0.24f + (0.76f * easedProgress)
            Pair(expansion, currentTheme.primaryLight)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = sin(phaseProgress * PI.toFloat()) * 0.035f
            Pair(1.0f + suspensionPulse, currentTheme.primaryLight)
        }
        BreathPhase.EXHALE -> {
            val contraction = 1.0f - (0.76f * easedProgress)
            Pair(contraction, currentTheme.overrunColor)
        }
        BreathPhase.HOLD_EMPTY -> {
            val restingPulse = sin(phaseProgress * PI.toFloat()) * 0.015f
            Pair(0.24f + restingPulse, currentTheme.primaryDark)
        }
    }

    val path1 = remember { Path() }
    val path2 = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * 0.84f
        if (maxRadius <= 0f) return@Canvas

        when (currentTheme.id) {
            ZenThemeId.BAMBOO_TEAL -> {
                // 🎋 1. BREATHING BAMBOO PLANT: Rooted, organic bamboo that grows and stretches upward on inhale & rests on exhale
                drawBreathingBambooPlant(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    clockSec = animClockSec,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    phaseGlowColor = phaseGlowColor,
                    path = path1,
                    detailPath = path2
                )
            }
            ZenThemeId.SACRED_LOTUS -> {
                // 🪷 2. SACRED LOTUS: Blooms on inhale, breaks into glowing floating particles on exhale, and gathers back on inhale
                drawBreathingLotusWithParticles(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    isExhaleOrEmpty = (currentPhase == BreathPhase.EXHALE || currentPhase == BreathPhase.HOLD_EMPTY),
                    particles = lotusParticles,
                    clockSec = animClockSec,
                    corePulse = corePulse,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    phaseGlowColor = phaseGlowColor,
                    path = path1,
                    detailPath = path2
                )
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                // 🌸 3. SAKURA TWILIGHT: Central blossom with orbiting cherry blossom petals that expand & spiral in twilight harmony
                drawBreathingSakuraVortex(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    particles = sakuraParticles,
                    clockSec = animClockSec,
                    corePulse = corePulse,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    phaseGlowColor = phaseGlowColor,
                    path = path1,
                    detailPath = path2
                )
            }
        }
    }
}

/**
 * 🎋 Theme 1: Breathing Bamboo Plant
 * A living bamboo clump that gracefully stretches, flexes, and blooms outward on inhale and gently compresses on exhale.
 */
private fun DrawScope.drawBreathingBambooPlant(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    clockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    phaseGlowColor: Color,
    path: Path,
    detailPath: Path
) {
    // 1. Atmosphere Radial Aura
    val auraRadius = maxRadius * (0.35f + 0.65f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                phaseGlowColor.copy(alpha = (0.24f * expansion).coerceIn(0f, 0.40f)),
                primaryDark.copy(alpha = (0.08f * expansion).coerceIn(0f, 0.20f)),
                Color.Transparent
            ),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center
    )

    // Base root position (anchored slightly below center)
    val rootY = center.y + maxRadius * 0.72f
    val baseStalkHeight = maxRadius * (0.40f + 0.95f * expansion)

    // Stalk definitions: (xOffsetFromCenter, stalkHeightRatio, curveDirection, nodeCount)
    val stalks = listOf(
        Triple(0f, 1.0f, 0.08f),         // Center tall stalk
        Triple(-maxRadius * 0.26f, 0.88f, -0.16f), // Left graceful stalk
        Triple(maxRadius * 0.24f, 0.82f, 0.18f)    // Right gentle stalk
    )

    stalks.forEachIndexed { stalkIdx, (xOff, heightRatio, curveDir) ->
        val stalkH = baseStalkHeight * heightRatio
        val sway = sin(clockSec * 1.2f + stalkIdx * 1.5f) * (4.0f + 6.0f * expansion)
        val stalkTopX = center.x + xOff + (curveDir * stalkH * 0.35f) + sway
        val stalkTopY = rootY - stalkH
        val stalkBaseX = center.x + xOff * 0.65f

        val nodeCount = 5
        var prevX = stalkBaseX
        var prevY = rootY

        for (node in 1..nodeCount) {
            val t = node.toFloat() / nodeCount.toFloat()
            // Organic quadratic curve up the culm
            val currX = stalkBaseX + (stalkTopX - stalkBaseX) * t + sin(t * PI.toFloat()) * (curveDir * 18f)
            val currY = rootY - (stalkH * t)

            // Draw Bamboo Segment (Culm)
            drawLine(
                color = primaryColor.copy(alpha = (0.55f + 0.35f * expansion).coerceIn(0f, 1f)),
                start = Offset(prevX, prevY),
                end = Offset(currX, currY),
                strokeWidth = (7.5f - t * 3.5f) * (0.85f + 0.20f * expansion),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // Draw Bamboo Joint Node (Ring)
            drawCircle(
                color = primaryLight.copy(alpha = 0.85f),
                radius = (4.8f - t * 2.0f) * (0.85f + 0.20f * expansion),
                center = Offset(currX, currY)
            )

            // Sprout Bamboo Leaves from node branches
            if (node >= 2) {
                val leafSide = if ((node + stalkIdx) % 2 == 0) 1f else -1f
                val branchLength = (22f + 16f * expansion) * (0.6f + (1f - t) * 0.5f)
                val branchAngle = (if (leafSide > 0) -35f else -145f) + (curveDir * 20f) + sin(clockSec * 1.5f + node) * 8f
                val rad = Math.toRadians(branchAngle.toDouble())
                val leafTipX = currX + cos(rad).toFloat() * branchLength
                val leafTipY = currY + sin(rad).toFloat() * branchLength

                // Draw Branch twig
                drawLine(
                    color = primaryColor.copy(alpha = 0.70f),
                    start = Offset(currX, currY),
                    end = Offset(leafTipX, leafTipY),
                    strokeWidth = 1.8f
                )

                // Draw Cluster of Bamboo Leaves
                val leafSizes = listOf(1.0f, 0.75f)
                leafSizes.forEachIndexed { lIdx, scale ->
                    val leafAngle = branchAngle + (lIdx * 25f * leafSide) + sin(clockSec * 2f + lIdx) * 6f
                    val leafSize = (24f + 18f * expansion) * scale

                    rotate(degrees = leafAngle + 90f, pivot = Offset(leafTipX, leafTipY)) {
                        path.reset()
                        val halfW = leafSize * 0.28f
                        val halfH = leafSize * 0.85f
                        path.moveTo(leafTipX, leafTipY - halfH)
                        path.cubicTo(
                            leafTipX + halfW * 1.15f, leafTipY - halfH * 0.35f,
                            leafTipX + halfW * 0.85f, leafTipY + halfH * 0.35f,
                            leafTipX, leafTipY + halfH
                        )
                        path.cubicTo(
                            leafTipX - halfW * 0.85f, leafTipY + halfH * 0.35f,
                            leafTipX - halfW * 1.15f, leafTipY - halfH * 0.35f,
                            leafTipX, leafTipY - halfH
                        )
                        path.close()

                        drawPath(
                            path = path,
                            color = primaryLight.copy(alpha = (0.60f + 0.35f * expansion).coerceIn(0.2f, 0.95f))
                        )
                        drawPath(
                            path = path,
                            color = primaryDark.copy(alpha = 0.40f),
                            style = Stroke(width = 1.0f)
                        )
                    }
                }
            }

            prevX = currX
            prevY = currY
        }
    }
}

/**
 * 🪷 Theme 2: Sacred Lotus Flower with Ethereal Particle Dispersion on Exhale
 * Blossoms in radiant golden amber on Inhale, and disperses into swirling golden particles on Exhale!
 */
private fun DrawScope.drawBreathingLotusWithParticles(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    isExhaleOrEmpty: Boolean,
    particles: List<LotusStardust>,
    clockSec: Float,
    corePulse: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    phaseGlowColor: Color,
    path: Path,
    detailPath: Path
) {
    // 1. Atmosphere Radial Aura
    val auraRadius = maxRadius * (0.35f + 0.70f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                phaseGlowColor.copy(alpha = (0.26f * expansion).coerceIn(0f, 0.42f)),
                primaryDark.copy(alpha = (0.10f * expansion).coerceIn(0f, 0.22f)),
                Color.Transparent
            ),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center
    )

    // 2. Central Blooming Lotus Petals (Visible & intact during Inhale / Hold Full, subtly dissolving on Exhale)
    val petalCount = 8
    val petalSize = maxRadius * (0.24f + 0.46f * expansion)
    val flowerAlpha = if (isExhaleOrEmpty) (expansion * 1.1f).coerceIn(0.20f, 1.0f) else 1.0f

    for (layer in 0..2) {
        val layerCount = if (layer == 0) 6 else if (layer == 1) 8 else 10
        val layerRadius = maxRadius * (0.12f + layer * 0.14f * expansion)
        val layerScale = if (layer == 0) 0.65f else if (layer == 1) 0.88f else 1.05f
        val layerColor = if (layer == 0) primaryLight else if (layer == 1) primaryColor else primaryDark

        for (i in 0 until layerCount) {
            val angleDeg = (i.toFloat() / layerCount.toFloat()) * 360f + (layer * 22.5f) + (clockSec * 2.5f)
            val rad = Math.toRadians(angleDeg.toDouble())
            val petalX = center.x + cos(rad).toFloat() * layerRadius
            val petalY = center.y + sin(rad).toFloat() * layerRadius

            rotate(degrees = angleDeg + 90f, pivot = Offset(petalX, petalY)) {
                path.reset()
                val currentPetalSize = petalSize * layerScale
                val halfW = currentPetalSize * 0.48f
                val halfH = currentPetalSize * 0.90f

                path.moveTo(petalX, petalY - halfH)
                path.cubicTo(
                    petalX + halfW * 1.35f, petalY - halfH * 0.35f,
                    petalX + halfW * 1.15f, petalY + halfH * 0.45f,
                    petalX, petalY + halfH
                )
                path.cubicTo(
                    petalX - halfW * 1.15f, petalY + halfH * 0.45f,
                    petalX - halfW * 1.35f, petalY - halfH * 0.35f,
                    petalX, petalY - halfH
                )
                path.close()

                drawPath(
                    path = path,
                    color = layerColor.copy(alpha = (0.50f * flowerAlpha + 0.40f * expansion).coerceIn(0.15f, 0.90f))
                )
                drawPath(
                    path = path,
                    color = primaryLight.copy(alpha = (0.75f * flowerAlpha).coerceIn(0.10f, 0.85f)),
                    style = Stroke(width = 1.2f)
                )
            }
        }
    }

    // 3. Floating Golden Lotus Particles (Disperse outward on exhale, gather inward on inhale)
    particles.forEach { star ->
        // On exhale/empty, dispersion distance expands outward; on inhale/full, gathers inward
        val dispersionFactor = if (isExhaleOrEmpty) (1.40f - expansion * 0.60f) else (0.50f + expansion * 0.50f)
        val r = maxRadius * star.baseDistance * dispersionFactor + sin(clockSec * star.speed + star.phase) * 6f
        val angle = star.baseAngle + (clockSec * 0.15f * star.speed)
        val pX = center.x + cos(angle) * r
        val pY = center.y + sin(angle) * r

        val pAlpha = if (isExhaleOrEmpty) {
            (0.40f + (1f - expansion) * 0.55f).coerceIn(0.2f, 0.95f)
        } else {
            (0.30f + expansion * 0.60f).coerceIn(0.2f, 0.95f)
        }

        drawCircle(
            color = star.color.copy(alpha = pAlpha),
            radius = star.size * (0.8f + 0.4f * expansion),
            center = Offset(pX, pY)
        )
    }

    // 4. Central Glowing Lotus Pistil
    val coreR = maxRadius * 0.14f * corePulse * (0.75f + 0.35f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryLight.copy(alpha = 0.80f),
                primaryColor.copy(alpha = 0.45f),
                Color.Transparent
            ),
            center = center,
            radius = coreR
        ),
        radius = coreR,
        center = center
    )
}

/**
 * 🌸 Theme 3: Sakura Twilight Vortex
 * Iconic 5-petal cherry blossom flower at center with floating cherry petals orbiting in celestial spirals.
 */
private fun DrawScope.drawBreathingSakuraVortex(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    particles: List<SakuraPetalParticle>,
    clockSec: Float,
    corePulse: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    phaseGlowColor: Color,
    path: Path,
    detailPath: Path
) {
    // 1. Atmosphere Radial Aura
    val auraRadius = maxRadius * (0.35f + 0.70f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                phaseGlowColor.copy(alpha = (0.24f * expansion).coerceIn(0f, 0.38f)),
                primaryDark.copy(alpha = (0.09f * expansion).coerceIn(0f, 0.20f)),
                Color.Transparent
            ),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center
    )

    // 2. Central 5-Petal Iconic Sakura Blossom
    val blossomRadius = maxRadius * (0.22f + 0.38f * expansion)
    for (i in 0 until 5) {
        val angleDeg = (i * 72f) + (clockSec * 1.8f)
        val rad = Math.toRadians(angleDeg.toDouble())
        val petalX = center.x + cos(rad).toFloat() * (blossomRadius * 0.42f)
        val petalY = center.y + sin(rad).toFloat() * (blossomRadius * 0.42f)

        rotate(degrees = angleDeg + 90f, pivot = Offset(petalX, petalY)) {
            path.reset()
            val pSize = blossomRadius * 0.85f
            val halfW = pSize * 0.44f
            val halfH = pSize * 0.90f
            val topY = petalY - halfH * 0.95f

            path.moveTo(petalX, petalY + halfH)
            path.cubicTo(
                petalX - halfW * 1.25f, petalY + halfH * 0.35f,
                petalX - halfW * 1.15f, petalY - halfH * 0.45f,
                petalX - halfW * 0.58f, topY
            )
            path.quadraticTo(
                petalX, topY + halfH * 0.22f,
                petalX + halfW * 0.58f, topY
            )
            path.cubicTo(
                petalX + halfW * 1.15f, petalY - halfH * 0.45f,
                petalX + halfW * 1.25f, petalY + halfH * 0.35f,
                petalX, petalY + halfH
            )
            path.close()

            drawPath(
                path = path,
                color = primaryColor.copy(alpha = (0.65f + 0.30f * expansion).coerceIn(0.2f, 0.95f))
            )
            drawPath(
                path = path,
                color = primaryLight.copy(alpha = 0.80f),
                style = Stroke(width = 1.2f)
            )
        }
    }

    // 3. Floating Orbiting Cherry Blossom Petals (Expanding on Inhale, spiraling inward on Exhale)
    particles.forEach { petal ->
        val dynamicR = maxRadius * petal.spiralRadiusRatio * (0.35f + 0.65f * expansion) + sin(clockSec * 1.5f + petal.phase) * 5f
        val currentAngle = petal.spiralAngle + (clockSec * 0.08f * (1.1f - petal.spiralRadiusRatio))
        val pX = center.x + cos(currentAngle) * dynamicR
        val pY = center.y + sin(currentAngle) * dynamicR

        val rot = petal.baseRotation + (clockSec * 20f * petal.rotSpeed)
        val pAlpha = (0.45f + 0.50f * expansion).coerceIn(0.25f, 0.95f)

        rotate(degrees = rot, pivot = Offset(pX, pY)) {
            path.reset()
            val size = petal.size * (0.85f + 0.25f * expansion)
            val halfW = size * 0.42f
            val halfH = size * 0.88f
            val topY = pY - halfH * 0.95f

            path.moveTo(pX, pY + halfH)
            path.cubicTo(
                pX - halfW * 1.20f, pY + halfH * 0.35f,
                pX - halfW * 1.10f, pY - halfH * 0.45f,
                pX - halfW * 0.55f, topY
            )
            path.quadraticTo(
                pX, topY + halfH * 0.20f,
                pX + halfW * 0.55f, topY
            )
            path.cubicTo(
                pX + halfW * 1.10f, pY - halfH * 0.45f,
                pX + halfW * 1.20f, pY + halfH * 0.35f,
                pX, pY + halfH
            )
            path.close()

            drawPath(path = path, color = petal.color.copy(alpha = pAlpha))
        }
    }

    // 4. Luminous Twilight Blossom Center
    val coreR = maxRadius * 0.12f * corePulse * (0.80f + 0.30f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryLight.copy(alpha = 0.85f),
                primaryColor.copy(alpha = 0.40f),
                Color.Transparent
            ),
            center = center,
            radius = coreR
        ),
        radius = coreR,
        center = center
    )
}

