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

// 🌠 Slow celestial shooting star / comet weaving between objects in unison with breath
private data class BreathingComet(
    val id: Int,
    val baseAngle: Float,
    val orbitSpeed: Float,
    val radialPhase: Float,
    val minRadiusRatio: Float,
    val maxRadiusRatio: Float,
    val trailLength: Int,
    val size: Float,
    val color: Color
)

// 🎋 Bamboo Teal Mandala Particle (Pointed Bamboo Leaves & Stardust)
private data class BambooMandalaParticle(
    val ringIndex: Int,
    val leafIndex: Int,
    val tOnLeaf: Float,
    val size: Float,
    val baseAngle: Float,
    val dispersionAngle: Float,
    val dispersionDistance: Float,
    val dispersionSpeed: Float,
    val flutterSpeed: Float,
    val phase: Float,
    val isLeafBlade: Boolean,
    val color: Color
)

// 🪷 Sacred Lotus Mandala Particle
private data class LotusMandalaParticle(
    val ringIndex: Int,
    val petalIndex: Int,
    val tOnPetal: Float,
    val size: Float,
    val dispersionAngle: Float,
    val dispersionDistance: Float,
    val dispersionSpeed: Float,
    val phase: Float,
    val color: Color
)

// 🌸 Sakura Twilight Vortex Particle
private data class SakuraVortexParticle(
    val index: Int,
    val armIndex: Int,
    val radiusRatio: Float,
    val size: Float,
    val orbitSpeed: Float,
    val spinSpeed: Float,
    val phase: Float,
    val isPetalBlade: Boolean,
    val color: Color
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier,
    particleCount: Int = 160
) {
    val currentTheme = LocalZenTheme.current

    // 🌠 Slow breathing comets / shooting stars that weave gently between particles
    val breathingComets = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(8) { i ->
            BreathingComet(
                id = i,
                baseAngle = (i.toFloat() / 8f) * 6.28318f,
                orbitSpeed = 0.09f + (i % 3) * 0.035f,
                radialPhase = i * 1.35f,
                minRadiusRatio = 0.22f + (i % 4) * 0.08f,
                maxRadiusRatio = 0.72f + (i % 3) * 0.10f,
                trailLength = 7,
                size = Random.nextFloat() * 2.5f + 3.2f,
                color = palette[i % palette.size]
            )
        }
    }

    // 🎋 1. Bamboo Teal Radial Leaf Mandala
    val bambooMandalaParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(160) { i ->
            val ring = when {
                i < 36 -> 0  // Inner leaf bud
                i < 96 -> 1  // Mid blooming bamboo leaves
                else -> 2    // Outer celestial leaf canopy
            }
            val leavesInRing = if (ring == 0) 6 else if (ring == 1) 8 else 12
            val leafIdx = i % leavesInRing
            val t = ((i / leavesInRing) % 6).toFloat() / 5f
            BambooMandalaParticle(
                ringIndex = ring,
                leafIndex = leafIdx,
                tOnLeaf = t,
                size = if (i % 2 == 0) Random.nextFloat() * 6f + 14f else Random.nextFloat() * 3.5f + 2.5f,
                baseAngle = (leafIdx.toFloat() / leavesInRing.toFloat()) * 360f + (ring * 22.5f),
                dispersionAngle = Random.nextFloat() * 6.28318f,
                dispersionDistance = Random.nextFloat() * 0.85f + 0.20f,
                dispersionSpeed = Random.nextFloat() * 0.8f + 0.6f,
                flutterSpeed = Random.nextFloat() * 1.5f + 1.2f,
                phase = Random.nextFloat() * 6.28318f,
                isLeafBlade = (i % 2 == 0),
                color = palette[i % palette.size]
            )
        }
    }

    // 🪷 2. Sacred Lotus Particle Mandala
    val lotusMandalaParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(180) { i ->
            val ring = when {
                i < 40 -> 0 // Inner bud
                i < 100 -> 1 // Mid bloom
                else -> 2   // Outer sacred radiance
            }
            val petalsInRing = if (ring == 0) 6 else if (ring == 1) 8 else 12
            val petal = i % petalsInRing
            val t = ((i / petalsInRing) % 6).toFloat() / 5f
            LotusMandalaParticle(
                ringIndex = ring,
                petalIndex = petal,
                tOnPetal = t,
                size = if (ring == 0) Random.nextFloat() * 3.5f + 2.5f else Random.nextFloat() * 4.5f + 3.0f,
                dispersionAngle = Random.nextFloat() * 6.28318f,
                dispersionDistance = Random.nextFloat() * 0.85f + 0.20f,
                dispersionSpeed = Random.nextFloat() * 0.8f + 0.6f,
                phase = Random.nextFloat() * 6.28318f,
                color = palette[i % palette.size]
            )
        }
    }

    // 🌸 3. Sakura Twilight Vortex
    val sakuraVortexParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(160) { i ->
            val arm = i % 5
            val rRatio = sqrt(i.toFloat() / 160f)
            SakuraVortexParticle(
                index = i,
                armIndex = arm,
                radiusRatio = rRatio,
                size = if (i % 3 == 0) Random.nextFloat() * 6f + 12f else Random.nextFloat() * 3.5f + 2.5f,
                orbitSpeed = Random.nextFloat() * 0.4f + 0.6f,
                spinSpeed = (Random.nextFloat() - 0.5f) * 2.2f,
                phase = Random.nextFloat() * 6.28318f,
                isPetalBlade = (i % 3 == 0),
                color = palette[i % palette.size]
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

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * 0.86f
        if (maxRadius <= 0f) return@Canvas

        // 1. Atmosphere Radial Aura
        val auraRadius = maxRadius * (0.35f + 0.68f * expansionFactor)
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

        // 2. Render Theme-Specific Breathing Mandala / Vortex
        when (currentTheme.id) {
            ZenThemeId.BAMBOO_TEAL -> {
                // 🎋 1. BAMBOO RADIAL LEAF MANDALA
                drawBambooLeafMandala(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    isExhale = (currentPhase == BreathPhase.EXHALE || currentPhase == BreathPhase.HOLD_EMPTY),
                    particles = bambooMandalaParticles,
                    clockSec = animClockSec,
                    primaryLight = currentTheme.primaryLight,
                    path = path1
                )
            }
            ZenThemeId.SACRED_LOTUS -> {
                // 🪷 2. SACRED LOTUS PARTICLE MANDALA & STARDUST DISPERSION
                drawLotusParticleMandala(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    isExhale = (currentPhase == BreathPhase.EXHALE || currentPhase == BreathPhase.HOLD_EMPTY),
                    particles = lotusMandalaParticles,
                    clockSec = animClockSec,
                    path = path1
                )
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                // 🌸 3. SAKURA FIBONACCI TWILIGHT VORTEX
                drawSakuraFibonacciVortex(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    particles = sakuraVortexParticles,
                    clockSec = animClockSec,
                    primaryLight = currentTheme.primaryLight,
                    path = path1
                )
            }
        }

        // 3. 🌠 Slow Shooting Star / Comet Streamers Weaving Through Field in Unison with Breath
        drawBreathingCometStreamers(
            center = center,
            maxRadius = maxRadius,
            expansion = expansionFactor,
            comets = breathingComets,
            clockSec = animClockSec,
            primaryLight = currentTheme.primaryLight
        )

        // 4. Central Luminous Core
        val coreR = maxRadius * 0.13f * corePulse * (0.78f + 0.32f * expansionFactor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    currentTheme.primaryLight.copy(alpha = 0.85f),
                    currentTheme.primary.copy(alpha = 0.40f),
                    Color.Transparent
                ),
                center = center,
                radius = coreR
            ),
            radius = coreR,
            center = center
        )
    }
}

