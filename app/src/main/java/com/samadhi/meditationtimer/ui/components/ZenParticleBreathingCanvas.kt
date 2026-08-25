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

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val minDim = minOf(width, height)
        val center = Offset(width / 2f, height / 2f)
        val ensoBaseRadius = minDim * 0.43f // Enlarged to make full, magnificent use of screen space

        // Draw the living Zen Ensō with top anchor leaf persisting on Hold Empty
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
            ZenThemeId.GOLDEN_BODHI -> {
                drawEnsoBodhiLeaf(
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
                    isFloret = (unit.index in listOf(0, 8, 17, 26, 35)),
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
// 1. MOONLIT BAMBOO ENSŌ LEAF & OFFSHOOT SPRAY
// =========================================================================

private fun DrawScope.drawSingleBambooBlade(
    tip: Offset,
    base: Offset,
    width: Float,
    alpha: Float,
    leafColor: Color,
    glowColor: Color
) {
    val dx = tip.x - base.x
    val dy = tip.y - base.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val normX = -dy / length
    val normY = dx / length

    val mid1 = Offset(base.x + dx * 0.30f, base.y + dy * 0.30f)
    val mid2 = Offset(base.x + dx * 0.65f, base.y + dy * 0.65f)

    val path = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(
            mid1.x + normX * width * 0.95f, mid1.y + normY * width * 0.95f,
            mid2.x + normX * width * 0.75f, mid2.y + normY * width * 0.75f,
            tip.x, tip.y
        )
        cubicTo(
            mid2.x - normX * width * 0.75f, mid2.y - normY * width * 0.75f,
            mid1.x - normX * width * 0.95f, mid1.y - normY * width * 0.95f,
            base.x, base.y
        )
        close()
    }

    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.95f * alpha),
                leafColor.copy(alpha = 0.90f * alpha),
                Color(0xFF041C14).copy(alpha = 0.85f * alpha)
            ),
            start = base,
            end = tip
        )
    )

    // Delicate central spine vein
    drawLine(
        color = glowColor.copy(alpha = 0.75f * alpha),
        start = base,
        end = Offset(base.x + dx * 0.90f, base.y + dy * 0.90f),
        strokeWidth = 1.0f,
        cap = StrokeCap.Round
    )

    // Silky outer contour
    drawPath(
        path = path,
        color = glowColor.copy(alpha = 0.50f * alpha),
        style = Stroke(width = 0.85f)
    )
}

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
        val len = scale * 40f
        val wid = scale * 8.5f
        val leafGreen = if (colorVariant > 0.5f) primary else primaryDark
        val glowRim = primaryLight

        val nodeBase = Offset(center.x, center.y + len * 0.48f)
        val branchJunction = Offset(center.x, center.y + len * 0.12f)

        // 1. Slender Bamboo Twig Stem (connecting Ensō track to leaf spray)
        val stalkPath = Path().apply {
            moveTo(nodeBase.x, nodeBase.y)
            quadraticTo(
                center.x - scale * 3.0f, center.y + len * 0.32f,
                branchJunction.x, branchJunction.y
            )
        }
        drawPath(
            path = stalkPath,
            color = glowRim.copy(alpha = 0.80f * alpha),
            style = Stroke(width = 1.5f * scale.coerceAtLeast(0.7f), cap = StrokeCap.Round)
        )

        // Tiny bamboo node joint accent dot
        drawCircle(
            color = glowRim.copy(alpha = 0.85f * alpha),
            radius = 1.6f * scale.coerceAtLeast(0.7f),
            center = nodeBase
        )

        // 2. Offshoot Branch Left (Secondary bamboo blade)
        val leftTip = Offset(center.x - scale * 17f, center.y - len * 0.18f)
        val leftStem = Path().apply {
            moveTo(branchJunction.x, branchJunction.y)
            quadraticTo(center.x - scale * 7f, center.y + len * 0.04f, center.x - scale * 8f, center.y - len * 0.04f)
        }
        drawPath(
            path = leftStem,
            color = glowRim.copy(alpha = 0.70f * alpha),
            style = Stroke(width = 1.0f * scale.coerceAtLeast(0.7f), cap = StrokeCap.Round)
        )
        drawSingleBambooBlade(
            tip = leftTip,
            base = Offset(center.x - scale * 8f, center.y - len * 0.04f),
            width = wid * 0.65f,
            alpha = alpha * 0.85f,
            leafColor = primaryDark,
            glowColor = glowRim
        )

        // 3. Offshoot Branch Right (if cluster variant has 3 leaves)
        if (colorVariant > 0.35f) {
            val rightTip = Offset(center.x + scale * 15f, center.y - len * 0.10f)
            val rightStem = Path().apply {
                moveTo(branchJunction.x, branchJunction.y)
                quadraticTo(center.x + scale * 5f, center.y + len * 0.06f, center.x + scale * 7f, center.y - len * 0.02f)
            }
            drawPath(
                path = rightStem,
                color = glowRim.copy(alpha = 0.70f * alpha),
                style = Stroke(width = 1.0f * scale.coerceAtLeast(0.7f), cap = StrokeCap.Round)
            )
            drawSingleBambooBlade(
                tip = rightTip,
                base = Offset(center.x + scale * 7f, center.y - len * 0.02f),
                width = wid * 0.58f,
                alpha = alpha * 0.80f,
                leafColor = primary,
                glowColor = glowRim
            )
        }

        // 4. Primary Center Bamboo Blade
        val mainTip = Offset(center.x, center.y - len * 0.48f)
        drawSingleBambooBlade(
            tip = mainTip,
            base = branchJunction,
            width = wid,
            alpha = alpha,
            leafColor = leafGreen,
            glowColor = glowRim
        )
    }
}

