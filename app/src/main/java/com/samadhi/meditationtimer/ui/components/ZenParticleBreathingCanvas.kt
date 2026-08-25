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
        drawGrowingBambooStalkEnso(
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
// 1. MOONLIT GOLDEN TIMBER BAMBOO ENSŌ & LUSH BOTANICAL GROVE ENGINE
// =========================================================================

private data class BotanicalBambooCluster(
    val angleDeg: Float,           // Angle on the circle (0 = top, 90 = right, 180 = bottom, 270 = left)
    val hasJoint: Boolean,         // Whether this location features a prominent golden bamboo node joint
    val isOutward: Boolean,        // General branch tilt
    val densityLevel: Int,         // 1 = delicate sprig, 2 = full spray, 3 = super lush dense grove
    val baseScale: Float,          // Scale multiplier
    val swayPhaseOffset: Float     // Wind sway phase offset
)

/**
 * Renders an authentic golden timber bamboo circular frame with prominent 3D raised
 * joint collars and a lush, multi-layered botanical bamboo grove matching traditional watercolor art.
 */
private fun DrawScope.drawGrowingBambooStalkEnso(
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

    // Substantial golden timber bamboo cane thickness (~18-20dp)
    val stalkThickness = minDim * 0.044f

    val topLeft = Offset(center.x - baseRadius, center.y - baseRadius)
    val arcSize = androidx.compose.ui.geometry.Size(baseRadius * 2f, baseRadius * 2f)

    // Palette: Warm Golden Timber Bamboo Cane (matching watercolor reference)
    val caneShadow = Color(0xFF422E10)     // Deep woody under-shadow
    val caneDeepWood = Color(0xFF9E7E36)   // Warm amber wood base
    val caneBody = Color(0xFFD8C274)       // Main golden bamboo cane body
    val caneLight = Color(0xFFEFE0A0)      // Bright golden cane highlight
    val caneSheen = Color(0xFFFFF8D6)      // Direct sunlight reflection streak

    // Foliage Palette: Rich multi-tonal watercolor bamboo leaves
    val leafShadow = Color(0xFF092415)     // Deep forest shadow leaf
    val leafMid = Color(0xFF1B6A3B)        // Vibrant emerald bamboo leaf
    val leafBright = Color(0xFF38A85C)     // Fresh spring bamboo blade
    val leafGlow = Color(0xFF6EDC94)       // Sunlight leaf highlight
    val leafSpine = Color(0xFFC4F7D8)      // Central vein spine line

    // 1. Ambient Zen Core Glow (Inhale / Hold Full)
    if (growthFraction > 0.05f) {
        val coreGlowRadius = baseRadius * (0.92f * growthFraction)
        val glowAlpha = when (currentPhase) {
            BreathPhase.INHALE -> 0.12f * phaseProgress
            BreathPhase.HOLD_FULL -> 0.18f + 0.04f * sin(animClockSec * 2.5f)
            BreathPhase.EXHALE -> 0.10f * (1.0f - phaseProgress)
            BreathPhase.HOLD_EMPTY -> 0.0f
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    leafGlow.copy(alpha = glowAlpha * 0.65f),
                    leafMid.copy(alpha = glowAlpha * 0.25f),
                    Color.Transparent
                ),
                center = center,
                radius = coreGlowRadius
            ),
            radius = coreGlowRadius,
            center = center
        )
    }

    // 2. Render Continuous Smooth Golden Timber Bamboo Culm
    if (totalSweep > 0.5f && overallAlpha > 0.01f) {
        // A. Deep Woody Contour Shadow Edge
        drawArc(
            color = caneShadow.copy(alpha = 0.95f * overallAlpha),
            startAngle = -90f,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stalkThickness * 1.08f, cap = StrokeCap.Round)
        )

        // B. Main Golden Timber Cane Body with Natural Sweep Gradient
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    caneDeepWood, caneBody, caneLight, caneBody,
                    caneDeepWood, caneBody, caneLight, caneBody, caneDeepWood
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

        // C. Inner Longitudinal Cylindrical Sheen Line
        drawArc(
            color = caneSheen.copy(alpha = 0.55f * overallAlpha),
            startAngle = -90f,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stalkThickness * 0.26f, cap = StrokeCap.Round)
        )
    }

    // 3. Prominent 3D Raised Golden Bamboo Joints & Dense Botanical Grove Clusters
    // 8 primary joint locations + dense layered foliage clusters around the ring
    val totalSegments = 8
    val botanicalClusters = listOf(
        BotanicalBambooCluster(0f, true, true, 2, 1.0f, 0.0f),       // Node 0 - Top Apex (Golden Joint + Arching Sprays)
        BotanicalBambooCluster(45f, true, true, 1, 0.85f, 1.2f),     // Node 1 - Top Right (Golden Joint + Delicate Sprig)
        BotanicalBambooCluster(90f, true, false, 2, 1.10f, 2.1f),    // Node 2 - Right (Golden Joint + Inward/Outward Sprays)
        BotanicalBambooCluster(135f, true, true, 3, 1.35f, 0.7f),    // Node 3 - Lower Right (Golden Joint + Lush Grove)
        BotanicalBambooCluster(180f, true, true, 3, 1.55f, 1.8f),    // Node 4 - Bottom Center (Golden Joint + Super Lush Dense Grove)
        BotanicalBambooCluster(225f, true, true, 3, 1.50f, 2.5f),    // Node 5 - Lower Left (Golden Joint + Super Lush Dense Grove)
        BotanicalBambooCluster(270f, true, false, 3, 1.40f, 0.9f),   // Node 6 - Left Middle (Golden Joint + Lush Left Grove)
        BotanicalBambooCluster(315f, true, true, 2, 1.15f, 1.5f)     // Node 7 - Upper Left (Golden Joint + Arching Canopy)
    )

    for ((index, cluster) in botanicalClusters.withIndex()) {
        val nodeDeg = -90f + cluster.angleDeg
        val isOutward = cluster.isOutward
        val swayOffset = cluster.swayPhaseOffset
        val density = cluster.densityLevel
        val baseScale = cluster.baseScale
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
                        val progress = ((phaseProgress - nodeFraction) / 0.12f).coerceIn(0f, 1f)
                        nodeAlpha = progress * progress
                        sproutScale = 0.35f + 0.65f * progress
                        swayAngle = (1f - progress) * (if (isOutward) 14f else -14f)
                    }
                }
            }
            BreathPhase.HOLD_FULL -> {
                nodeAlpha = 1.0f
                sproutScale = 1.0f + 0.035f * sin(animClockSec * 2.0f + swayOffset)
                swayAngle = sin(animClockSec * 1.8f + swayOffset) * 4.5f
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
                        swayAngle = dissolve * 12f
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

        // Draw Prominent Raised 3D Golden Bamboo Node Collar Ring
        if (cluster.hasJoint && (!isTopSprout || currentPhase != BreathPhase.HOLD_EMPTY)) {
            drawProminentBambooJointCollar(
                nodeCenter = nodeCenter,
                nodeRad = nodeRad,
                stalkThickness = stalkThickness,
                alpha = nodeAlpha,
                caneBody = caneBody,
                caneDeepWood = caneDeepWood,
                caneLight = caneLight,
                caneShadow = caneShadow,
                caneSheen = caneSheen
            )
        }

        // Tangent rotation along circle
        val tangentDeg = nodeDeg + 90f
        val branchAngle = tangentDeg + (if (isOutward) 35f else -40f) + swayAngle
        val clusterScale = (minDim / 360f) * sproutScale * baseScale

        // If top sprout on HOLD_EMPTY, draw peaceful glowing aura around bud
        if (isTopSprout && currentPhase == BreathPhase.HOLD_EMPTY) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        leafGlow.copy(alpha = 0.40f),
                        leafMid.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = nodeCenter,
                    radius = minDim * 0.08f
                ),
                radius = minDim * 0.08f,
                center = nodeCenter
            )
        }

        // Draw Truly Lush Multi-Layered Botanical Bamboo Grove Cluster
        drawLushBotanicalGrove(
            nodeCenter = nodeCenter,
            branchAngle = branchAngle,
            scale = clusterScale,
            alpha = nodeAlpha,
            densityLevel = density,
            animClockSec = animClockSec,
            swayOffset = swayOffset,
            leafShadow = leafShadow,
            leafMid = leafMid,
            leafBright = leafBright,
            leafGlow = leafGlow,
            leafSpine = leafSpine
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
                    caneLight.copy(alpha = 0.90f),
                    leafGlow.copy(alpha = 0.50f),
                    Color.Transparent
                ),
                center = tipOffset,
                radius = stalkThickness * 0.90f
            ),
            radius = stalkThickness * 0.90f,
            center = tipOffset
        )
    }
}