/**
 * 🎋 Theme 1: Bamboo Leaf Radial Mandala
 * A radiant teal/emerald mandala of pointed bamboo leaves and luminous jade stardust,
 * expanding outwards into a blooming leaf sanctuary on Inhale and swirling gently on Exhale.
 */
private fun DrawScope.drawBambooLeafMandala(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    isExhale: Boolean,
    particles: List<BambooMandalaParticle>,
    clockSec: Float,
    primaryLight: Color,
    path: Path
) {
    particles.forEach { p ->
        val leavesInRing = if (p.ringIndex == 0) 6 else if (p.ringIndex == 1) 8 else 12
        val ringScale = if (p.ringIndex == 0) 0.35f else if (p.ringIndex == 1) 0.65f else 0.95f
        val currentAngleDeg = p.baseAngle + (clockSec * 1.8f * (if (p.ringIndex % 2 == 0) 1f else -1f))
        val rad = Math.toRadians(currentAngleDeg.toDouble())

        // Structured Mandala Position
        val leafBaseR = maxRadius * ringScale * (0.22f + 0.68f * expansion)
        val flutter = sin(clockSec * p.flutterSpeed + p.phase) * (4f + 4f * expansion)
        val leafSweep = sin(p.tOnLeaf * PI.toFloat()) * (maxRadius * 0.12f * ringScale * expansion)

        val structX = center.x + cos(rad).toFloat() * (leafBaseR * p.tOnLeaf) + cos(rad + PI / 2).toFloat() * (leafSweep + flutter)
        val structY = center.y + sin(rad).toFloat() * (leafBaseR * p.tOnLeaf) + sin(rad + PI / 2).toFloat() * (leafSweep + flutter)

        // Dispersed Spiral Nebula Position (on Exhale)
        val dispersionFactor = (1.40f - expansion * 0.65f)
        val dispAngle = p.dispersionAngle + (clockSec * 0.15f * p.dispersionSpeed)
        val dispR = maxRadius * p.dispersionDistance * dispersionFactor + sin(clockSec * p.dispersionSpeed + p.phase) * 6f
        val dispX = center.x + cos(dispAngle).toFloat() * dispR
        val dispY = center.y + sin(dispAngle).toFloat() * dispR

        val disperseT = if (isExhale) (1.0f - (expansion - 0.24f) / 0.76f).coerceIn(0f, 1f) else 0f
        val finalX = structX + (dispX - structX) * disperseT
        val finalY = structY + (dispY - structY) * disperseT

        val alpha = if (isExhale) {
            (0.35f + (1f - expansion) * 0.60f).coerceIn(0.2f, 0.95f)
        } else {
            (0.40f + expansion * 0.55f).coerceIn(0.2f, 0.95f)
        }

        if (p.isLeafBlade) {
            // Pointed Bodhi / Bamboo Leaf Blade
            val rot = currentAngleDeg + 90f + flutter * 2f
            rotate(degrees = rot, pivot = Offset(finalX, finalY)) {
                path.reset()
                val size = p.size * (0.80f + 0.35f * expansion)
                val halfW = size * 0.28f
                val halfH = size * 0.85f

                path.moveTo(finalX, finalY - halfH)
                path.cubicTo(
                    finalX + halfW * 1.15f, finalY - halfH * 0.35f,
                    finalX + halfW * 0.85f, finalY + halfH * 0.35f,
                    finalX, finalY + halfH
                )
                path.cubicTo(
                    finalX - halfW * 0.85f, finalY + halfH * 0.35f,
                    finalX - halfW * 1.15f, finalY - halfH * 0.35f,
                    finalX, finalY - halfH
                )
                path.close()

                drawPath(path = path, color = p.color.copy(alpha = alpha))
                drawPath(path = path, color = primaryLight.copy(alpha = alpha * 0.75f), style = Stroke(width = 1.0f))
            }
        } else {
            // Jade Stardust Firefly
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * (0.85f + 0.30f * expansion),
                center = Offset(finalX, finalY)
            )
        }
    }
}

