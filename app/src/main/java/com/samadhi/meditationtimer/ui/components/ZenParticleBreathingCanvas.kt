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
    if (themeId == ZenThemeId.BAMBOO_TEAL) {
        drawBotanicalWatercolorBambooEnso(
            center = center,
            baseRadius = baseRadius,
            minDim = minDim,
            currentPhase = currentPhase,
            phaseProgress = phaseProgress,
            animClockSec = animClockSec,
            primary = primary,
            primaryLight = primaryLight,
            primaryDark = primaryDark
        )
        return
    }

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
            ZenThemeId.BAMBOO_TEAL -> {}
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
// 1. MOONLIT BAMBOO LUSH BOTANICAL WATERCOLOR ENSŌ ENGINE (CONCEPT 2)
// =========================================================================

private data class BotanicalBambooNode(
    val angleDeg: Float,           // 0° = top apex, 60°, 120°, 180°, 240°, 300°
    val isOutward: Boolean,        // Branch orientation
    val scaleMult: Float,          // Foliage density scale
    val swayOffset: Float          // Wind oscillation phase offset
)

/**
 * Renders Concept 2: Lush Botanical Watercolor Bamboo Ensō matching the Moonlit Bamboo Teal theme.
 * Features a 3D living cylindrical culm with smooth sweep gradient, authentic node collars,
 * and multi-layered watercolor leaf sprays framing the circle.
 */