/**
 * Draws a prominent, clearly visible 3D raised golden bamboo joint collar with deep notch crease and golden bevel highlights.
 */
private fun DrawScope.drawProminentBambooJointCollar(
    nodeCenter: Offset,
    nodeRad: Float,
    stalkThickness: Float,
    alpha: Float,
    caneBody: Color,
    caneDeepWood: Color,
    caneLight: Color,
    caneShadow: Color,
    caneSheen: Color
) {
    val normalX = -sin(nodeRad)
    val normalY = cos(nodeRad)
    val jointHalfSpan = stalkThickness * 0.68f // Clearly wider than the culm to form the raised collar

    val p1 = Offset(nodeCenter.x - normalX * jointHalfSpan, nodeCenter.y - normalY * jointHalfSpan)
    val p2 = Offset(nodeCenter.x + normalX * jointHalfSpan, nodeCenter.y + normalY * jointHalfSpan)

    // 1. Raised 3D Bamboo Joint Swelling Bulge
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                caneSheen.copy(alpha = 0.95f * alpha),
                caneLight.copy(alpha = 0.95f * alpha),
                caneDeepWood.copy(alpha = 0.95f * alpha)
            ),
            center = nodeCenter,
            radius = stalkThickness * 0.65f
        ),
        radius = stalkThickness * 0.65f,
        center = nodeCenter
    )

    // 2. Dark Notch Crease Shadow Band across the cane
    drawLine(
        color = Color(0xFF281804).copy(alpha = 0.95f * alpha),
        start = p1,
        end = p2,
        strokeWidth = stalkThickness * 0.32f,
        cap = StrokeCap.Round
    )

    // 3. Raised Golden Wooden Collar Ridge Line (Upper highlight)
    drawLine(
        color = caneSheen.copy(alpha = 0.95f * alpha),
        start = Offset(p1.x + cos(nodeRad) * 2.0f, p1.y + sin(nodeRad) * 2.0f),
        end = Offset(p2.x + cos(nodeRad) * 2.0f, p2.y + sin(nodeRad) * 2.0f),
        strokeWidth = stalkThickness * 0.18f,
        cap = StrokeCap.Round
    )

    // 4. Under-Bevel Shadow Line
    drawLine(
        color = caneShadow.copy(alpha = 0.85f * alpha),
        start = Offset(p1.x - cos(nodeRad) * 1.8f, p1.y - sin(nodeRad) * 1.8f),
        end = Offset(p2.x - cos(nodeRad) * 1.8f, p2.y - sin(nodeRad) * 1.8f),
        strokeWidth = stalkThickness * 0.14f,
        cap = StrokeCap.Round
    )

    // 5. Center Golden Highlight Gem Shine
    drawCircle(
        color = Color.White.copy(alpha = 0.90f * alpha),
        radius = stalkThickness * 0.08f,
        center = Offset(nodeCenter.x + cos(nodeRad) * 1.2f, nodeCenter.y + sin(nodeRad) * 1.2f)
    )
}