/**
 * 🪷 Theme 2: Sacred Lotus Particle Mandala & Cosmic Dispersion
 * 180 luminous stardust particles forming a golden lotus mandala on Inhale,
 * and bursting apart into a cosmic nebula vortex on Exhale.
 */
private fun DrawScope.drawLotusParticleMandala(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    isExhale: Boolean,
    particles: List<LotusMandalaParticle>,
    clockSec: Float,
    path: Path
) {
    particles.forEach { p ->
        val petalsInRing = if (p.ringIndex == 0) 6 else if (p.ringIndex == 1) 8 else 12
        val basePetalAngleDeg = (p.petalIndex.toFloat() / petalsInRing.toFloat()) * 360f + (p.ringIndex * 22.5f) + (clockSec * 2.0f)
        val ringScale = if (p.ringIndex == 0) 0.35f else if (p.ringIndex == 1) 0.65f else 0.95f

        val petalAngleRad = Math.toRadians(basePetalAngleDeg.toDouble())
        val petalBaseRadius = maxRadius * ringScale * (0.20f + 0.70f * expansion)
        val petalSweep = sin(p.tOnPetal * PI.toFloat()) * (maxRadius * 0.16f * ringScale * expansion)
        
        val structuredX = center.x + cos(petalAngleRad).toFloat() * (petalBaseRadius * p.tOnPetal) + cos(petalAngleRad + PI / 2).toFloat() * petalSweep
        val structuredY = center.y + sin(petalAngleRad).toFloat() * (petalBaseRadius * p.tOnPetal) + sin(petalAngleRad + PI / 2).toFloat() * petalSweep

        val dispersionFactor = (1.45f - expansion * 0.70f)
        val dispAngle = p.dispersionAngle + (clockSec * 0.18f * p.dispersionSpeed)
        val dispR = maxRadius * p.dispersionDistance * dispersionFactor + sin(clockSec * p.dispersionSpeed + p.phase) * 8f
        val dispersedX = center.x + cos(dispAngle).toFloat() * dispR
        val dispersedY = center.y + sin(dispAngle).toFloat() * dispR

        val disperseT = if (isExhale) (1.0f - (expansion - 0.24f) / 0.76f).coerceIn(0f, 1f) else 0f
        val finalX = structuredX + (dispersedX - structuredX) * disperseT
        val finalY = structuredY + (dispersedY - structuredY) * disperseT

        val alpha = if (isExhale) {
            (0.35f + (1f - expansion) * 0.60f).coerceIn(0.2f, 0.95f)
        } else {
            (0.40f + expansion * 0.55f).coerceIn(0.2f, 0.95f)
        }

        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = p.size * (0.85f + 0.35f * expansion),
            center = Offset(finalX, finalY)
        )
    }
}

