package com.samadhi.meditationtimer.ui.components

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
import androidx.compose.ui.geometry.Size
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Data structure for ambient floating botanical particles (bamboo leaves, lotus petals, sakura petals).
 */
private data class AmbientBotanicalParticle(
    val relX: Float,
    val relY: Float,
    val speedY: Float,
    val driftSpeedX: Float,
    val baseScale: Float,
    val initialRotation: Float,
    val rotSpeed: Float,
    val alphaBase: Float,
    val orbitOffset: Float
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier
) {
    val zenTheme = LocalZenTheme.current

    // Smooth ambient time clock for organic motion and pulses
    val infiniteTransition = rememberInfiniteTransition(label = "ZenCanvasClock")
    val animClockSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ZenAnimClock"
    )

    // Precompute bounded ambient botanical particles
    val ambientParticles = remember {
        val rand = Random(42)
        List(26) {
            AmbientBotanicalParticle(
                relX = 0.12f + rand.nextFloat() * 0.76f,
                relY = 0.10f + rand.nextFloat() * 0.80f,
                speedY = 0.012f + rand.nextFloat() * 0.022f,
                driftSpeedX = 0.35f + rand.nextFloat() * 0.75f,
                baseScale = 0.60f + rand.nextFloat() * 0.55f,
                initialRotation = rand.nextFloat() * 360f,
                rotSpeed = (rand.nextFloat() - 0.5f) * 35f,
                alphaBase = 0.30f + rand.nextFloat() * 0.40f,
                orbitOffset = rand.nextFloat() * 360f
            )
        }
    }

    // Continuous breath growth factor [0.0f .. 1.0f] with natural cubic ease
    val rawGrowth = when (currentPhase) {
        BreathPhase.INHALE -> phaseProgress
        BreathPhase.HOLD_FULL -> 1.0f
        BreathPhase.EXHALE -> (1.0f - phaseProgress)
        BreathPhase.HOLD_EMPTY -> 0.0f
    }
    // Smooth cosine-based easing for ultra-fluid expansion & contraction
    val smoothedGrowth = (1.0f - cos(rawGrowth * Math.PI.toFloat())) / 2.0f

    // Hold subtle living pulse
    val holdPulse = if (currentPhase == BreathPhase.HOLD_FULL) {
        sin(animClockSec * 2.8f) * 0.022f
    } else if (currentPhase == BreathPhase.HOLD_EMPTY) {
        sin(animClockSec * 1.5f) * 0.010f
    } else {
        0.0f
    }

    val finalGrowth = (smoothedGrowth + holdPulse).coerceIn(0.0f, 1.05f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val minDim = minOf(width, height)
        val center = Offset(width / 2f, height / 2f)

        val minRadius = minDim * 0.16f
        val maxRadius = minDim * 0.34f
        val currentRadius = minRadius + (maxRadius - minRadius) * finalGrowth
        val trackRadius = minDim * 0.40f

        when (zenTheme.id) {
            ZenThemeId.BAMBOO_TEAL -> {
                drawFluidBambooBreathing(
                    center = center,
                    minDim = minDim,
                    radius = currentRadius,
                    trackRadius = trackRadius,
                    growthFactor = finalGrowth,
                    currentPhase = currentPhase,
                    phaseProgress = phaseProgress,
                    animClockSec = animClockSec,
                    primaryColor = zenTheme.primary,
                    primaryLight = zenTheme.primaryLight,
                    primaryDark = zenTheme.primaryDark,
                    ambientParticles = ambientParticles
                )
            }
            ZenThemeId.SACRED_LOTUS -> {
                drawFluidLotusBreathing(
                    center = center,
                    minDim = minDim,
                    radius = currentRadius,
                    trackRadius = trackRadius,
                    growthFactor = finalGrowth,
                    currentPhase = currentPhase,
                    phaseProgress = phaseProgress,
                    animClockSec = animClockSec,
                    primaryColor = zenTheme.primary,
                    primaryLight = zenTheme.primaryLight,
                    primaryDark = zenTheme.primaryDark,
                    ambientParticles = ambientParticles
                )
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                drawFluidSakuraBreathing(
                    center = center,
                    minDim = minDim,
                    radius = currentRadius,
                    trackRadius = trackRadius,
                    growthFactor = finalGrowth,
                    currentPhase = currentPhase,
                    phaseProgress = phaseProgress,
                    animClockSec = animClockSec,
                    primaryColor = zenTheme.primary,
                    primaryLight = zenTheme.primaryLight,
                    primaryDark = zenTheme.primaryDark,
                    ambientParticles = ambientParticles
                )
            }
        }
    }
}