/**
 * Draws a truly lush, multi-layered botanical bamboo grove cluster with dense overlapping foliage sprays.
 */
private fun DrawScope.drawLushBotanicalGrove(
    nodeCenter: Offset,
    branchAngle: Float,
    scale: Float,
    alpha: Float,
    densityLevel: Int,
    animClockSec: Float,
    swayOffset: Float,
    leafShadow: Color,
    leafMid: Color,
    leafBright: Color,
    leafGlow: Color,
    leafSpine: Color
) {
    rotate(degrees = branchAngle, pivot = nodeCenter) {
        val twigLength = scale * 26f
        val primaryLeafLen = scale * 48f
        val primaryLeafWid = scale * 9.5f

        val twigEnd = Offset(nodeCenter.x, nodeCenter.y - twigLength)

        // 1. Organic Curving Main Branchlet connecting to joint
        val stemPath = Path().apply {
            moveTo(nodeCenter.x, nodeCenter.y)
            quadraticTo(
                nodeCenter.x + scale * 4.0f, nodeCenter.y - twigLength * 0.5f,
                twigEnd.x, twigEnd.y
            )
        }
        drawPath(
            path = stemPath,
            color = leafBright.copy(alpha = 0.85f * alpha),
            style = Stroke(width = 2.0f * scale.coerceAtLeast(0.7f), cap = StrokeCap.Round)
        )

        // =========================================================================
        // SPRAY 1: PRIMARY MAIN CENTER FOLIAGE SPRAY (5 Overlapping Leaves)
        // =========================================================================
        drawBotanicalSubSpray(
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
            animClockSec = animClockSec,
            sprayOffset = swayOffset
        )

        // =========================================================================
        // SPRAY 2: SECONDARY INWARD FRAMING SPRAY (4 Overlapping Leaves)
        // =========================================================================
        if (densityLevel >= 2) {
            val inwardStemEnd = Offset(twigEnd.x - scale * 9f, twigEnd.y - scale * 6f)
            drawLine(
                color = leafBright.copy(alpha = 0.80f * alpha),
                start = twigEnd,
                end = inwardStemEnd,
                strokeWidth = 1.4f * scale.coerceAtLeast(0.7f),
                cap = StrokeCap.Round
            )
            drawBotanicalSubSpray(
                origin = inwardStemEnd,
                baseAngle = -28f,
                leafLength = primaryLeafLen * 0.85f,
                leafWidth = primaryLeafWid * 0.82f,
                alpha = alpha * 0.95f,
                scale = scale * 0.88f,
                leafShadow = leafShadow,
                leafMid = leafMid,
                leafBright = leafBright,
                leafGlow = leafGlow,
                leafSpine = leafSpine,
                animClockSec = animClockSec,
                sprayOffset = swayOffset + 1.2f
            )
        }

        // =========================================================================
        // SPRAY 3: TERTIARY OUTWARD BURSTING SPRAY (4 Overlapping Leaves)
        // =========================================================================
        if (densityLevel >= 2) {
            val outwardStemEnd = Offset(twigEnd.x + scale * 9f, twigEnd.y - scale * 5f)
            drawLine(
                color = leafBright.copy(alpha = 0.80f * alpha),
                start = twigEnd,
                end = outwardStemEnd,
                strokeWidth = 1.4f * scale.coerceAtLeast(0.7f),
                cap = StrokeCap.Round
            )
            drawBotanicalSubSpray(
                origin = outwardStemEnd,
                baseAngle = 26f,
                leafLength = primaryLeafLen * 0.82f,
                leafWidth = primaryLeafWid * 0.80f,
                alpha = alpha * 0.92f,
                scale = scale * 0.85f,
                leafShadow = leafShadow,
                leafMid = leafMid,
                leafBright = leafBright,
                leafGlow = leafGlow,
                leafSpine = leafSpine,
                animClockSec = animClockSec,
                sprayOffset = swayOffset + 2.4f
            )
        }

        // =========================================================================
        // SPRAY 4 & 5: SUPER LUSH DEEP GROVE EXTENSION (Dense Cascading Background)
        // =========================================================================
        if (densityLevel >= 3) {
            // Lower Deep Background Fan (6 cascading leaves)
            val deepBase1 = Offset(twigEnd.x - scale * 16f, twigEnd.y - scale * 12f)
            drawLine(
                color = leafBright.copy(alpha = 0.70f * alpha),
                start = twigEnd,
                end = deepBase1,
                strokeWidth = 1.2f * scale.coerceAtLeast(0.7f),
                cap = StrokeCap.Round
            )
            drawBotanicalSubSpray(
                origin = deepBase1,
                baseAngle = -48f,
                leafLength = primaryLeafLen * 0.75f,
                leafWidth = primaryLeafWid * 0.74f,
                alpha = alpha * 0.88f,
                scale = scale * 0.80f,
                leafShadow = leafShadow,
                leafMid = leafMid,
                leafBright = leafBright,
                leafGlow = leafGlow,
                leafSpine = leafSpine,
                animClockSec = animClockSec,
                sprayOffset = swayOffset + 3.5f
            )

            // Upper Deep Background Fan (6 cascading leaves)
            val deepBase2 = Offset(twigEnd.x + scale * 16f, twigEnd.y - scale * 11f)
            drawLine(
                color = leafBright.copy(alpha = 0.70f * alpha),
                start = twigEnd,
                end = deepBase2,
                strokeWidth = 1.2f * scale.coerceAtLeast(0.7f),
                cap = StrokeCap.Round
            )
            drawBotanicalSubSpray(
                origin = deepBase2,
                baseAngle = 45f,
                leafLength = primaryLeafLen * 0.72f,
                leafWidth = primaryLeafWid * 0.70f,
                alpha = alpha * 0.85f,
                scale = scale * 0.78f,
                leafShadow = leafShadow,
                leafMid = leafMid,
                leafBright = leafBright,
                leafGlow = leafGlow,
                leafSpine = leafSpine,
                animClockSec = animClockSec,
                sprayOffset = swayOffset + 4.2f
            )
        }
    }
}

