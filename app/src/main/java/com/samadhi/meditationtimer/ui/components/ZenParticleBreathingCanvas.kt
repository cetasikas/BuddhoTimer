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
 * Static metadata for an Ensō garland unit (leaf, petal, or blossom)
 * positioned along the sacred Zen circle.
 */
private data class EnsoUnit(
    val index: Int,
    val angleDeg: Float,         // 0° = Top (-90° standard), sweeps clockwise to 360°
    val radiusJitter: Float,     // Organic variation (-0.06f .. +0.06f)
    val angleJitter: Float,      // Natural angle sway
    val sizeScale: Float,        // Variable brushstroke thickness
    val rotationOffset: Float,   // Tangent orientation offset
    val driftAngle: Float,       // Direction when unmaking on exhale
    val driftSpeed: Float,
    val colorVariant: Float      // Shading variation
)

/**
 * Ambient background floating mists/embers for tranquil visual depth.
 */
private data class AmbientDriftParticle(
    val relX: Float,
    val relY: Float,
    val speedY: Float,
    val driftFreq: Float,
    val scale: Float,
    val baseAlpha: Float
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier
) {
    val zenTheme = LocalZenTheme.current

    // Smooth ambient clock for organic swaying and breathing illumination
    val infiniteTransition = rememberInfiniteTransition(label = "EnsoClock")
    val animClockSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EnsoTime"
    )

    // Precompute 42 organic Ensō units around the circumference
    val ensoUnits = remember {
        val count = 42
        val rand = Random(108) // Auspicious seed
        List(count) { i ->
            val fraction = i.toFloat() / count.toFloat()
            // Ensō brushstroke variation: thicker and wider in middle of stroke, elegant at endpoints
            val strokeThickness = 0.85f + 0.35f * sin(fraction * Math.PI.toFloat())
            EnsoUnit(
                index = i,
                angleDeg = fraction * 360f,
                radiusJitter = (rand.nextFloat() - 0.5f) * 0.08f,
                angleJitter = (rand.nextFloat() - 0.5f) * 4f,
                sizeScale = strokeThickness * (0.80f + rand.nextFloat() * 0.40f),
                rotationOffset = (rand.nextFloat() - 0.5f) * 24f,
                driftAngle = (rand.nextFloat() * 360f),
                driftSpeed = 0.6f + rand.nextFloat() * 0.8f,
                colorVariant = rand.nextFloat()
            )
        }
    }

    // Precompute subtle ambient background dust/embers
    val ambientMists = remember {
        val rand = Random(42)
        List(18) {
            AmbientDriftParticle(
                relX = 0.10f + rand.nextFloat() * 0.80f,
                relY = 0.10f + rand.nextFloat() * 0.80f,
                speedY = 0.015f + rand.nextFloat() * 0.020f,
                driftFreq = 0.4f + rand.nextFloat() * 0.8f,
                scale = 0.5f + rand.nextFloat() * 0.6f,
                baseAlpha = 0.15f + rand.nextFloat() * 0.20f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val minDim = minOf(width, height)
        val center = Offset(width / 2f, height / 2f)
        val ensoBaseRadius = minDim * 0.33f

        // Draw ambient tranquil background mist
        drawAmbientMists(ambientMists, minDim, animClockSec, zenTheme.primaryLight)

        // Draw the living Zen Ensō based on active theme
        drawZenEnsoGarland(
            ensoUnits = ensoUnits,
            center = center,
            baseRadius = ensoBaseRadius,
            minDim = minDim,
            currentPhase = currentPhase,
            phaseProgress = phaseProgress,
            animClockSec = animClockSec,
            themeId = zenTheme.id,
            primary = zenTheme.primary,
            primaryLight = zenTheme.primaryLight,
            primaryDark = zenTheme.primaryDark
        )
    }
}

// =========================================================================
// ZEN ENSŌ DRAWING & UNMAKING ENGINE
// =========================================================================

private fun DrawScope.drawZenEnsoGarland(
    ensoUnits: List<EnsoUnit>,
    center: Offset,
    baseRadius: Float,
    minDim: Float,
    currentPhase: BreathPhase,
    phaseProgress: Float,
    animClockSec: Float,
    themeId: ZenThemeId,
    primary: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    val totalUnits = ensoUnits.size

    // Calculate how much of the Ensō is formed / active (0.0 to 1.0)
    // Inhale: 0.0 ➔ 1.0 (Clockwise sweep making the circle)
    // Hold Full: 1.0 (Complete glowing circle)
    // Exhale: 1.0 ➔ 0.0 (Progressive unmaking / dissolving inward)
    // Hold Empty: 0.0 (Pure peaceful void)
    val ensoFormFactor = when (currentPhase) {
        BreathPhase.INHALE -> phaseProgress
        BreathPhase.HOLD_FULL -> 1.0f
        BreathPhase.EXHALE -> (1.0f - phaseProgress)
        BreathPhase.HOLD_EMPTY -> 0.0f
    }

    // Central Core Aura when Ensō is holding or blooming
    if (ensoFormFactor > 0.05f) {
        val coreGlowRadius = baseRadius * (0.85f * ensoFormFactor)
        val glowAlpha = when (currentPhase) {
            BreathPhase.INHALE -> 0.15f * phaseProgress
            BreathPhase.HOLD_FULL -> 0.22f + 0.06f * sin(animClockSec * 2.5f)
            BreathPhase.EXHALE -> 0.15f * (1.0f - phaseProgress)
            BreathPhase.HOLD_EMPTY -> 0.0f
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryLight.copy(alpha = glowAlpha),
                    primary.copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                ),
                center = center,
                radius = coreGlowRadius
            ),
            radius = coreGlowRadius,
            center = center
        )
    }

    // Render each unit along the Ensō ring
    for (unit in ensoUnits) {
        val unitFraction = unit.index.toFloat() / totalUnits.toFloat()

        // Determine this unit's presence and animation state
        var unitAlpha = 0.0f
        var unitScaleMult = 1.0f
        var driftOffsetX = 0.0f
        var driftOffsetY = 0.0f
        var extraRotation = 0.0f

        when (currentPhase) {
            BreathPhase.INHALE -> {
                // Sweep clockwise: unit appears when inhale progress reaches its fraction
                val appearanceStart = unitFraction * 0.88f // finishes drawing near 90% of inhale
                if (phaseProgress >= appearanceStart) {
                    val unitProgress = ((phaseProgress - appearanceStart) / 0.12f).coerceIn(0.0f, 1.0f)
                    // Organic spring bloom
                    unitAlpha = unitProgress
                    unitScaleMult = 0.3f + 0.7f * unitProgress
                    extraRotation = (1.0f - unitProgress) * 30f
                }
            }
            BreathPhase.HOLD_FULL -> {
                // Complete Ensō with soft harmonic sway
                unitAlpha = 1.0f
                unitScaleMult = 1.0f + 0.04f * sin(animClockSec * 2.0f + unit.index)
                extraRotation = sin(animClockSec * 1.5f + unit.index) * 4f
            }
            BreathPhase.EXHALE -> {
                // Unmaking / Dissolving the circle:
                // Units detach in order or dissolve gently inward
                val detachStart = (1.0f - unitFraction) * 0.75f
                if (phaseProgress >= detachStart) {
                    val dissolveProgress = ((phaseProgress - detachStart) / 0.25f).coerceIn(0.0f, 1.0f)
                    unitAlpha = (1.0f - dissolveProgress)

                    // Drift gently inward toward center with natural swirl
                    val driftRad = Math.toRadians(unit.driftAngle.toDouble() + animClockSec * 20.0).toFloat()
                    val driftDist = minDim * 0.14f * dissolveProgress * unit.driftSpeed
                    driftOffsetX = cos(driftRad) * driftDist
                    driftOffsetY = sin(driftRad) * driftDist - (minDim * 0.03f * dissolveProgress) // subtle upward wind
                    extraRotation = dissolveProgress * 45f * if (unit.index % 2 == 0) 1f else -1f
                    unitScaleMult = (1.0f - dissolveProgress * 0.5f)
                } else {
                    unitAlpha = 1.0f
                }
            }
            BreathPhase.HOLD_EMPTY -> {
                // Pure stillness
                unitAlpha = 0.0f
            }
        }

        if (unitAlpha <= 0.01f) continue

        // Base Position along the Ensō ring (0° = Top at -90°)
        val baseAngleDeg = -90f + unit.angleDeg + unit.angleJitter + (sin(animClockSec * 1.2f + unit.index) * 2f)
        val angleRad = Math.toRadians(baseAngleDeg.toDouble()).toFloat()

        val r = baseRadius * (1.0f + unit.radiusJitter)
        val posX = center.x + cos(angleRad) * r + driftOffsetX
        val posY = center.y + sin(angleRad) * r + driftOffsetY
        val unitCenter = Offset(posX, posY)

        // Tangent rotation along the circle path
        val tangentRotation = baseAngleDeg + 90f + unit.rotationOffset + extraRotation
        val finalScale = (minDim / 360f) * unit.sizeScale * unitScaleMult

        // Render theme-specific artistic botanical unit
        when (themeId) {
            ZenThemeId.BAMBOO_TEAL -> {
                drawEnsoBambooLeaf(
                    center = unitCenter,
                    scale = finalScale,
                    rotation = tangentRotation,
                    alpha = unitAlpha,
                    colorVariant = unit.colorVariant,
                    primary = primary,
                    primaryLight = primaryLight,
                    primaryDark = primaryDark
                )
            }
            ZenThemeId.SACRED_LOTUS -> {
                drawEnsoLotusPetal(
                    center = unitCenter,
                    scale = finalScale,
                    rotation = tangentRotation,
                    alpha = unitAlpha,
                    colorVariant = unit.colorVariant,
                    primary = primary,
                    primaryLight = primaryLight,
                    primaryDark = primaryDark
                )
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                drawEnsoSakuraPetal(
                    center = unitCenter,
                    scale = finalScale,
                    rotation = tangentRotation,
                    alpha = unitAlpha,
                    colorVariant = unit.colorVariant,
                    isFloret = (unit.index % 6 == 0),
                    primary = primary,
                    primaryLight = primaryLight,
                    primaryDark = primaryDark
                )
            }
        }
    }

    // Leading Brush-Tip Ember during Inhalation
    if (currentPhase == BreathPhase.INHALE && phaseProgress in 0.02f..0.98f) {
        val sweepDeg = -90f + (phaseProgress * 360f)
        val sweepRad = Math.toRadians(sweepDeg.toDouble()).toFloat()
        val tipX = center.x + cos(sweepRad) * baseRadius
        val tipY = center.y + sin(sweepRad) * baseRadius
        val tipOffset = Offset(tipX, tipY)

        // Radiant brushstroke tip glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.90f),
                    primaryLight.copy(alpha = 0.70f),
                    primary.copy(alpha = 0.20f),
                    Color.Transparent
                ),
                center = tipOffset,
                radius = minDim * 0.06f
            ),
            radius = minDim * 0.06f,
            center = tipOffset
        )
    }
}