// =========================================================================
// 1. MOONLIT BAMBOO: FLUID LIVING BUBBLE + CALM NOTCH BEAD + JADE LEAF SWIRL
// =========================================================================

private fun DrawScope.drawFluidBambooBreathing(
    center: Offset,
    minDim: Float,
    radius: Float,
    trackRadius: Float,
    growthFactor: Float,
    currentPhase: BreathPhase,
    phaseProgress: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    ambientParticles: List<AmbientBotanicalParticle>
) {
    // 1. Ambient Floating & Drifting Bamboo Leaves
    drawAmbientBotanicals(
        particles = ambientParticles,
        minDim = minDim,
        animClockSec = animClockSec,
        growthFactor = growthFactor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    ) { pCenter, pScale, pRot, pColor, pRim ->
        drawSingleBambooLeaf(
            center = pCenter,
            scale = pScale * 0.70f,
            rotation = pRot,
            fillColor = pColor,
            rimColor = pRim
        )
    }

    // 2. Multi-Layered Luminous Core Breathing Bubble (No static track line)
    drawCalmBubbleLayers(
        center = center,
        radius = radius,
        growthFactor = growthFactor,
        animClockSec = animClockSec,
        primaryColor = primaryColor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )

    // 3. Fluid Organic Bamboo Leaves Swirling with the Breath
    val leafCount = 7
    for (i in 0 until leafCount) {
        val baseAngle = (i * (360f / leafCount))
        val dynamicAngle = baseAngle + animClockSec * 3.5f + sin(animClockSec * 1.5f + i) * 12f
        val rad = Math.toRadians(dynamicAngle.toDouble()).toFloat()

        // Dynamic fluid radius that breathes and sways organically
        val r = radius * (0.85f + 0.25f * growthFactor) + sin(animClockSec * 2f + i * 2f) * (minDim * 0.015f)
        val leafX = center.x + cos(rad) * r
        val leafY = center.y + sin(rad) * r

        val leafRot = dynamicAngle + 70f + sin(animClockSec * 1.8f + i) * 15f
        val leafScale = (minDim / 360f) * (0.55f + 0.35f * growthFactor)

        drawSingleBambooLeaf(
            center = Offset(leafX, leafY),
            scale = leafScale,
            rotation = leafRot,
            fillColor = primaryDark,
            rimColor = primaryLight
        )
    }

    // 4. Calm Milestone Notches & Traveling Half-Circle Indicator Bead
    drawCalmPhaseNotchesAndBead(
        center = center,
        trackRadius = trackRadius,
        currentPhase = currentPhase,
        phaseProgress = phaseProgress,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )
}

// =========================================================================
// 2. SACRED LOTUS: FLUID LIVING BUBBLE + CALM NOTCH BEAD + GOLDEN PETAL MANDALA
// =========================================================================

private fun DrawScope.drawFluidLotusBreathing(
    center: Offset,
    minDim: Float,
    radius: Float,
    trackRadius: Float,
    growthFactor: Float,
    currentPhase: BreathPhase,
    phaseProgress: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    ambientParticles: List<AmbientBotanicalParticle>
) {
    // 1. Ambient Floating Lotus Petals
    drawAmbientBotanicals(
        particles = ambientParticles,
        minDim = minDim,
        animClockSec = animClockSec,
        growthFactor = growthFactor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    ) { pCenter, pScale, pRot, pColor, pRim ->
        drawSingleLotusPetal(
            tip = pCenter,
            scale = pScale * 0.65f,
            rotation = pRot,
            petalColor = pColor,
            rimColor = pRim
        )
    }

    // 2. Multi-Layered Luminous Core Breathing Bubble
    drawCalmBubbleLayers(
        center = center,
        radius = radius,
        growthFactor = growthFactor,
        animClockSec = animClockSec,
        primaryColor = primaryColor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )

    // 3. Fluid Organic Lotus Petal Blossom Swirl
    val petalCount = 8
    for (i in 0 until petalCount) {
        val baseAngle = (i * (360f / petalCount))
        val dynamicAngle = baseAngle + animClockSec * 2.5f + sin(animClockSec * 1.2f + i) * 8f
        val rad = Math.toRadians(dynamicAngle.toDouble()).toFloat()

        val r = radius * (0.80f + 0.28f * growthFactor)
        val anchorX = center.x + cos(rad) * r
        val anchorY = center.y + sin(rad) * r

        val petalRot = dynamicAngle - 90f + sin(animClockSec * 1.4f + i) * 10f
        val petalScale = (minDim / 350f) * (0.60f + 0.35f * growthFactor)

        drawSingleLotusPetal(
            tip = Offset(anchorX, anchorY),
            scale = petalScale,
            rotation = petalRot,
            petalColor = if (i % 2 == 0) primaryDark else primaryColor,
            rimColor = primaryLight
        )
    }

    // 4. Calm Milestone Notches & Traveling Half-Circle Indicator Bead
    drawCalmPhaseNotchesAndBead(
        center = center,
        trackRadius = trackRadius,
        currentPhase = currentPhase,
        phaseProgress = phaseProgress,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )
}