/**
 * 🌸 Theme 3: Sakura Fibonacci Twilight Vortex
 * 160 radiant cherry blossom particles & floating petal blades orbiting in golden ratio Fibonacci spirals.
 */
private fun DrawScope.drawSakuraFibonacciVortex(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    particles: List<SakuraVortexParticle>,
    clockSec: Float,
    primaryLight: Color,
    path: Path
) {
    particles.forEach { p ->
        val spiralArmAngle = (p.armIndex * 72f) + (p.radiusRatio * 180f)
        val currentAngle = Math.toRadians((spiralArmAngle + (clockSec * 16f * p.orbitSpeed)).toDouble())
        val dynamicR = maxRadius * p.radiusRatio * (0.28f + 0.72f * expansion) + sin(clockSec * 1.5f + p.phase) * 6f
        val pX = center.x + cos(currentAngle).toFloat() * dynamicR
        val pY = center.y + sin(currentAngle).toFloat() * dynamicR

        val alpha = (0.40f + 0.55f * expansion).coerceIn(0.2f, 0.95f)

        if (p.isPetalBlade) {
            val rot = Math.toDegrees(currentAngle).toFloat() + 90f + (clockSec * 25f * p.spinSpeed)
            rotate(degrees = rot, pivot = Offset(pX, pY)) {
                path.reset()
                val size = p.size * (0.85f + 0.30f * expansion)
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

                drawPath(path = path, color = p.color.copy(alpha = alpha))
                drawPath(path = path, color = primaryLight.copy(alpha = alpha * 0.7f), style = Stroke(width = 1.0f))
            }
        } else {
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * (0.85f + 0.30f * expansion),
                center = Offset(pX, pY)
            )
        }
    }
}

/**
 * 🌠 Ethereal Slow Breathing Comets / Shooting Stars
 * Gracefully weaves through and between other objects along breathing orbital paths with luminous fading streamer trails.
 */
private fun DrawScope.drawBreathingCometStreamers(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    comets: List<BreathingComet>,
    clockSec: Float,
    primaryLight: Color
) {
    comets.forEach { comet ->
        // Breathing orbital dynamics: radius expands/contracts in unison with breath
        val currentRadiusRatio = comet.minRadiusRatio + (comet.maxRadiusRatio - comet.minRadiusRatio) * expansion
        val baseR = maxRadius * currentRadiusRatio
        val angleSpeed = comet.orbitSpeed * (0.85f + 0.30f * expansion)

        var prevX = 0f
        var prevY = 0f

        // Draw Fading Trailing Tail Streamer (7 trailing steps)
        for (step in comet.trailLength downTo 0) {
            val lagSec = step * 0.055f
            val historicClock = clockSec - lagSec
            val theta = comet.baseAngle + (historicClock * angleSpeed) + sin(historicClock * 0.6f + comet.radialPhase) * 0.35f
            val r = baseR + sin(historicClock * 0.8f + comet.radialPhase) * (maxRadius * 0.08f * expansion)

            val x = center.x + cos(theta) * r
            val y = center.y + sin(theta) * r

            if (step < comet.trailLength) {
                val trailT = 1.0f - (step.toFloat() / comet.trailLength.toFloat())
                val trailAlpha = (trailT * (0.35f + 0.40f * expansion)).coerceIn(0f, 0.75f)
                val strokeW = comet.size * (0.4f + trailT * 0.8f)

                drawLine(
                    color = comet.color.copy(alpha = trailAlpha),
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            prevX = x
            prevY = y
        }

        // Draw Luminous Shooting Star Head
        val headClock = clockSec
        val headTheta = comet.baseAngle + (headClock * angleSpeed) + sin(headClock * 0.6f + comet.radialPhase) * 0.35f
        val headR = baseR + sin(headClock * 0.8f + comet.radialPhase) * (maxRadius * 0.08f * expansion)
        val headX = center.x + cos(headTheta) * headR
        val headY = center.y + sin(headTheta) * headR

        // Radiant comet halo & core
        drawCircle(
            color = comet.color.copy(alpha = (0.50f + 0.40f * expansion).coerceIn(0.2f, 0.95f)),
            radius = comet.size * (0.90f + 0.30f * expansion),
            center = Offset(headX, headY)
        )
        drawCircle(
            color = primaryLight.copy(alpha = (0.75f + 0.25f * expansion).coerceIn(0.4f, 1.0f)),
            radius = comet.size * 0.50f,
            center = Offset(headX, headY)
        )
    }
}

