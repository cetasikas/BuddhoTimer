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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenAmber
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

private data class AmbientPetal(
    val id: Int,
    val initialAngle: Float,
    val distanceRatio: Float,
    val baseScale: Float,
    val rotationSpeed: Float,
    val colorIndex: Int
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current

    // Ambient floating particles surrounding the hero subject
    val ambientPetals = remember(currentTheme.id) {
        val count = 16
        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat()
        List(count) { i ->
            AmbientPetal(
                id = i,
                initialAngle = i * goldenAngle + Random.nextFloat() * 0.4f,
                distanceRatio = 0.50f + (i.toFloat() / count.toFloat()) * 0.45f,
                baseScale = Random.nextFloat() * 0.4f + 0.7f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 40f,
                colorIndex = i % currentTheme.particlePalette.size
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "livingBreathClock")
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
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val easedProgress = easeInOutCubic(phaseProgress)
    val (growthFactor, phaseGlowColor, glowPulseIntensity) = when (currentPhase) {
        BreathPhase.INHALE -> {
            val growth = 0.28f + (0.72f * easedProgress)
            val glow = 0.35f + (0.65f * easedProgress)
            Triple(growth, currentTheme.primaryLight, glow)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = sin(phaseProgress * PI.toFloat()) * 0.035f
            val glowPulse = 1.0f + sin(phaseProgress * PI.toFloat()) * 0.25f
            Triple(1.0f + suspensionPulse, currentTheme.primaryLight, glowPulse)
        }
        BreathPhase.EXHALE -> {
            val contraction = 1.0f - (0.72f * easedProgress)
            val glow = 1.0f - (0.65f * easedProgress)
            Triple(contraction, currentTheme.overrunColor, glow)
        }
        BreathPhase.HOLD_EMPTY -> {
            val quietPulse = sin(phaseProgress * PI.toFloat()) * 0.020f
            val quietGlow = 0.35f + sin(phaseProgress * PI.toFloat()) * 0.12f
            Triple(0.28f + quietPulse, currentTheme.primary, quietGlow)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)
        val minDim = width.coerceAtMost(height)

        // 1. Ambient Background Breath Glow Aura
        val auraRadius = minDim * (0.32f + 0.38f * growthFactor) * corePulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseGlowColor.copy(alpha = (0.24f * glowPulseIntensity).coerceIn(0.06f, 0.40f)),
                    currentTheme.primary.copy(alpha = (0.10f * glowPulseIntensity).coerceIn(0.02f, 0.20f)),
                    Color.Transparent
                ),
                center = center,
                radius = auraRadius * 1.35f
            ),
            radius = auraRadius * 1.35f,
            center = center
        )

        // 2. Ambient Drifting Petals in Outer Orbit
        val palette = currentTheme.particlePalette
        for (petal in ambientPetals) {
            val orbitRadius = minDim * (petal.distanceRatio * (0.85f + 0.15f * growthFactor))
            val currentAngle = petal.initialAngle + animClockSec * (0.025f / petal.distanceRatio)
            val px = center.x + cos(currentAngle) * orbitRadius
            val py = center.y + sin(currentAngle) * orbitRadius
            val rot = (Math.toDegrees(currentAngle.toDouble()).toFloat()) + petal.rotationSpeed * animClockSec
            val pColor = palette[petal.colorIndex]

            when (currentTheme.id) {
                ZenThemeId.BAMBOO_TEAL -> {
                    drawSingleBambooLeaf(
                        tip = Offset(px, py),
                        scale = petal.baseScale * (0.8f + 0.2f * growthFactor),
                        rotation = rot,
                        leafColor = pColor.copy(alpha = 0.45f),
                        rimColor = phaseGlowColor.copy(alpha = 0.50f)
                    )
                }
                ZenThemeId.SACRED_LOTUS -> {
                    drawSingleLotusPetal(
                        tip = Offset(px, py),
                        scale = petal.baseScale * (0.7f + 0.3f * growthFactor),
                        rotation = rot,
                        petalColor = pColor.copy(alpha = 0.40f),
                        rimColor = phaseGlowColor.copy(alpha = 0.50f)
                    )
                }
                ZenThemeId.SAKURA_TWILIGHT -> {
                    drawSingleSakuraPetal(
                        center = Offset(px, py),
                        scale = petal.baseScale * (0.75f + 0.25f * growthFactor),
                        rotation = rot,
                        petalColor = pColor.copy(alpha = 0.50f),
                        rimColor = phaseGlowColor.copy(alpha = 0.55f)
                    )
                }
            }
        }

        // 3. Render Bespoke Hero Theme Breathing Animation
        when (currentTheme.id) {
            ZenThemeId.BAMBOO_TEAL -> {
                // 🎋 1. LIVING GROWING BAMBOO STALK THAT BREATHES
                drawLivingBambooStalk(
                    center = center,
                    minDim = minDim,
                    growthFactor = growthFactor,
                    animClockSec = animClockSec,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    glowIntensity = glowPulseIntensity
                )
            }
            ZenThemeId.SACRED_LOTUS -> {
                // 🪷 2. SACRED LOTUS FLOWER ON LILY PAD WITH RIPPLES
                drawSacredLotusOnLilyPad(
                    center = center,
                    minDim = minDim,
                    bloomFactor = growthFactor,
                    animClockSec = animClockSec,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    glowIntensity = glowPulseIntensity
                )
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                // 🌸 3. BLOOMING CHERRY BLOSSOM WITH STAMENS & TWILIGHT GLOW
                drawSakuraCherryBlossom(
                    center = center,
                    minDim = minDim,
                    bloomFactor = growthFactor,
                    animClockSec = animClockSec,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    glowIntensity = glowPulseIntensity
                )
            }
        }
    }
}