// =========================================================================
// 3. SAKURA TWILIGHT: FLUID LIVING BUBBLE + CALM NOTCH BEAD + CHERRY PETAL VORTEX
// =========================================================================

private fun DrawScope.drawFluidSakuraBreathing(
    center: Offset,
    minDim: Float,
    radius: Float,
    trackRadius: Float,
    growthFactor: Float,
    currentPhase: BreathPhase,
    phaseProgress: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    ambientParticles: List<AmbientBotanicalParticle>
) {
    // 1. Ambient Floating Sakura Petals
    drawAmbientBotanicals(
        particles = ambientParticles,
        minDim = minDim,
        animClockSec = animClockSec,
        growthFactor = growthFactor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    ) { pCenter, pScale, pRot, pColor, pRim ->
        drawSingleSakuraPetal(
            center = pCenter,
            scale = pScale * 0.75f,
            rotation = pRot,
            petalColor = pColor,
            rimColor = pRim
        )
    }

    // 2. Multi-Layered Luminous Core Breathing Bubble
    drawCalmBubbleLayers(
        center = center,
        radius = radius,
        growthFactor = growthFactor,
        animClockSec = animClockSec,
        primaryColor = primaryColor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )

    // 3. Fluid Organic Cherry Blossom Florets & Petals Swirling in Vortex
    val floretCount = 5
    for (i in 0 until floretCount) {
        val baseAngle = (i * (360f / floretCount))
        val dynamicAngle = baseAngle + animClockSec * 3.0f + sin(animClockSec * 1.5f + i) * 10f
        val rad = Math.toRadians(dynamicAngle.toDouble()).toFloat()

        val r = radius * (0.88f + 0.22f * growthFactor)
        val fx = center.x + cos(rad) * r
        val fy = center.y + sin(rad) * r

        val floretScale = (minDim / 370f) * (0.50f + 0.45f * growthFactor)

        drawSakura5PetalFlower(
            origin = Offset(fx, fy),
            scale = floretScale,
            bloomFactor = growthFactor,
            animClockSec = animClockSec + i,
            primaryColor = primaryColor,
            primaryLight = primaryLight,
            primaryDark = primaryDark
        )
    }

    // 4. Calm Milestone Notches & Traveling Half-Circle Indicator Bead
    drawCalmPhaseNotchesAndBead(
        center = center,
        trackRadius = trackRadius,
        currentPhase = currentPhase,
        phaseProgress = phaseProgress,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )
}

// =========================================================================
// CALM MILESTONE NOTCHES & TRAVELING HALF-CIRCLE INDICATOR BEAD
// =========================================================================