/**
 * Draws a botanical fan spray of 4 to 5 gracefully overlapping lanceolate bamboo leaves.
 */
private fun DrawScope.drawBotanicalSubSpray(
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
    animClockSec: Float,
    sprayOffset: Float
) {
    rotate(degrees = baseAngle, pivot = origin) {
        // Leaf 1: Deep Background Shadow Leaf Left (-22°)
        val blade1Tip = Offset(origin.x - scale * 14f, origin.y - leafLength * 0.88f)
        drawSingleBotanicalBlade(
            tip = blade1Tip,
            base = origin,
            width = leafWidth * 0.82f,
            alpha = alpha * 0.90f,
            leafColor = leafShadow,
            glowColor = leafMid,
            shadowColor = Color(0xFF04140B),
            sheenColor = leafBright,
            flutter = sin(animClockSec * 2.2f + sprayOffset) * 2.5f
        )

        // Leaf 2: Deep Background Shadow Leaf Right (+20°)
        val blade2Tip = Offset(origin.x + scale * 13f, origin.y - leafLength * 0.84f)
        drawSingleBotanicalBlade(
            tip = blade2Tip,
            base = origin,
            width = leafWidth * 0.78f,
            alpha = alpha * 0.88f,
            leafColor = leafShadow,
            glowColor = leafMid,
            shadowColor = Color(0xFF04140B),
            sheenColor = leafBright,
            flutter = sin(animClockSec * 1.9f + sprayOffset + 1.1f) * 2.5f
        )

        // Leaf 3: Midtone Body Leaf Left (-11°)
        val blade3Tip = Offset(origin.x - scale * 7f, origin.y - leafLength * 0.94f)
        drawSingleBotanicalBlade(
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
        drawSingleBotanicalBlade(
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
        drawSingleBotanicalBlade(
            tip = blade5Tip,
            base = origin,
            width = leafWidth * 0.88f,
            alpha = alpha * 0.92f,
            leafColor = leafMid,
            glowColor = leafBright,
            shadowColor = leafShadow,
            sheenColor = leafGlow,
            flutter = sin(animClockSec * 2.1f + sprayOffset + 4.5f) * 2.8f
        )
    }
}

/**
 * Draws a single watercolor lanceolate bamboo blade with sharp pointed needle tip,
 * rich multi-tonal fill, central spine line, and natural leaf belly curvature.
 */
private fun DrawScope.drawSingleBotanicalBlade(
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

    // Lush Botanical Gradient Fill
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
