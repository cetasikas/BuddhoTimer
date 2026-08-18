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
 * Ambient falling background leaves/petals in Box Breathing mode.
 */
private data class AmbientFallingLeaf(
    val relX: Float,
    val relY: Float,
    val speedY: Float,
    val driftFreq: Float,
    val driftAmp: Float,
    val scale: Float,
    val initialRot: Float,
    val rotSpeed: Float,
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
            val strokeThickness = 0.88f + 0.32f * sin(fraction * Math.PI.toFloat())
            EnsoUnit(
                index = i,
                angleDeg = fraction * 360f,
                radiusJitter = (rand.nextFloat() - 0.5f) * 0.06f,
                angleJitter = (rand.nextFloat() - 0.5f) * 3.0f,
                sizeScale = strokeThickness * (0.85f + rand.nextFloat() * 0.35f),
                rotationOffset = (rand.nextFloat() - 0.5f) * 18f,
                driftAngle = (rand.nextFloat() * 360f),
                driftSpeed = 0.6f + rand.nextFloat() * 0.8f,
                colorVariant = rand.nextFloat()
            )
        }
    }

    // Precompute 18 ambient falling background leaves/petals
    val fallingLeaves = remember {
        val rand = Random(42)
        List(18) {
            AmbientFallingLeaf(
                relX = 0.06f + rand.nextFloat() * 0.88f,
                relY = rand.nextFloat(),
                speedY = 0.016f + rand.nextFloat() * 0.022f,
                driftFreq = 0.35f + rand.nextFloat() * 0.65f,
                driftAmp = 0.04f + rand.nextFloat() * 0.05f,
                scale = 0.45f + rand.nextFloat() * 0.40f,
                initialRot = rand.nextFloat() * 360f,
                rotSpeed = (rand.nextFloat() - 0.5f) * 45f,
                baseAlpha = 0.16f + rand.nextFloat() * 0.16f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val minDim = minOf(width, height)
        val center = Offset(width / 2f, height / 2f)
        val ensoBaseRadius = minDim * 0.43f // Enlarged to make full, magnificent use of screen space

        // 1. Draw ambient falling background leaves matching active theme
        drawAmbientFallingLeaves(
            fallingLeaves = fallingLeaves,
            minDim = minDim,
            animClockSec = animClockSec,
            themeId = zenTheme.id,
            primary = zenTheme.primary,
            primaryLight = zenTheme.primaryLight,
            primaryDark = zenTheme.primaryDark
        )

        // 2. Draw the living Zen Ensō with top anchor leaf persisting on Hold Empty
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
// AMBIENT FALLING BACKGROUND LEAVES / PETALS
// =========================================================================

private fun DrawScope.drawAmbientFallingLeaves(
    fallingLeaves: List<AmbientFallingLeaf>,
    minDim: Float,
    animClockSec: Float,
    themeId: ZenThemeId,
    primary: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    val boundW = size.width * 0.88f
    val boundH = size.height * 0.88f
    val startX = size.width * 0.06f
    val startY = size.height * 0.06f

    for (leaf in fallingLeaves) {
        val yProg = (animClockSec * leaf.speedY + leaf.relY) % 1.0f
        val posY = startY + yProg * boundH
        val sway = sin(animClockSec * leaf.driftFreq + leaf.relX * 10f) * (minDim * leaf.driftAmp)
        val posX = (startX + leaf.relX * boundW + sway).coerceIn(startX, startX + boundW)
        val rot = leaf.initialRot + animClockSec * leaf.rotSpeed
        val finalScale = (minDim / 380f) * leaf.scale

        val leafCenter = Offset(posX, posY)

        when (themeId) {
            ZenThemeId.BAMBOO_TEAL -> {
                drawEnsoBambooLeaf(
                    center = leafCenter,
                    scale = finalScale,
                    rotation = rot,
                    alpha = leaf.baseAlpha,
                    colorVariant = 0.5f,
                    primary = primary,
                    primaryLight = primaryLight,
                    primaryDark = primaryDark
                )
            }
            ZenThemeId.SACRED_LOTUS -> {
                drawEnsoLotusPetal(
                    center = leafCenter,
                    scale = finalScale,
                    rotation = rot,
                    alpha = leaf.baseAlpha,
                    colorVariant = 0.5f,
                    primary = primary,
                    primaryLight = primaryLight,
                    primaryDark = primaryDark
                )
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                drawEnsoSakuraPetal(
                    center = leafCenter,
                    scale = finalScale,
                    rotation = rot,
                    alpha = leaf.baseAlpha,
                    colorVariant = 0.5f,
                    isFloret = false,
                    primary = primary,
                    primaryLight = primaryLight,
                    primaryDark = primaryDark
                )
            }
        }
    }
}

// =========================================================================
// ZEN ENSŌ DRAWING & UNMAKING ENGINE (TOP LEAF PERSISTS ON HOLD EMPTY)
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

    val ensoFormFactor = when (currentPhase) {
        BreathPhase.INHALE -> phaseProgress
        BreathPhase.HOLD_FULL -> 1.0f
        BreathPhase.EXHALE -> (1.0f - phaseProgress)
        BreathPhase.HOLD_EMPTY -> 0.0f
    }

    // Central Core Aura when Ensō is holding or blooming
    if (ensoFormFactor > 0.05f) {
        val coreGlowRadius = baseRadius * (0.90f * ensoFormFactor)
        val glowAlpha = when (currentPhase) {
            BreathPhase.INHALE -> 0.14f * phaseProgress
            BreathPhase.HOLD_FULL -> 0.20f + 0.05f * sin(animClockSec * 2.5f)
            BreathPhase.EXHALE -> 0.14f * (1.0f - phaseProgress)
            BreathPhase.HOLD_EMPTY -> 0.0f
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryLight.copy(alpha = glowAlpha),
                    primary.copy(alpha = glowAlpha * 0.45f),
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
        val isTopAnchorLeaf = (unit.index == 0) // The very first/last leaf at the top apex (-90°)

        var unitAlpha = 0.0f
        var unitScaleMult = 1.0f
        var driftOffsetX = 0.0f
        var driftOffsetY = 0.0f
        var extraRotation = 0.0f

        when (currentPhase) {
            BreathPhase.INHALE -> {
                if (isTopAnchorLeaf) {
                    // Top anchor leaf is already present, gently radiating as the breath begins
                    unitAlpha = 1.0f
                    unitScaleMult = 1.0f + 0.04f * sin(animClockSec * 2.0f)
                } else {
                    // Sweep clockwise: unit appears when inhale progress reaches its fraction
                    val appearanceStart = unitFraction * 0.88f
                    if (phaseProgress >= appearanceStart) {
                        val unitProgress = ((phaseProgress - appearanceStart) / 0.12f).coerceIn(0.0f, 1.0f)
                        unitAlpha = unitProgress
                        unitScaleMult = 0.3f + 0.7f * unitProgress
                        extraRotation = (1.0f - unitProgress) * 30f
                    }
                }
            }
            BreathPhase.HOLD_FULL -> {
                // Complete Ensō with soft harmonic sway
                unitAlpha = 1.0f
                unitScaleMult = 1.0f + 0.04f * sin(animClockSec * 2.0f + unit.index)
                extraRotation = sin(animClockSec * 1.5f + unit.index) * 4f
            }
            BreathPhase.EXHALE -> {
                if (isTopAnchorLeaf) {
                    // Top anchor leaf stays resting at the top apex
                    unitAlpha = 1.0f
                    unitScaleMult = 1.0f + 0.03f * sin(animClockSec * 1.8f)
                } else {
                    // Other leaves (1..41) detach and dissolve in unmaking order
                    val detachStart = (1.0f - unitFraction) * 0.75f
                    if (phaseProgress >= detachStart) {
                        val dissolveProgress = ((phaseProgress - detachStart) / 0.25f).coerceIn(0.0f, 1.0f)
                        unitAlpha = (1.0f - dissolveProgress)

                        val driftRad = Math.toRadians(unit.driftAngle.toDouble() + animClockSec * 20.0).toFloat()
                        val driftDist = minDim * 0.14f * dissolveProgress * unit.driftSpeed
                        driftOffsetX = cos(driftRad) * driftDist
                        driftOffsetY = sin(driftRad) * driftDist - (minDim * 0.03f * dissolveProgress)
                        extraRotation = dissolveProgress * 45f * if (unit.index % 2 == 0) 1f else -1f
                        unitScaleMult = (1.0f - dissolveProgress * 0.5f)
                    } else {
                        unitAlpha = 1.0f
                    }
                }
            }
            BreathPhase.HOLD_EMPTY -> {
                if (isTopAnchorLeaf) {
                    // Top anchor leaf persists during HOLD_EMPTY at the top of the circle
                    unitAlpha = 0.95f
                    unitScaleMult = 1.05f + 0.05f * sin(animClockSec * 2.2f)
                    extraRotation = sin(animClockSec * 1.8f) * 6f
                } else {
                    unitAlpha = 0.0f
                }
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

        // If top anchor leaf during HOLD_EMPTY, add a gentle glowing aura around it
        if (isTopAnchorLeaf && (currentPhase == BreathPhase.HOLD_EMPTY || (currentPhase == BreathPhase.EXHALE && phaseProgress > 0.8f))) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryLight.copy(alpha = 0.35f * unitAlpha),
                        primary.copy(alpha = 0.15f * unitAlpha),
                        Color.Transparent
                    ),
                    center = unitCenter,
                    radius = minDim * 0.08f
                ),
                radius = minDim * 0.08f,
                center = unitCenter
            )
        }

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
                    isFloret = (unit.index % 5 == 0),
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
// 1. MOONLIT BAMBOO ENSŌ LEAF
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

        drawLine(
            color = glowRim.copy(alpha = 0.80f * alpha),
            start = Offset(center.x, center.y - len * 0.40f),
            end = Offset(center.x, center.y + len * 0.50f),
            strokeWidth = 1.1f,
            cap = StrokeCap.Round
        )

        drawPath(
            path = path,
            color = glowRim.copy(alpha = 0.65f * alpha),
            style = Stroke(width = 1.0f)
        )
    }
}

// =========================================================================
// 2. SACRED LOTUS ENSŌ PETAL
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

        drawLine(
            color = Color.White.copy(alpha = 0.75f * alpha),
            start = Offset(center.x, center.y - len * 0.45f),
            end = Offset(center.x, center.y + len * 0.40f),
            strokeWidth = 1.0f
        )

        drawPath(
            path = path,
            color = primaryLight.copy(alpha = 0.80f * alpha),
            style = Stroke(width = 1.1f)
        )
    }
}