// =========================================================================
// 🎋 1. THEME: MOONLIT BAMBOO — LIVING GROWING & SWAYING BAMBOO STALK
// =========================================================================
private fun DrawScope.drawLivingBambooStalk(
    center: Offset,
    minDim: Float,
    growthFactor: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    glowIntensity: Float
) {
    val stalkBaseY = center.y + minDim * 0.38f
    val fullHeight = minDim * 0.72f
    val currentHeight = fullHeight * (0.35f + 0.65f * growthFactor)
    val stalkTopY = stalkBaseY - currentHeight
    val stalkWidth = minDim * 0.038f * (0.85f + 0.15f * growthFactor)

    // Gentle natural wind sway
    val swayOffset = sin(animClockSec * 1.2f) * (minDim * 0.024f * growthFactor)

    // 1. Ground Node Base / Soil Mist Aura
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.30f * glowIntensity),
                primaryDark.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(center.x, stalkBaseY),
            radius = minDim * 0.22f
        ),
        topLeft = Offset(center.x - minDim * 0.22f, stalkBaseY - minDim * 0.05f),
        size = Size(minDim * 0.44f, minDim * 0.10f)
    )

    // 2. Draw 4 Segmented Bamboo Culms (Internodes)
    val nodeCount = 4
    val segmentHeight = currentHeight / nodeCount

    for (i in 0 until nodeCount) {
        val segBottomY = stalkBaseY - (i * segmentHeight)
        val segTopY = stalkBaseY - ((i + 1) * segmentHeight)

        // Calculate slight progressive sway curvature
        val bottomCurve = (i.toFloat() / nodeCount) * swayOffset
        val topCurve = ((i + 1).toFloat() / nodeCount) * swayOffset

        val bx = center.x + bottomCurve
        val tx = center.x + topCurve

        // Draw Culm Segment Body
        val culmPath = Path().apply {
            moveTo(bx - stalkWidth / 2f, segBottomY)
            lineTo(tx - stalkWidth * 0.46f, segTopY + 2f)
            lineTo(tx + stalkWidth * 0.46f, segTopY + 2f)
            lineTo(bx + stalkWidth / 2f, segBottomY)
            close()
        }

        // Culm Fill with subtle vertical bamboo gradient
        drawPath(
            path = culmPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    primaryDark.copy(alpha = 0.95f),
                    primaryColor.copy(alpha = 0.95f),
                    primaryLight.copy(alpha = 0.90f),
                    primaryColor.copy(alpha = 0.95f),
                    primaryDark.copy(alpha = 0.95f)
                ),
                startX = bx - stalkWidth,
                endX = bx + stalkWidth
            )
        )

        // Culm Rim Outline
        drawPath(
            path = culmPath,
            color = primaryLight.copy(alpha = 0.45f),
            style = Stroke(width = 1.0f)
        )

        // 3. Bamboo Node Ring (Joint)
        val nodeY = segTopY
        val nodeX = tx
        drawOval(
            color = primaryLight.copy(alpha = 0.95f),
            topLeft = Offset(nodeX - stalkWidth * 0.65f, nodeY - stalkWidth * 0.22f),
            size = Size(stalkWidth * 1.3f, stalkWidth * 0.44f)
        )
        // Node Ridge Glow
        drawOval(
            color = primaryColor.copy(alpha = 0.85f),
            topLeft = Offset(nodeX - stalkWidth * 0.55f, nodeY - stalkWidth * 0.14f),
            size = Size(stalkWidth * 1.1f, stalkWidth * 0.28f)
        )

        // 4. Branchlets and Leaf Sprigs Emerging from Nodes
        val isLeftBranch = i % 2 == 0
        val branchAngle = if (isLeftBranch) -38f + (swayOffset * 0.4f) else 38f + (swayOffset * 0.4f)
        val branchLength = minDim * 0.12f * (0.6f + 0.4f * growthFactor)

        drawBambooBranchletCluster(
            nodeOrigin = Offset(nodeX, nodeY),
            angle = branchAngle,
            branchLength = branchLength,
            growthFactor = growthFactor,
            primaryColor = primaryColor,
            primaryLight = primaryLight,
            primaryDark = primaryDark,
            glowIntensity = glowIntensity,
            leafCount = if (i == nodeCount - 1) 4 else 3
        )
    }

    // 5. Crown Tip Shoots & Leaves at Apex
    drawBambooCrown(
        apex = Offset(center.x + swayOffset, stalkTopY),
        minDim = minDim,
        growthFactor = growthFactor,
        primaryColor = primaryColor,
        primaryLight = primaryLight,
        glowIntensity = glowIntensity
    )
}