private fun DrawScope.drawBotanicalWatercolorBambooEnso(
    center: Offset,
    baseRadius: Float,
    minDim: Float,
    currentPhase: BreathPhase,
    phaseProgress: Float,
    animClockSec: Float,
    primary: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    val growthFraction = when (currentPhase) {
        BreathPhase.INHALE -> phaseProgress
        BreathPhase.HOLD_FULL -> 1.0f
        BreathPhase.EXHALE -> (1.0f - phaseProgress)
        BreathPhase.HOLD_EMPTY -> 0.0f
    }

    val overallAlpha = when (currentPhase) {
        BreathPhase.INHALE -> 1.0f
        BreathPhase.HOLD_FULL -> 1.0f
        BreathPhase.EXHALE -> (1.0f - phaseProgress * 0.75f)
        BreathPhase.HOLD_EMPTY -> 0.0f
    }

    val totalSweep = growthFraction * 360f

    // 3D Cylindrical Bamboo Culm Thickness (~18-20dp)
    val stalkThickness = minDim * 0.045f

    val topLeft = Offset(center.x - baseRadius, center.y - baseRadius)
    val arcSize = androidx.compose.ui.geometry.Size(baseRadius * 2f, baseRadius * 2f)

    // Palette: Serene Moonlit Bamboo Teal & Sage Watercolor Hues
    val culmShadow = Color(0xFF092020)     // Deep outer shadow contour
    val culmDeep = primaryDark             // Color(0xFF164E4D) - Deep twilight jade
    val culmBody = primary                 // Color(0xFF2EA8A4) - Vibrant moonlit teal
    val culmLight = Color(0xFF38C7C2)      // Bright teal body
    val culmSage = Color(0xFF8BA870)       // Warm bamboo sage tone
    val culmSheen = Color(0xFFD1FBF3)      // Direct moonlight reflection streak

    val leafShadow = Color(0xFF0A2928)     // Deep shadow foliage blade
    val leafMid = primary                  // Color(0xFF2EA8A4) - Vibrant teal blade
    val leafBright = Color(0xFF38C7C2)     // Fresh bright bamboo blade
    val leafGlow = primaryLight            // Color(0xFF5EEAD4) - Luminous mint highlight
    val leafSpine = Color(0xFFB8FAF0)      // Central vein spine line
    val leafSage = Color(0xFF8BA870)       // Warm sage accent blade

    // 1. Ambient Zen Core Glow (Inhale / Hold Full)
    if (growthFraction > 0.05f) {
        val coreGlowRadius = baseRadius * (0.92f * growthFraction)
        val glowAlpha = when (currentPhase) {
            BreathPhase.INHALE -> 0.14f * phaseProgress
            BreathPhase.HOLD_FULL -> 0.20f + 0.04f * sin(animClockSec * 2.5f)
            BreathPhase.EXHALE -> 0.12f * (1.0f - phaseProgress)
            BreathPhase.HOLD_EMPTY -> 0.0f
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    leafGlow.copy(alpha = glowAlpha * 0.65f),
                    culmBody.copy(alpha = glowAlpha * 0.25f),
                    Color.Transparent
                ),
                center = center,
                radius = coreGlowRadius
            ),
            radius = coreGlowRadius,
            center = center
        )
    }

    // 2. Render 3D Living Bamboo Culm (Stalk) with Smooth Sweep Shading
    if (totalSweep > 0.5f && overallAlpha > 0.01f) {
        // A. Deep Shadow Contour Under-Stroke
        drawArc(
            color = culmShadow.copy(alpha = 0.95f * overallAlpha),
            startAngle = -90f,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stalkThickness * 1.08f, cap = StrokeCap.Round)
        )

        // B. Main Cylindrical Living Bamboo Body with Natural Sweep Gradient
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    culmDeep, culmBody, culmLight, culmSage,
                    culmDeep, culmBody, culmLight, culmSage, culmDeep
                ),
                center = center
            ),
            startAngle = -90f,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stalkThickness * 0.90f, cap = StrokeCap.Round)
        )

        // C. Inner Longitudinal Moonlight Sheen Streak
        drawArc(
            color = culmSheen.copy(alpha = 0.45f * overallAlpha),
            startAngle = -90f,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stalkThickness * 0.24f, cap = StrokeCap.Round)
        )
    }

    // 3. 6 Authentic Bamboo Node Joints & Lush Multi-Layered Foliage Sprays
    val totalSegments = 6
    val nodes = listOf(
        BotanicalBambooNode(0f, true, 1.0f, 0.0f),       // Node 0 - Top Apex (Resting anchor sprout)
        BotanicalBambooNode(60f, true, 1.15f, 1.3f),     // Node 1 - Upper-Right (Outward spray)
        BotanicalBambooNode(120f, false, 1.30f, 2.1f),   // Node 2 - Lower-Right (Lush inward/outward grove)
        BotanicalBambooNode(180f, true, 1.45f, 0.8f),    // Node 3 - Bottom Center (Lush cascading grove)
        BotanicalBambooNode(240f, false, 1.35f, 1.9f),   // Node 4 - Lower-Left (Lush inward/outward grove)
        BotanicalBambooNode(300f, true, 1.20f, 2.7f)     // Node 5 - Upper-Left (Outward canopy spray)
    )

    for ((index, node) in nodes.withIndex()) {
        val nodeDeg = -90f + node.angleDeg
        val isOutward = node.isOutward
        val swayOffset = node.swayOffset
        val scaleMultiplier = node.scaleMult
        val nodeFraction = index.toFloat() / totalSegments.toFloat()
        val isTopSprout = (index == 0)

        var nodeAlpha = 0.0f
        var sproutScale = 0.0f
        var swayAngle = 0.0f

        when (currentPhase) {
            BreathPhase.INHALE -> {
                if (isTopSprout) {
                    nodeAlpha = 1.0f
                    sproutScale = 1.0f
                } else {
                    if (phaseProgress >= nodeFraction) {
                        val progress = ((phaseProgress - nodeFraction) / 0.14f).coerceIn(0f, 1f)
                        nodeAlpha = progress * progress
                        sproutScale = 0.35f + 0.65f * progress
                        swayAngle = (1f - progress) * (if (isOutward) 12f else -12f)
                    }
                }
            }
            BreathPhase.HOLD_FULL -> {
                nodeAlpha = 1.0f
                sproutScale = 1.0f + 0.035f * sin(animClockSec * 2.0f + swayOffset)
                swayAngle = sin(animClockSec * 1.8f + swayOffset) * 4.0f
            }
            BreathPhase.EXHALE -> {
                if (isTopSprout) {
                    nodeAlpha = 1.0f
                    sproutScale = 1.0f
                } else {
                    val detachFraction = (1.0f - nodeFraction) * 0.8f
                    if (phaseProgress >= detachFraction) {
                        val dissolve = ((phaseProgress - detachFraction) / 0.20f).coerceIn(0f, 1f)
                        nodeAlpha = 1.0f - dissolve
                        sproutScale = 1.0f - dissolve * 0.35f
                        swayAngle = dissolve * 10f
                    } else {
                        nodeAlpha = 1.0f
                        sproutScale = 1.0f
                    }
                }
            }
            BreathPhase.HOLD_EMPTY -> {
                if (isTopSprout) {
                    nodeAlpha = 0.95f
                    sproutScale = 1.0f + 0.04f * sin(animClockSec * 2.0f)
                    swayAngle = sin(animClockSec * 1.5f) * 3f
                } else {
                    nodeAlpha = 0.0f
                }
            }
        }

        if (nodeAlpha <= 0.01f) continue

        val nodeRad = Math.toRadians(nodeDeg.toDouble()).toFloat()
        val nodeX = center.x + cos(nodeRad) * baseRadius
        val nodeY = center.y + sin(nodeRad) * baseRadius
        val nodeCenter = Offset(nodeX, nodeY)

        // Draw Authentic Botanical Bamboo Joint Collar
        if (!isTopSprout || currentPhase != BreathPhase.HOLD_EMPTY) {
            drawBotanicalBambooJoint(
                nodeCenter = nodeCenter,
                nodeRad = nodeRad,
                stalkThickness = stalkThickness,
                alpha = nodeAlpha,
                culmBody = culmBody,
                culmLight = culmLight,
                culmSage = culmSage,
                culmShadow = culmShadow,
                culmSheen = culmSheen
            )
        }

        // Tangent rotation along circle
        val tangentDeg = nodeDeg + 90f
        val branchAngle = tangentDeg + (if (isOutward) 36f else -38f) + swayAngle
        val clusterScale = (minDim / 360f) * sproutScale * scaleMultiplier

        // If top sprout on HOLD_EMPTY, draw peaceful glowing aura around bud
        if (isTopSprout && currentPhase == BreathPhase.HOLD_EMPTY) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        leafGlow.copy(alpha = 0.40f),
                        culmBody.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = nodeCenter,
                    radius = minDim * 0.08f
                ),
                radius = minDim * 0.08f,
                center = nodeCenter
            )
        }

        // Draw Lush Layered Watercolor Bamboo Foliage Cluster
        drawLushWatercolorFoliageCluster(
            nodeCenter = nodeCenter,
            branchAngle = branchAngle,
            scale = clusterScale,
            alpha = nodeAlpha,
            animClockSec = animClockSec,
            swayOffset = swayOffset,
            isDense = (index in listOf(2, 3, 4)),
            leafShadow = leafShadow,
            leafMid = leafMid,
            leafBright = leafBright,
            leafGlow = leafGlow,
            leafSpine = leafSpine,
            leafSage = leafSage
        )
    }

    // 4. Leading Fresh Growing Bamboo Shoot Tip during Inhale
    if (currentPhase == BreathPhase.INHALE && phaseProgress in 0.01f..0.99f) {
        val sweepDeg = -90f + totalSweep
        val sweepRad = Math.toRadians(sweepDeg.toDouble()).toFloat()
        val tipX = center.x + cos(sweepRad) * baseRadius
        val tipY = center.y + sin(sweepRad) * baseRadius
        val tipOffset = Offset(tipX, tipY)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    leafGlow.copy(alpha = 0.90f),
                    culmBody.copy(alpha = 0.40f),
                    Color.Transparent
                ),
                center = tipOffset,
                radius = stalkThickness * 0.88f
            ),
            radius = stalkThickness * 0.88f,
            center = tipOffset
        )
    }
}