// =========================================================================
// 3. SAKURA TWILIGHT ENSŌ PETAL & REFINED ARTISTIC FLORET
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
        // Refined, artistic 5-petal cherry blossom with layered translucent petals & glowing stamens
        rotate(degrees = rotation, pivot = center) {
            val floretScale = scale * 0.85f

            // Layer 1: 5 Main Petals with delicate heart notches
            for (p in 0 until 5) {
                rotate(degrees = p * 72f, pivot = center) {
                    val pLen = floretScale * 22f
                    val pWid = floretScale * 11.5f
                    val pNotch = pLen * 0.16f

                    val path = Path().apply {
                        moveTo(center.x, center.y)
                        cubicTo(
                            center.x - pWid * 1.1f, center.y - pLen * 0.35f,
                            center.x - pWid * 0.9f, center.y - pLen,
                            center.x - pWid * 0.45f, center.y - pLen
                        )
                        quadraticTo(
                            center.x, center.y - pLen + pNotch,
                            center.x + pWid * 0.45f, center.y - pLen
                        )
                        cubicTo(
                            center.x + pWid * 0.9f, center.y - pLen,
                            center.x + pWid * 1.1f, center.y - pLen * 0.35f,
                            center.x, center.y
                        )
                        close()
                    }

                    // Petal Gradient: Translucent blush white tip to vibrant magenta base
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF0F5).copy(alpha = 0.95f * alpha),
                                primaryLight.copy(alpha = 0.92f * alpha),
                                primary.copy(alpha = 0.88f * alpha),
                                primaryDark.copy(alpha = 0.82f * alpha)
                            ),
                            startY = center.y - pLen,
                            endY = center.y
                        )
                    )
                    // Delicate petal rim stroke
                    drawPath(
                        path = path,
                        color = primaryLight.copy(alpha = 0.70f * alpha),
                        style = Stroke(width = 1.0f)
                    )
                }
            }

            // Layer 2: Delicate Stamen Filaments with Glowing Golden-Pink Pollen Anthers
            val stamenCount = 8
            for (s in 0 until stamenCount) {
                val sAngle = s * (360f / stamenCount) + 18f
                val sRad = Math.toRadians(sAngle.toDouble()).toFloat()
                val sLen = floretScale * 10f
                val sx = center.x + cos(sRad) * sLen
                val sy = center.y + sin(sRad) * sLen

                // Filament
                drawLine(
                    color = primaryLight.copy(alpha = 0.85f * alpha),
                    start = center,
                    end = Offset(sx, sy),
                    strokeWidth = 1.0f,
                    cap = StrokeCap.Round
                )
                // Golden Pollen Anther
                drawCircle(
                    color = Color(0xFFFFE4B5).copy(alpha = 0.95f * alpha),
                    radius = floretScale * 1.5f,
                    center = Offset(sx, sy)
                )
            }

            // Layer 3: Central Crimson Pistil Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE91E63).copy(alpha = 0.95f * alpha),
                        Color(0xFFAD1457).copy(alpha = 0.85f * alpha),
                        primaryDark.copy(alpha = 0.60f * alpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = floretScale * 4.5f
                ),
                radius = floretScale * 4.5f,
                center = center
            )
        }
    } else {
        // Individual Notched Sakura Petal with heart notch
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
                        Color(0xFFFFF0F5).copy(alpha = 0.95f * alpha),
                        primaryLight.copy(alpha = 0.90f * alpha),
                        primary.copy(alpha = 0.85f * alpha),
                        primaryDark.copy(alpha = 0.80f * alpha)
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