private fun DrawScope.drawBambooBranchletCluster(
    nodeOrigin: Offset,
    angle: Float,
    branchLength: Float,
    growthFactor: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    glowIntensity: Float,
    leafCount: Int
) {
    val rad = Math.toRadians(angle.toDouble()).toFloat()
    val branchEnd = Offset(
        nodeOrigin.x + sin(rad) * branchLength,
        nodeOrigin.y - cos(rad) * branchLength * 0.65f
    )

    // Draw Branchlet Stem
    drawLine(
        color = primaryDark.copy(alpha = 0.90f),
        start = nodeOrigin,
        end = branchEnd,
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = primaryLight.copy(alpha = 0.60f),
        start = nodeOrigin,
        end = branchEnd,
        strokeWidth = 1.0f,
        cap = StrokeCap.Round
    )

    // Fan of Lanceolate Bamboo Leaves
    val leafSpreadAngles = when (leafCount) {
        4 -> listOf(-28f, -10f, 10f, 28f)
        else -> listOf(-24f, 0f, 24f)
    }

    for (leafAngle in leafSpreadAngles) {
        val totalLeafRot = angle + leafAngle + (if (angle < 0) -15f else 15f)
        drawSingleBambooLeaf(
            tip = branchEnd,
            scale = (0.75f + 0.45f * growthFactor) * (branchLength / 45f),
            rotation = totalLeafRot,
            leafColor = primaryColor,
            rimColor = primaryLight
        )
    }
}