private fun DrawScope.drawCalmPhaseNotchesAndBead(
    center: Offset,
    trackRadius: Float,
    currentPhase: BreathPhase,
    phaseProgress: Float,
    primaryLight: Color,
    primaryDark: Color
) {
    // 4 Phase Stop Angles (degrees, Top=-90, Right=0, Bottom=90, Left=180)
    // Inhale: -90° ➔ 0° (Top to Right)
    // Hold Full: 0° ➔ 90° (Right to Bottom)
    // Exhale: 90° ➔ 180° (Bottom to Left)
    // Hold Empty: 180° ➔ 270° (-90°) (Left to Top)
    val notchAngles = listOf(-90f, 0f, 90f, 180f)

    // 1. Draw Phase Milestone Notches (Capsule radial marks)
    val notchLen = 14f
    val notchWidth = 3.5f
    for (notchAngle in notchAngles) {
        val nRad = Math.toRadians(notchAngle.toDouble()).toFloat()
        val cosA = cos(nRad)
        val sinA = sin(nRad)

        val innerX = center.x + cosA * (trackRadius - notchLen / 2f)
        val innerY = center.y + sinA * (trackRadius - notchLen / 2f)
        val outerX = center.x + cosA * (trackRadius + notchLen / 2f)
        val outerY = center.y + sinA * (trackRadius + notchLen / 2f)

        // Radial Notch Tick Mark
        drawLine(
            color = primaryLight.copy(alpha = 0.85f),
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = notchWidth,
            cap = StrokeCap.Round
        )
    }

    // 2. Calculate Continuous Moving Angle for the Traveling Bead
    val (phaseStartAngle, phaseSweep) = when (currentPhase) {
        BreathPhase.INHALE -> Pair(-90f, 90f)
        BreathPhase.HOLD_FULL -> Pair(0f, 90f)
        BreathPhase.EXHALE -> Pair(90f, 90f)
        BreathPhase.HOLD_EMPTY -> Pair(180f, 90f)
    }
    val currentAngleDeg = phaseStartAngle + (phaseSweep * phaseProgress)
    val currentAngleRad = Math.toRadians(currentAngleDeg.toDouble()).toFloat()

    val beadCenterX = center.x + cos(currentAngleRad) * trackRadius
    val beadCenterY = center.y + sin(currentAngleRad) * trackRadius
    val beadCenter = Offset(beadCenterX, beadCenterY)

    // 3. Draw Traveling Half-Circle Capsule Bead (Matches Calm Reference!)
    // The flat side faces center, curved dome faces outward
    rotate(degrees = currentAngleDeg + 90f, pivot = beadCenter) {
        val beadW = 18f
        val beadH = 10f

        val halfCirclePath = Path().apply {
            moveTo(beadCenter.x - beadW / 2f, beadCenter.y) // Left corner of flat base
            lineTo(beadCenter.x + beadW / 2f, beadCenter.y) // Right corner of flat base
            // Curved dome arching upward
            cubicTo(
                beadCenter.x + beadW / 2f, beadCenter.y - beadH * 1.3f,
                beadCenter.x - beadW / 2f, beadCenter.y - beadH * 1.3f,
                beadCenter.x - beadW / 2f, beadCenter.y
            )
            close()
        }

        // Luminous Bead Glow Aura
        drawCircle(
            color = primaryLight.copy(alpha = 0.40f),
            radius = 16f,
            center = beadCenter
        )
        // Bead Fill (Pure radiant light)
        drawPath(
            path = halfCirclePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, primaryLight, primaryDark),
                startY = beadCenter.y - beadH,
                endY = beadCenter.y
            )
        )
        // Bead Border
        drawPath(
            path = halfCirclePath,
            color = Color.White,
            style = Stroke(width = 1.2f)
        )
    }
}

// =========================================================================
// SHARED CALM BREATHE BUBBLE MULTI-LAYERED GLOW RENDERER (NO STATIC LINE)
// =========================================================================

private fun DrawScope.drawCalmBubbleLayers(
    center: Offset,
    radius: Float,
    growthFactor: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    // 1. Outermost Ambient Atmosphere Aura (Diffused soothing light)
    val auraRadius = radius * 1.50f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryLight.copy(alpha = 0.20f + 0.12f * growthFactor),
                primaryColor.copy(alpha = 0.10f + 0.08f * growthFactor),
                primaryDark.copy(alpha = 0.03f),
                Color.Transparent
            ),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center
    )

    // 2. Main Luminous Core Bubble (Smooth radial gradient sphere)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryLight.copy(alpha = 0.45f + 0.35f * growthFactor),
                primaryColor.copy(alpha = 0.30f + 0.30f * growthFactor),
                primaryDark.copy(alpha = 0.40f + 0.25f * growthFactor),
                Color(0x22000000)
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // 3. Bubble Rim Luminous Edge
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                primaryLight.copy(alpha = 0.85f),
                primaryColor.copy(alpha = 0.60f),
                primaryLight.copy(alpha = 0.95f),
                primaryDark.copy(alpha = 0.50f),
                primaryLight.copy(alpha = 0.85f)
            ),
            center = center
        ),
        radius = radius,
        center = center,
        style = Stroke(width = 2.0f)
    )

    // 4. Specular Gloss Reflection Arch (Upper hemisphere highlight for glassmorphism)
    val highlightRadius = radius * 0.70f
    val highlightCenter = Offset(center.x, center.y - radius * 0.25f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.25f + 0.15f * growthFactor),
                primaryLight.copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = highlightCenter,
            radius = highlightRadius
        ),
        radius = highlightRadius,
        center = highlightCenter
    )
}