/**
 * Draws an authentic natural bamboo joint with soft swelling, joint crease line, and warm collar highlight.
 */
private fun DrawScope.drawBotanicalBambooJoint(
    nodeCenter: Offset,
    nodeRad: Float,
    stalkThickness: Float,
    alpha: Float,
    culmBody: Color,
    culmLight: Color,
    culmSage: Color,
    culmShadow: Color,
    culmSheen: Color
) {
    val normalX = -sin(nodeRad)
    val normalY = cos(nodeRad)
    val jointHalfSpan = stalkThickness * 0.62f

    val p1 = Offset(nodeCenter.x - normalX * jointHalfSpan, nodeCenter.y - normalY * jointHalfSpan)
    val p2 = Offset(nodeCenter.x + normalX * jointHalfSpan, nodeCenter.y + normalY * jointHalfSpan)

    // 1. Natural Joint Swelling Bulge
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                culmSheen.copy(alpha = 0.95f * alpha),
                culmLight.copy(alpha = 0.92f * alpha),
                culmSage.copy(alpha = 0.88f * alpha),
                culmShadow.copy(alpha = 0.85f * alpha)
            ),
            center = nodeCenter,
            radius = stalkThickness * 0.60f
        ),
        radius = stalkThickness * 0.60f,
        center = nodeCenter
    )

    // 2. Fine Joint Crease Seam Line
    drawLine(
        color = Color(0xFF061A19).copy(alpha = 0.95f * alpha),
        start = p1,
        end = p2,
        strokeWidth = stalkThickness * 0.22f,
        cap = StrokeCap.Round
    )

    // 3. Warm Bamboo Collar Highlight Rim
    drawLine(
        color = culmSheen.copy(alpha = 0.92f * alpha),
        start = Offset(p1.x + cos(nodeRad) * 1.5f, p1.y + sin(nodeRad) * 1.5f),
        end = Offset(p2.x + cos(nodeRad) * 1.5f, p2.y + sin(nodeRad) * 1.5f),
        strokeWidth = stalkThickness * 0.14f,
        cap = StrokeCap.Round
    )
}