// =========================================================================
// 1. MOONLIT BAMBOO ENSŌ LEAF (ARTISTIC CALLIGRAPHIC JADE LEAF)
// =========================================================================

private fun DrawScope.drawEnsoBambooLeaf(
    center: Offset,
    scale: Float,
    rotation: Float,
    alpha: Float,
    colorVariant: Float,
    primary: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    rotate(degrees = rotation, pivot = center) {
        val len = scale * 38f
        val wid = scale * 9.5f

        val path = Path().apply {
            moveTo(center.x, center.y - len * 0.45f)
            cubicTo(
                center.x - wid * 1.1f, center.y - len * 0.15f,
                center.x - wid * 0.9f, center.y + len * 0.30f,
                center.x, center.y + len * 0.55f
            )
            cubicTo(
                center.x + wid * 0.9f, center.y + len * 0.30f,
                center.x + wid * 1.1f, center.y - len * 0.15f,
                center.x, center.y - len * 0.45f
            )
            close()
        }

        val leafGreen = if (colorVariant > 0.5f) primary else primaryDark
        val glowRim = primaryLight

        // Body Gradient
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    glowRim.copy(alpha = 0.95f * alpha),
                    leafGreen.copy(alpha = 0.90f * alpha),
                    Color(0xFF07241A).copy(alpha = 0.85f * alpha)
                ),
                startY = center.y - len * 0.45f,
                endY = center.y + len * 0.55f
            )
        )

        // Midrib Central Spine Line
        drawLine(
            color = glowRim.copy(alpha = 0.80f * alpha),
            start = Offset(center.x, center.y - len * 0.40f),
            end = Offset(center.x, center.y + len * 0.50f),
            strokeWidth = 1.1f,
            cap = StrokeCap.Round
        )

        // Soft Outer Rim Glow
        drawPath(
            path = path,
            color = glowRim.copy(alpha = 0.65f * alpha),
            style = Stroke(width = 1.0f)
        )
    }
}