private fun DrawScope.drawBambooCrown(
    apex: Offset,
    minDim: Float,
    growthFactor: Float,
    primaryColor: Color,
    primaryLight: Color,
    glowIntensity: Float
) {
    // Apex Glow Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryLight.copy(alpha = 0.35f * glowIntensity),
                primaryColor.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = apex,
            radius = minDim * 0.14f * growthFactor
        ),
        radius = minDim * 0.14f * growthFactor,
        center = apex
    )

    val crownAngles = listOf(-35f, -14f, 0f, 14f, 35f)
    for (ang in crownAngles) {
        drawSingleBambooLeaf(
            tip = apex,
            scale = (0.85f + 0.50f * growthFactor) * (minDim / 340f),
            rotation = ang,
            leafColor = primaryColor,
            rimColor = primaryLight
        )
    }
}

private fun DrawScope.drawSingleBambooLeaf(
    tip: Offset,
    scale: Float,
    rotation: Float,
    leafColor: Color,
    rimColor: Color
) {
    rotate(degrees = rotation, pivot = tip) {
        val len = scale * 48f
        val wid = scale * 12f

        val path = Path().apply {
            moveTo(tip.x, tip.y) // Base attachment
            cubicTo(
                tip.x - wid * 0.8f, tip.y - len * 0.35f,
                tip.x - wid * 1.1f, tip.y - len * 0.70f,
                tip.x, tip.y - len // Pointed tip
            )
            cubicTo(
                tip.x + wid * 1.1f, tip.y - len * 0.70f,
                tip.x + wid * 0.8f, tip.y - len * 0.35f,
                tip.x, tip.y
            )
            close()
        }

        // Leaf Body Fill
        drawPath(
            path = path,
            color = leafColor.copy(alpha = 0.92f)
        )

        // Leaf Rim Outline
        drawPath(
            path = path,
            color = rimColor.copy(alpha = 0.85f),
            style = Stroke(width = 1.0f)
        )

        // Leaf Center Midrib
        drawLine(
            color = rimColor.copy(alpha = 0.75f),
            start = tip,
            end = Offset(tip.x, tip.y - len * 0.92f),
            strokeWidth = 1.0f,
            cap = StrokeCap.Round
        )
    }
}