/**
 * Draws a multi-tiered botanical foliage cluster with lush overlapping bamboo blades.
 */
private fun DrawScope.drawLushWatercolorFoliageCluster(
    nodeCenter: Offset,
    branchAngle: Float,
    scale: Float,
    alpha: Float,
    animClockSec: Float,
    swayOffset: Float,
    isDense: Boolean,
    leafShadow: Color,
    leafMid: Color,
    leafBright: Color,
    leafGlow: Color,
    leafSpine: Color,
    leafSage: Color
) {
    rotate(degrees = branchAngle, pivot = nodeCenter) {
        val twigLength = scale * 26f
        val primaryLeafLen = scale * 46f
        val primaryLeafWid = scale * 9.5f

        val twigEnd = Offset(nodeCenter.x, nodeCenter.y - twigLength)

        // 1. Organic Curving Main Twig Stem connecting to node
        val stemPath = Path().apply {
            moveTo(nodeCenter.x, nodeCenter.y)
            quadraticTo(
                nodeCenter.x + scale * 3.5f, nodeCenter.y - twigLength * 0.5f,
                twigEnd.x, twigEnd.y
            )
        }
        drawPath(
            path = stemPath,
            color = leafBright.copy(alpha = 0.85f * alpha),
            style = Stroke(width = 1.8f * scale.coerceAtLeast(0.7f), cap = StrokeCap.Round)
        )

        // =========================================================================
        // SPRAY 1: PRIMARY MAIN CENTER FOLIAGE SPRAY (5 Overlapping Leaves)
        // =========================================================================
        drawWatercolorLeafFan(
            origin = twigEnd,
            baseAngle = 0f,
            leafLength = primaryLeafLen,
            leafWidth = primaryLeafWid,
            alpha = alpha,
            scale = scale,
            leafShadow = leafShadow,
            leafMid = leafMid,
            leafBright = leafBright,
            leafGlow = leafGlow,
            leafSpine = leafSpine,
            leafSage = leafSage,
            animClockSec = animClockSec,
            sprayOffset = swayOffset
        )

        // =========================================================================
        // SPRAY 2: SECONDARY INWARD FRAMING SPRAY (4 Overlapping Leaves)
        // =========================================================================
        val inwardStemEnd = Offset(twigEnd.x - scale * 9f, twigEnd.y - scale * 6f)
        drawLine(
            color = leafBright.copy(alpha = 0.80f * alpha),
            start = twigEnd,
            end = inwardStemEnd,
            strokeWidth = 1.3f * scale.coerceAtLeast(0.7f),
            cap = StrokeCap.Round
        )
        drawWatercolorLeafFan(
            origin = inwardStemEnd,
            baseAngle = -30f,
            leafLength = primaryLeafLen * 0.85f,
            leafWidth = primaryLeafWid * 0.82f,
            alpha = alpha * 0.95f,
            scale = scale * 0.88f,
            leafShadow = leafShadow,
            leafMid = leafMid,
            leafBright = leafBright,
            leafGlow = leafGlow,
            leafSpine = leafSpine,
            leafSage = leafSage,
            animClockSec = animClockSec,
            sprayOffset = swayOffset + 1.2f
        )

        // =========================================================================
        // SPRAY 3: TERTIARY OUTWARD BURSTING SPRAY (4 Overlapping Leaves)
        // =========================================================================
        val outwardStemEnd = Offset(twigEnd.x + scale * 9f, twigEnd.y - scale * 5f)
        drawLine(
            color = leafBright.copy(alpha = 0.80f * alpha),
            start = twigEnd,
            end = outwardStemEnd,
            strokeWidth = 1.3f * scale.coerceAtLeast(0.7f),
            cap = StrokeCap.Round
        )
        drawWatercolorLeafFan(
            origin = outwardStemEnd,
            baseAngle = 28f,
            leafLength = primaryLeafLen * 0.82f,
            leafWidth = primaryLeafWid * 0.80f,
            alpha = alpha * 0.92f,
            scale = scale * 0.85f,
            leafShadow = leafShadow,
            leafMid = leafMid,
            leafBright = leafBright,
            leafGlow = leafGlow,
            leafSpine = leafSpine,
            leafSage = leafSage,
            animClockSec = animClockSec,
            sprayOffset = swayOffset + 2.4f
        )

        // =========================================================================
        // DENSE CANOPY EXTENSION (For Bottom & Side Clusters - Image 2 Lushness)
        // =========================================================================
        if (isDense) {
            val deepBase = Offset(twigEnd.x - scale * 15f, twigEnd.y - scale * 12f)
            drawLine(
                color = leafBright.copy(alpha = 0.70f * alpha),
                start = twigEnd,
                end = deepBase,
                strokeWidth = 1.1f * scale.coerceAtLeast(0.7f),
                cap = StrokeCap.Round
            )
            drawWatercolorLeafFan(
                origin = deepBase,
                baseAngle = -50f,
                leafLength = primaryLeafLen * 0.75f,
                leafWidth = primaryLeafWid * 0.74f,
                alpha = alpha * 0.88f,
                scale = scale * 0.80f,
                leafShadow = leafShadow,
                leafMid = leafMid,
                leafBright = leafBright,
                leafGlow = leafGlow,
                leafSpine = leafSpine,
                leafSage = leafSage,
                animClockSec = animClockSec,
                sprayOffset = swayOffset + 3.6f
            )
        }
    }
}