// =========================================================================
// 2. SACRED LOTUS ENSŌ PETAL (WARM GOLDEN & CORAL SACRED GARLAND)
// =========================================================================

private fun DrawScope.drawEnsoLotusPetal(
    center: Offset,
    scale: Float,
    rotation: Float,
    alpha: Float,
    colorVariant: Float,
    primary: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    rotate(degrees = rotation, pivot = center) {
        val len = scale * 40f
        val wid = scale * 16f

        val path = Path().apply {
            moveTo(center.x, center.y - len * 0.50f)
            cubicTo(
                center.x - wid * 1.0f, center.y - len * 0.10f,
                center.x - wid * 0.7f, center.y + len * 0.45f,
                center.x, center.y + len * 0.50f
            )
            cubicTo(
                center.x + wid * 0.7f, center.y + len * 0.45f,
                center.x + wid * 1.0f, center.y - len * 0.10f,
                center.x, center.y - len * 0.50f
            )
            close()
        }

        val baseColor = if (colorVariant > 0.5f) primary else primaryDark

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF8E7).copy(alpha = 0.95f * alpha),
                    primaryLight.copy(alpha = 0.90f * alpha),
                    baseColor.copy(alpha = 0.85f * alpha)
                ),
                startY = center.y - len * 0.50f,
                endY = center.y + len * 0.50f
            )
        )

        // Center spine reflection
        drawLine(
            color = Color.White.copy(alpha = 0.75f * alpha),
            start = Offset(center.x, center.y - len * 0.45f),
            end = Offset(center.x, center.y + len * 0.40f),
            strokeWidth = 1.0f
        )

        // Golden rim
        drawPath(
            path = path,
            color = primaryLight.copy(alpha = 0.80f * alpha),
            style = Stroke(width = 1.1f)
        )
    }
}