// =========================================================================
// 🪷 2. THEME: SACRED LOTUS — SACRED LOTUS FLOWER ON LILY PAD
// =========================================================================
private fun DrawScope.drawSacredLotusOnLilyPad(
    center: Offset,
    minDim: Float,
    bloomFactor: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    glowIntensity: Float
) {
    val waterCenter = Offset(center.x, center.y + minDim * 0.14f)

    // 1. Concentric Water Ripples Under Lily Pad
    for (rIndex in 0..2) {
        val ripplePhase = ((animClockSec * 0.4f + rIndex * 0.33f) % 1.0f)
        val rippleRadius = minDim * (0.20f + 0.22f * ripplePhase) * (0.85f + 0.15f * bloomFactor)
        val rippleAlpha = ((1.0f - ripplePhase) * 0.35f * glowIntensity).coerceIn(0f, 0.45f)

        drawOval(
            color = primaryLight.copy(alpha = rippleAlpha),
            topLeft = Offset(waterCenter.x - rippleRadius, waterCenter.y - rippleRadius * 0.40f),
            size = Size(rippleRadius * 2f, rippleRadius * 0.80f),
            style = Stroke(width = 1.4f)
        )
    }

    // 2. Floating Lily Pad (Disc with Notch & Radial Veins)
    val padWidth = minDim * 0.54f * (0.88f + 0.12f * bloomFactor)
    val padHeight = padWidth * 0.45f
    val padCenter = waterCenter

    val padPath = Path().apply {
        val rect = Rect(padCenter.x - padWidth / 2f, padCenter.y - padHeight / 2f, padCenter.x + padWidth / 2f, padCenter.y + padHeight / 2f)
        addOval(rect)
    }

    // Lily Pad Fill (Deep emerald-gold aquatic tone)
    drawPath(
        path = padPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF2E6038).copy(alpha = 0.95f),
                Color(0xFF1B4026).copy(alpha = 0.95f),
                primaryDark.copy(alpha = 0.95f)
            ),
            center = padCenter,
            radius = padWidth * 0.5f
        )
    )
    // Lily Pad Rim Glow
    drawPath(
        path = padPath,
        color = primaryLight.copy(alpha = 0.65f),
        style = Stroke(width = 1.6f)
    )

    // Lily Pad Radial Veins
    val veinAngles = listOf(-140f, -100f, -60f, -20f, 20f, 60f, 100f, 140f)
    for (vAngle in veinAngles) {
        val vRad = Math.toRadians(vAngle.toDouble()).toFloat()
        val vx = padCenter.x + cos(vRad) * (padWidth * 0.46f)
        val vy = padCenter.y + sin(vRad) * (padHeight * 0.46f)
        drawLine(
            color = primaryLight.copy(alpha = 0.35f),
            start = padCenter,
            end = Offset(vx, vy),
            strokeWidth = 1.0f
        )
    }

    // 3. Sacred Lotus Blossom Blooming Seated on the Lily Pad
    val lotusOrigin = Offset(padCenter.x, padCenter.y - minDim * 0.04f)
    val petalBaseScale = (minDim / 300f)

    // Outer Petal Tier (Expands broadly outward during inhale)
    val outerPetalCount = 8
    for (i in 0 until outerPetalCount) {
        val angle = (i * (360f / outerPetalCount))
        val petalExpansion = bloomFactor * 1.0f
        val rotAngle = angle * petalExpansion + (sin(animClockSec * 0.8f + i) * 3f)
        val petalScale = petalBaseScale * (0.85f + 0.35f * bloomFactor)

        drawSingleLotusPetal(
            tip = lotusOrigin,
            scale = petalScale,
            rotation = rotAngle,
            petalColor = primaryDark,
            rimColor = primaryLight
        )
    }

    // Mid Petal Tier (Radiant Golden Lotus Petals)
    val midPetalCount = 6
    for (i in 0 until midPetalCount) {
        val angle = (i * (360f / midPetalCount)) + 30f
        val petalExpansion = bloomFactor * 0.85f
        val rotAngle = angle * petalExpansion + (sin(animClockSec * 0.8f + i + 2) * 2f)
        val petalScale = petalBaseScale * (0.95f + 0.30f * bloomFactor)

        drawSingleLotusPetal(
            tip = lotusOrigin,
            scale = petalScale,
            rotation = rotAngle,
            petalColor = primaryColor,
            rimColor = primaryLight
        )
    }

    // Inner Petal Tier (Embraces the center bud)
    val innerPetalCount = 5
    for (i in 0 until innerPetalCount) {
        val angle = (i * (360f / innerPetalCount)) + 18f
        val petalExpansion = bloomFactor * 0.65f
        val rotAngle = angle * petalExpansion
        val petalScale = petalBaseScale * (0.75f + 0.25f * bloomFactor)

        drawSingleLotusPetal(
            tip = lotusOrigin,
            scale = petalScale,
            rotation = rotAngle,
            petalColor = Color(0xFFFFE082),
            rimColor = Color(0xFFFFF8E1)
        )
    }

    // 4. Central Golden Stamen Receptacle / Seed Pod
    val coreRadius = minDim * 0.055f * (0.75f + 0.25f * bloomFactor)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF9C4),
                Color(0xFFFFD54F),
                primaryColor
            ),
            center = lotusOrigin,
            radius = coreRadius
        ),
        radius = coreRadius,
        center = lotusOrigin
    )
    drawCircle(
        color = primaryLight,
        radius = coreRadius,
        center = lotusOrigin,
        style = Stroke(width = 1.2f)
    )

    // Stamen Pollen Dots
    val dotCount = 7
    for (d in 0 until dotCount) {
        val dAngle = (d * (360f / dotCount)) * (PI.toFloat() / 180f)
        val dx = lotusOrigin.x + cos(dAngle) * (coreRadius * 0.55f)
        val dy = lotusOrigin.y + sin(dAngle) * (coreRadius * 0.55f)
        drawCircle(
            color = primaryDark,
            radius = coreRadius * 0.18f,
            center = Offset(dx, dy)
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
        val len = scale * 52f
        val wid = scale * 22f

        val path = Path().apply {
            moveTo(tip.x, tip.y) // Base attachment
            cubicTo(
                tip.x - wid * 1.15f, tip.y - len * 0.35f,
                tip.x - wid * 1.20f, tip.y - len * 0.70f,
                tip.x, tip.y - len // Pointed apex
            )
            cubicTo(
                tip.x + wid * 1.20f, tip.y - len * 0.70f,
                tip.x + wid * 1.15f, tip.y - len * 0.35f,
                tip.x, tip.y
            )
            close()
        }

        // Petal Gradient Fill
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    rimColor.copy(alpha = 0.95f),
                    petalColor.copy(alpha = 0.92f),
                    petalColor.copy(alpha = 0.85f)
                ),
                startY = tip.y - len,
                endY = tip.y
            )
        )

        // Petal Rim
        drawPath(
            path = path,
            color = rimColor.copy(alpha = 0.90f),
            style = Stroke(width = 1.1f)
        )

        // Lotus Vein Rib
        drawLine(
            color = rimColor.copy(alpha = 0.65f),
            start = tip,
            end = Offset(tip.x, tip.y - len * 0.88f),
            strokeWidth = 1.0f
        )
    }
}