/**
 * Draws a fan spray of 4 to 5 gracefully overlapping lanceolate watercolor bamboo leaves.
 */
private fun DrawScope.drawWatercolorLeafFan(
    origin: Offset,
    baseAngle: Float,
    leafLength: Float,
    leafWidth: Float,
    alpha: Float,
    scale: Float,
    leafShadow: Color,
    leafMid: Color,
    leafBright: Color,
    leafGlow: Color,
    leafSpine: Color,
    leafSage: Color,
    animClockSec: Float,
    sprayOffset: Float
) {
    rotate(degrees = baseAngle, pivot = origin) {
        // Leaf 1: Deep Background Shadow Leaf Left (-22°)
        val blade1Tip = Offset(origin.x - scale * 14f, origin.y - leafLength * 0.88f)
        drawSingleWatercolorBambooBlade(
            tip = blade1Tip,
            base = origin,
            width = leafWidth * 0.82f,
            alpha = alpha * 0.90f,
            leafColor = leafShadow,
            glowColor = leafMid,
            shadowColor = Color(0xFF051515),
            sheenColor = leafBright,
            flutter = sin(animClockSec * 2.2f + sprayOffset) * 2.5f
        )

        // Leaf 2: Warm Sage Accent Leaf Right (+22°)
        val blade2Tip = Offset(origin.x + scale * 14f, origin.y - leafLength * 0.84f)
        drawSingleWatercolorBambooBlade(
            tip = blade2Tip,
            base = origin,
            width = leafWidth * 0.78f,
            alpha = alpha * 0.88f,
            leafColor = leafSage,
            glowColor = leafGlow,
            shadowColor = leafShadow,
            sheenColor = leafSpine,
            flutter = sin(animClockSec * 1.9f + sprayOffset + 1.1f) * 2.5f
        )

        // Leaf 3: Midtone Body Leaf Left (-11°)
        val blade3Tip = Offset(origin.x - scale * 7f, origin.y - leafLength * 0.94f)
        drawSingleWatercolorBambooBlade(
            tip = blade3Tip,
            base = origin,
            width = leafWidth * 0.92f,
            alpha = alpha * 0.95f,
            leafColor = leafMid,
            glowColor = leafBright,
            shadowColor = leafShadow,
            sheenColor = leafGlow,
            flutter = sin(animClockSec * 2.0f + sprayOffset + 2.3f) * 3.0f
        )

        // Leaf 4: Main Prominent Foreground Blade (Center, 0°)
        val blade4Tip = Offset(origin.x, origin.y - leafLength)
        drawSingleWatercolorBambooBlade(
            tip = blade4Tip,
            base = origin,
            width = leafWidth,
            alpha = alpha,
            leafColor = leafBright,
            glowColor = leafGlow,
            shadowColor = leafMid,
            sheenColor = leafSpine,
            flutter = sin(animClockSec * 1.7f + sprayOffset + 3.1f) * 3.5f
        )

        // Leaf 5: Foreground Highlight Leaf Right (+12°)
        val blade5Tip = Offset(origin.x + scale * 8f, origin.y - leafLength * 0.92f)
        drawSingleWatercolorBambooBlade(
            tip = blade5Tip,
            base = origin,
            width = leafWidth * 0.88f,
            alpha = alpha * 0.92f,
            leafColor = leafMid,
            glowColor = leafGlow,
            shadowColor = leafShadow,
            sheenColor = leafSpine,
            flutter = sin(animClockSec * 2.1f + sprayOffset + 4.5f) * 2.8f
        )
    }
}