// =========================================================================
// THEME BOTANICAL DRAWING HELPERS
// =========================================================================

private fun DrawScope.drawSingleBambooLeaf(
    center: Offset,
    scale: Float,
    rotation: Float,
    fillColor: Color,
    rimColor: Color
) {
    rotate(degrees = rotation, pivot = center) {
        val len = scale * 34f
        val wid = scale * 8.5f

        val path = Path().apply {
            moveTo(center.x, center.y)
            cubicTo(
                center.x - wid * 0.8f, center.y + len * 0.35f,
                center.x - wid, center.y + len * 0.75f,
                center.x, center.y + len
            )
            cubicTo(
                center.x + wid, center.y + len * 0.75f,
                center.x + wid * 0.8f, center.y + len * 0.35f,
                center.x, center.y
            )
            close()
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    rimColor.copy(alpha = 0.95f),
                    fillColor.copy(alpha = 0.92f),
                    Color(0xFF0F3025).copy(alpha = 0.90f)
                ),
                startY = center.y,
                endY = center.y + len
            )
        )
        drawLine(
            color = rimColor.copy(alpha = 0.80f),
            start = center,
            end = Offset(center.x, center.y + len * 0.92f),
            strokeWidth = 1.0f
        )
        drawPath(
            path = path,
            color = rimColor.copy(alpha = 0.75f),
            style = Stroke(width = 1.1f)
        )
    }
}

private fun DrawScope.drawSingleLotusPetal(
    tip: Offset,
    scale: Float,
    rotation: Float,
    petalColor: Color,
    rimColor: Color
) {
    rotate(degrees = rotation, pivot = tip) {
        val len = scale * 44f
        val wid = scale * 18f

        val path = Path().apply {
            moveTo(tip.x, tip.y)
            cubicTo(
                tip.x - wid, tip.y + len * 0.45f,
                tip.x - wid * 0.65f, tip.y + len,
                tip.x, tip.y + len
            )
            cubicTo(
                tip.x + wid * 0.65f, tip.y + len,
                tip.x + wid, tip.y + len * 0.45f,
                tip.x, tip.y
            )
            close()
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    petalColor.copy(alpha = 0.92f),
                    rimColor.copy(alpha = 0.90f),
                    Color(0xFFFFF8E7).copy(alpha = 0.95f)
                ),
                startY = tip.y,
                endY = tip.y + len
            )
        )
        drawLine(
            color = rimColor.copy(alpha = 0.70f),
            start = tip,
            end = Offset(tip.x, tip.y + len * 0.90f),
            strokeWidth = 1.0f
        )
        drawPath(
            path = path,
            color = rimColor.copy(alpha = 0.85f),
            style = Stroke(width = 1.2f)
        )
    }
}