// =========================================================================
// 🌸 3. THEME: SAKURA TWILIGHT — BLOOMING CHERRY BLOSSOM & TWIG
// =========================================================================
private fun DrawScope.drawSakuraCherryBlossom(
    center: Offset,
    minDim: Float,
    bloomFactor: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    glowIntensity: Float
) {
    val branchStart = Offset(center.x - minDim * 0.34f, center.y + minDim * 0.22f)
    val branchEnd = Offset(center.x + minDim * 0.30f, center.y - minDim * 0.20f)
    val heroOrigin = center

    // 1. Organic Cherry Blossom Twig
    val twigPath = Path().apply {
        moveTo(branchStart.x, branchStart.y)
        cubicTo(
            center.x - minDim * 0.16f, center.y + minDim * 0.14f,
            center.x - minDim * 0.05f, center.y - minDim * 0.04f,
            branchEnd.x, branchEnd.y
        )
    }

    // Twig Body
    drawPath(
        path = twigPath,
        color = Color(0xFF3E1C2B).copy(alpha = 0.90f),
        style = Stroke(width = 4.2f, cap = StrokeCap.Round)
    )
    drawPath(
        path = twigPath,
        color = primaryLight.copy(alpha = 0.45f),
        style = Stroke(width = 1.2f, cap = StrokeCap.Round)
    )

    // 2. Side Blossom Buds on Twig
    val budPositions = listOf(
        Pair(Offset(center.x - minDim * 0.18f, center.y + minDim * 0.10f), -45f),
        Pair(Offset(center.x + minDim * 0.18f, center.y - minDim * 0.12f), 55f)
    )
    for ((budPos, budRot) in budPositions) {
        drawSakuraBud(
            origin = budPos,
            rotation = budRot,
            scale = (minDim / 400f) * (0.8f + 0.2f * bloomFactor),
            primaryColor = primaryColor,
            primaryLight = primaryLight
        )
    }

    // 3. Central Hero 5-Petal Cherry Blossom (Expands & Blooms with breath)
    val flowerScale = (minDim / 260f) * (0.60f + 0.40f * bloomFactor)
    val gentleSway = sin(animClockSec * 0.9f) * 4f

    rotate(degrees = gentleSway, pivot = heroOrigin) {
        // Draw 5 Notched Heart-Tipped Cherry Blossom Petals
        for (i in 0..4) {
            val petalAngle = (i * 72f)
            rotate(degrees = petalAngle, pivot = heroOrigin) {
                drawSakuraHeroPetal(
                    origin = heroOrigin,
                    scale = flowerScale,
                    bloomFactor = bloomFactor,
                    primaryColor = primaryColor,
                    primaryLight = primaryLight,
                    primaryDark = primaryDark
                )
            }
        }

        // 4. Pistils & Stamens Radiating from Center
        val stamenCount = 10
        val stamenLength = flowerScale * 26f * (0.75f + 0.25f * bloomFactor)

        for (s in 0 until stamenCount) {
            val sAngle = (s * (360f / stamenCount)) * (PI.toFloat() / 180f)
            val sx = heroOrigin.x + cos(sAngle) * stamenLength
            val sy = heroOrigin.y + sin(sAngle) * stamenLength

            // Stamen Filament Thread
            drawLine(
                color = primaryLight.copy(alpha = 0.85f),
                start = heroOrigin,
                end = Offset(sx, sy),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
            // Stamen Anther Dot (Glowing magenta/gold)
            drawCircle(
                color = Color(0xFFFFD1DC),
                radius = flowerScale * 2.4f,
                center = Offset(sx, sy)
            )
            drawCircle(
                color = Color(0xFFFF4081),
                radius = flowerScale * 1.4f,
                center = Offset(sx, sy)
            )
        }

        // 5. Central Pistil Core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF80AB),
                    primaryColor,
                    primaryDark
                ),
                center = heroOrigin,
                radius = flowerScale * 12f
            ),
            radius = flowerScale * 12f,
            center = heroOrigin
        )
        drawCircle(
            color = primaryLight.copy(alpha = 0.90f),
            radius = flowerScale * 12f,
            center = heroOrigin,
            style = Stroke(width = 1.2f)
        )
    }
}