// =========================================================================
// 3. SAKURA TWILIGHT ENSŌ PETAL & FLORET (HEART-NOTCHED SAKURA WREATH)
// =========================================================================

private fun DrawScope.drawEnsoSakuraPetal(
    center: Offset,
    scale: Float,
    rotation: Float,
    alpha: Float,
    colorVariant: Float,
    isFloret: Boolean,
    primary: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    if (isFloret) {
        // Draw a 5-petal cherry floret ornament on key positions along the wreath
        rotate(degrees = rotation, pivot = center) {
            val floretScale = scale * 0.75f
            for (p in 0 until 5) {
                rotate(degrees = p * 72f, pivot = center) {
                    val pLen = floretScale * 20f
                    val pWid = floretScale * 10f
                    val pNotch = pLen * 0.16f

                    val path = Path().apply {
                        moveTo(center.x, center.y)
                        cubicTo(
                            center.x - pWid, center.y - pLen * 0.35f,
                            center.x - pWid * 0.8f, center.y - pLen,
                            center.x - pWid * 0.4f, center.y - pLen
                        )
                        quadraticTo(
                            center.x, center.y - pLen + pNotch,
                            center.x + pWid * 0.4f, center.y - pLen
                        )
                        cubicTo(
                            center.x + pWid * 0.8f, center.y - pLen,
                            center.x + pWid, center.y - pLen * 0.35f,
                            center.x, center.y
                        )
                        close()
                    }

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryLight.copy(alpha = 0.95f * alpha),
                                primary.copy(alpha = 0.90f * alpha),
                                primaryDark.copy(alpha = 0.80f * alpha)
                            ),
                            startY = center.y - pLen,
                            endY = center.y
                        )
                    )
                }
            }
            // Core Stamen Glow
            drawCircle(
                color = Color(0xFFFFD1DC).copy(alpha = 0.90f * alpha),
                radius = floretScale * 4f,
                center = center
            )
        }
    } else {
        // Individual Notched Sakura Petal
        rotate(degrees = rotation, pivot = center) {
            val len = scale * 34f
            val wid = scale * 15f
            val notch = len * 0.16f

            val path = Path().apply {
                moveTo(center.x, center.y + len * 0.45f)
                cubicTo(
                    center.x - wid * 1.1f, center.y + len * 0.10f,
                    center.x - wid * 0.95f, center.y - len * 0.50f,
                    center.x - wid * 0.42f, center.y - len * 0.55f
                )
                quadraticTo(
                    center.x, center.y - len * 0.55f + notch,
                    center.x + wid * 0.42f, center.y - len * 0.55f
                )
                cubicTo(
                    center.x + wid * 0.95f, center.y - len * 0.50f,
                    center.x + wid * 1.1f, center.y + len * 0.10f,
                    center.x, center.y + len * 0.45f
                )
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryLight.copy(alpha = 0.95f * alpha),
                        primary.copy(alpha = 0.90f * alpha),
                        primaryDark.copy(alpha = 0.82f * alpha)
                    ),
                    startY = center.y - len * 0.55f,
                    endY = center.y + len * 0.45f
                )
            )
            drawPath(
                path = path,
                color = primaryLight.copy(alpha = 0.75f * alpha),
                style = Stroke(width = 1.0f)
            )
        }
    }
}

// =========================================================================
// AMBIENT TRANQUIL MIST / EMBERS
// =========================================================================

private fun DrawScope.drawAmbientMists(
    mists: List<AmbientDriftParticle>,
    minDim: Float,
    animClockSec: Float,
    lightColor: Color
) {
    val boundW = size.width * 0.84f
    val boundH = size.height * 0.84f
    val startX = size.width * 0.08f
    val startY = size.height * 0.08f

    for (m in mists) {
        val yProg = (animClockSec * m.speedY + m.relY) % 1.0f
        val posY = startY + yProg * boundH
        val sway = sin(animClockSec * m.driftFreq + m.relX * 8f) * (minDim * 0.04f)
        val posX = (startX + m.relX * boundW + sway).coerceIn(startX, startX + boundW)

        drawCircle(
            color = lightColor.copy(alpha = m.baseAlpha * 0.7f),
            radius = (minDim * 0.008f) * m.scale,
            center = Offset(posX, posY)
        )
    }
}