private fun DrawScope.drawSakura5PetalFlower(
    origin: Offset,
    scale: Float,
    bloomFactor: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    for (i in 0 until 5) {
        val baseAngle = i * 72f
        val sway = sin(animClockSec * 1.2f + i) * 2f
        val petalRot = baseAngle + sway

        rotate(degrees = petalRot, pivot = origin) {
            val len = scale * 36f * (0.80f + 0.20f * bloomFactor)
            val wid = scale * 18f * (0.85f + 0.15f * bloomFactor)
            val notch = len * 0.16f

            val path = Path().apply {
                moveTo(origin.x, origin.y)
                cubicTo(
                    origin.x - wid * 1.1f, origin.y - len * 0.40f,
                    origin.x - wid * 0.9f, origin.y - len,
                    origin.x - wid * 0.45f, origin.y - len
                )
                quadraticTo(
                    origin.x, origin.y - len + notch,
                    origin.x + wid * 0.45f, origin.y - len
                )
                cubicTo(
                    origin.x + wid * 0.9f, origin.y - len,
                    origin.x + wid * 1.1f, origin.y - len * 0.40f,
                    origin.x, origin.y
                )
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryLight.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.92f),
                        primaryDark.copy(alpha = 0.85f)
                    ),
                    startY = origin.y - len,
                    endY = origin.y
                )
            )
            drawPath(
                path = path,
                color = primaryLight.copy(alpha = 0.88f),
                style = Stroke(width = 1.1f)
            )
        }
    }

    // Stamen Filaments & Core
    val stamenCount = 8
    for (s in 0 until stamenCount) {
        val sAngle = s * (360f / stamenCount)
        val sRad = Math.toRadians(sAngle.toDouble()).toFloat()
        val sLen = scale * 14f * (0.6f + 0.4f * bloomFactor)
        val sx = origin.x + cos(sRad) * sLen
        val sy = origin.y + sin(sRad) * sLen

        drawLine(
            color = primaryLight.copy(alpha = 0.85f),
            start = origin,
            end = Offset(sx, sy),
            strokeWidth = 1.0f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFFFFD1DC),
            radius = scale * 1.8f,
            center = Offset(sx, sy)
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF80AB), primaryColor, primaryDark),
            center = origin,
            radius = scale * 7f
        ),
        radius = scale * 7f,
        center = origin
    )
}

private fun DrawScope.drawSingleSakuraPetal(
    center: Offset,
    scale: Float,
    rotation: Float,
    petalColor: Color,
    rimColor: Color
) {
    rotate(degrees = rotation, pivot = center) {
        val len = scale * 24f
        val wid = scale * 11f
        val notch = len * 0.15f

        val path = Path().apply {
            moveTo(center.x, center.y + len * 0.4f)
            cubicTo(
                center.x - wid * 1.1f, center.y + len * 0.1f,
                center.x - wid * 1.0f, center.y - len * 0.6f,
                center.x - wid * 0.45f, center.y - len
            )
            quadraticTo(
                center.x, center.y - len + notch,
                center.x + wid * 0.45f, center.y - len
            )
            cubicTo(
                center.x + wid * 1.0f, center.y - len * 0.6f,
                center.x + wid * 1.1f, center.y + len * 0.1f,
                center.x, center.y + len * 0.4f
            )
            close()
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    rimColor.copy(alpha = 0.90f),
                    petalColor.copy(alpha = 0.85f)
                ),
                startY = center.y - len,
                endY = center.y + len * 0.4f
            )
        )
        drawPath(path = path, color = rimColor.copy(alpha = 0.70f), style = Stroke(width = 1.0f))
    }
}

// =========================================================================
// AMBIENT BOTANICAL PARTICLES (STRICTLY CONSTRAINED TO VISIBLE SCREEN BOUNDS)
// =========================================================================

private inline fun DrawScope.drawAmbientBotanicals(
    particles: List<AmbientBotanicalParticle>,
    minDim: Float,
    animClockSec: Float,
    growthFactor: Float,
    primaryLight: Color,
    primaryDark: Color,
    drawLeaf: (center: Offset, scale: Float, rotation: Float, fill: Color, rim: Color) -> Unit
) {
    val minX = size.width * 0.08f
    val maxX = size.width * 0.92f
    val minY = size.height * 0.08f
    val maxY = size.height * 0.92f
    val boundW = maxX - minX
    val boundH = maxY - minY

    for (p in particles) {
        val cycleProgress = (animClockSec * p.speedY + p.relY) % 1.0f
        val curY = minY + cycleProgress * boundH

        val driftX = sin(animClockSec * p.driftSpeedX + p.relX * 10f) * (minDim * 0.08f)
        val curX = (minX + (p.relX * boundW) + driftX).coerceIn(minX, maxX)

        val rotation = p.initialRotation + animClockSec * p.rotSpeed
        val dynamicScale = (minDim / 380f) * p.baseScale * (0.80f + 0.20f * growthFactor)

        drawLeaf(
            Offset(curX, curY),
            dynamicScale,
            rotation,
            primaryDark.copy(alpha = p.alphaBase),
            primaryLight.copy(alpha = p.alphaBase + 0.15f)
        )
    }
}