/**
 * Draws a single watercolor lanceolate bamboo blade with sharp pointed needle tip,
 * rich multi-tonal fill, central spine line, and natural leaf belly curvature.
 */
private fun DrawScope.drawSingleWatercolorBambooBlade(
    tip: Offset,
    base: Offset,
    width: Float,
    alpha: Float,
    leafColor: Color,
    glowColor: Color,
    shadowColor: Color,
    sheenColor: Color,
    flutter: Float
) {
    val dx = tip.x - base.x
    val dy = tip.y - base.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val normX = -dy / length
    val normY = dx / length

    val mid1 = Offset(base.x + dx * 0.26f + normX * flutter * 0.4f, base.y + dy * 0.26f + normY * flutter * 0.4f)
    val mid2 = Offset(base.x + dx * 0.65f + normX * flutter, base.y + dy * 0.65f + normY * flutter)

    val path = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(
            mid1.x + normX * width * 1.12f, mid1.y + normY * width * 1.12f,
            mid2.x + normX * width * 0.72f, mid2.y + normY * width * 0.72f,
            tip.x + normX * flutter * 1.2f, tip.y + normY * flutter * 1.2f
        )
        cubicTo(
            mid2.x - normX * width * 0.72f, mid2.y - normY * width * 0.72f,
            mid1.x - normX * width * 1.12f, mid1.y - normY * width * 1.12f,
            base.x, base.y
        )
        close()
    }

    // Lush Botanical Watercolor Gradient Fill
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.95f * alpha),
                leafColor.copy(alpha = 0.92f * alpha),
                shadowColor.copy(alpha = 0.90f * alpha)
            ),
            start = base,
            end = tip
        )
    )

    // Delicate Central Spine Vein Line
    drawLine(
        color = sheenColor.copy(alpha = 0.75f * alpha),
        start = base,
        end = Offset(base.x + dx * 0.94f + normX * flutter, base.y + dy * 0.94f + normY * flutter),
        strokeWidth = 1.1f,
        cap = StrokeCap.Round
    )

    // Subtle Watercolor Contour Rim
    drawPath(
        path = path,
        color = glowColor.copy(alpha = 0.35f * alpha),
        style = Stroke(width = 0.85f)
    )
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