private fun DrawScope.drawSakuraHeroPetal(
    origin: Offset,
    scale: Float,
    bloomFactor: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    val len = scale * 56f
    val wid = scale * 26f * (0.85f + 0.15f * bloomFactor)
    val notch = len * 0.18f

    val path = Path().apply {
        moveTo(origin.x, origin.y) // Center origin
        cubicTo(
            origin.x - wid * 1.15f, origin.y - len * 0.40f,
            origin.x - wid * 0.95f, origin.y - len,
            origin.x - wid * 0.45f, origin.y - len
        )
        // Distinctive Sakura Heart Notch Cleft at tip
        quadraticTo(
            origin.x, origin.y - len + notch,
            origin.x + wid * 0.45f, origin.y - len
        )
        cubicTo(
            origin.x + wid * 0.95f, origin.y - len,
            origin.x + wid * 1.15f, origin.y - len * 0.40f,
            origin.x, origin.y
        )
        close()
    }

    // Radiant Twilight Petal Gradient
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

    // Petal Outline
    drawPath(
        path = path,
        color = primaryLight.copy(alpha = 0.88f),
        style = Stroke(width = 1.1f)
    )
}

private fun DrawScope.drawSakuraBud(
    origin: Offset,
    rotation: Float,
    scale: Float,
    primaryColor: Color,
    primaryLight: Color
) {
    rotate(degrees = rotation, pivot = origin) {
        val s = scale * 14f
        drawOval(
            color = primaryColor,
            topLeft = Offset(origin.x - s * 0.5f, origin.y - s * 1.2f),
            size = Size(s, s * 1.4f)
        )
        drawOval(
            color = primaryLight,
            topLeft = Offset(origin.x - s * 0.5f, origin.y - s * 1.2f),
            size = Size(s, s * 1.4f),
            style = Stroke(width = 1.0f)
        )
    }
}

private fun DrawScope.drawSingleSakuraPetal(
    center: Offset,
    scale: Float,
    rotation: Float,
    petalColor: Color,
    rimColor: Color
) {
    rotate(degrees = rotation, pivot = center) {
        val len = scale * 26f
        val wid = scale * 12f
        val notch = len * 0.16f

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

        drawPath(path = path, color = petalColor)
        drawPath(path = path, color = rimColor, style = Stroke(width = 1.0f))
    }
}