// =========================================================================
// 2. GOLDEN BODHI ENSŌ LEAF
// =========================================================================

private fun DrawScope.drawEnsoBodhiLeaf(
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
        val wid = scale * 15f

        val path = Path().apply {
            moveTo(center.x, center.y - len * 0.52f) // Tapered tip
            cubicTo(
                center.x - wid * 1.15f, center.y - len * 0.20f,
                center.x - wid * 0.85f, center.y + len * 0.35f,
                center.x, center.y + len * 0.48f // Heart base
            )
            cubicTo(
                center.x + wid * 0.85f, center.y + len * 0.35f,
                center.x + wid * 1.15f, center.y - len * 0.20f,
                center.x, center.y - len * 0.52f
            )
            close()
        }

        val baseColor = if (colorVariant > 0.5f) primary else primaryDark

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryLight.copy(alpha = 0.95f * alpha),
                    baseColor.copy(alpha = 0.88f * alpha),
                    Color(0xFF5D4010).copy(alpha = 0.85f * alpha)
                ),
                startY = center.y - len * 0.52f,
                endY = center.y + len * 0.48f
            )
        )

        // Bodhi leaf central stem & lateral filament veins
        drawLine(
            color = primaryLight.copy(alpha = 0.85f * alpha),
            start = Offset(center.x, center.y - len * 0.48f),
            end = Offset(center.x, center.y + len * 0.45f),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round
        )

        // Delicate radiating lateral veins
        for (i in 1..3) {
            val vy = center.y - len * 0.35f + (len * 0.65f) * (i / 4f)
            val vLen = wid * (0.65f - (i * 0.08f))
            drawLine(
                color = primaryLight.copy(alpha = 0.50f * alpha),
                start = Offset(center.x, vy),
                end = Offset(center.x + vLen, vy - len * 0.08f),
                strokeWidth = 0.8f
            )
            drawLine(
                color = primaryLight.copy(alpha = 0.50f * alpha),
                start = Offset(center.x, vy),
                end = Offset(center.x - vLen, vy - len * 0.08f),
                strokeWidth = 0.8f
            )
        }

        drawPath(
            path = path,
            color = primaryLight.copy(alpha = 0.70f * alpha),
            style = Stroke(width = 1.0f)
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
        // Refined, artistic 5-petal cherry blossom with natural ovate petals & glowing stamens
        rotate(degrees = rotation, pivot = center) {
            val floretScale = scale * 0.90f

            // Layer 1: 5 Main Petals with subtle natural tip cleft
            for (p in 0 until 5) {
                rotate(degrees = p * 72f, pivot = center) {
                    val pLen = floretScale * 22f
                    val pWid = floretScale * 11f
                    val pCleft = pLen * 0.06f

                    val path = Path().apply {
                        moveTo(center.x, center.y)
                        cubicTo(
                            center.x - pWid * 0.9f, center.y - pLen * 0.35f,
                            center.x - pWid * 0.95f, center.y - pLen * 0.85f,
                            center.x - pWid * 0.35f, center.y - pLen
                        )
                        quadraticTo(
                            center.x, center.y - pLen + pCleft,
                            center.x + pWid * 0.35f, center.y - pLen
                        )
                        cubicTo(
                            center.x + pWid * 0.95f, center.y - pLen * 0.85f,
                            center.x + pWid * 0.9f, center.y - pLen * 0.35f,
                            center.x, center.y
                        )
                        close()
                    }

                    // Petal Gradient: Soft translucent blush tip to luminous magenta base
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
                        color = primaryLight.copy(alpha = 0.65f * alpha),
                        style = Stroke(width = 0.9f)
                    )
                }
            }

            // Layer 2: Delicate Stamen Filaments with Glowing Golden-Pink Pollen Anthers
            val stamenCount = 8
            for (s in 0 until stamenCount) {
                val sAngle = s * (360f / stamenCount) + 18f
                val sRad = Math.toRadians(sAngle.toDouble()).toFloat()
                val sLen = floretScale * 9.5f
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
                    radius = floretScale * 1.4f,
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
                    radius = floretScale * 4.2f
                ),
                radius = floretScale * 4.2f,
                center = center
            )
        }
    } else {
        // Individual realistic Sakura Petal (elongated, natural ovate with subtle cleft)
        rotate(degrees = rotation, pivot = center) {
            val len = scale * 34f
            val wid = scale * 13.5f
            val cleft = len * 0.06f

            val path = Path().apply {
                moveTo(center.x, center.y + len * 0.45f)
                cubicTo(
                    center.x - wid * 0.95f, center.y + len * 0.15f,
                    center.x - wid * 1.0f, center.y - len * 0.40f,
                    center.x - wid * 0.35f, center.y - len * 0.55f
                )
                quadraticTo(
                    center.x, center.y - len * 0.55f + cleft,
                    center.x + wid * 0.35f, center.y - len * 0.55f
                )
                cubicTo(
                    center.x + wid * 1.0f, center.y - len * 0.40f,
                    center.x + wid * 0.95f, center.y + len * 0.15f,
                    center.x, center.y + len * 0.45f
                )
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF5F8).copy(alpha = 0.95f * alpha),
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
                color = primaryLight.copy(alpha = 0.70f * alpha),
                style = Stroke(width = 0.9f)
            )
        }
    }
}
