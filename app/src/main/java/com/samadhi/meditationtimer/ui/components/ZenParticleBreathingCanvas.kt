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
import androidx.compose.ui.geometry.Rect
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
    val alphaBase: Float
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier
) {
    val zenTheme = LocalZenTheme.current

    // Smooth ambient time clock
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

    // Precompute bounded ambient particles
    val ambientParticles = remember {
        val rand = Random(42)
        List(30) {
            AmbientBotanicalParticle(
                relX = 0.12f + rand.nextFloat() * 0.76f,
                relY = 0.10f + rand.nextFloat() * 0.80f,
                speedY = 0.015f + rand.nextFloat() * 0.025f,
                driftSpeedX = 0.4f + rand.nextFloat() * 0.8f,
                baseScale = 0.65f + rand.nextFloat() * 0.65f,
                initialRotation = rand.nextFloat() * 360f,
                rotSpeed = (rand.nextFloat() - 0.5f) * 45f,
                alphaBase = 0.35f + rand.nextFloat() * 0.45f
            )
        }
    }

    // Continuous breath growth factor [0.0f .. 1.0f]
    val growthFactor = when (currentPhase) {
        BreathPhase.INHALE -> phaseProgress
        BreathPhase.HOLD_FULL -> 1.0f
        BreathPhase.EXHALE -> (1.0f - phaseProgress)
        BreathPhase.HOLD_EMPTY -> 0.0f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val minDim = minOf(width, height)
        val center = Offset(width / 2f, height / 2f)

        when (zenTheme.id) {
            ZenThemeId.BAMBOO_TEAL -> {
                drawMoonlitBambooBreathing(
                    center = center,
                    minDim = minDim,
                    growthFactor = growthFactor,
                    currentPhase = currentPhase,
                    animClockSec = animClockSec,
                    primaryColor = zenTheme.primary,
                    primaryLight = zenTheme.primaryLight,
                    primaryDark = zenTheme.primaryDark,
                    ambientParticles = ambientParticles
                )
            }
            ZenThemeId.SACRED_LOTUS -> {
                drawSacredLotusBreathing(
                    center = center,
                    minDim = minDim,
                    growthFactor = growthFactor,
                    currentPhase = currentPhase,
                    animClockSec = animClockSec,
                    primaryColor = zenTheme.primary,
                    primaryLight = zenTheme.primaryLight,
                    primaryDark = zenTheme.primaryDark,
                    ambientParticles = ambientParticles
                )
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                drawSakuraTwilightBreathing(
                    center = center,
                    minDim = minDim,
                    growthFactor = growthFactor,
                    currentPhase = currentPhase,
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
// 1. MOONLIT BAMBOO: LIVING BAMBOO STALK WITH GROWING BRANCHES & UNFURLING LEAVES
// =========================================================================

private fun DrawScope.drawMoonlitBambooBreathing(
    center: Offset,
    minDim: Float,
    growthFactor: Float,
    currentPhase: BreathPhase,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    ambientParticles: List<AmbientBotanicalParticle>
) {
    val stalkBaseX = center.x
    val stalkBottomY = center.y + minDim * 0.42f
    val stalkTopY = center.y - minDim * 0.40f
    val stalkTotalHeight = stalkBottomY - stalkTopY
    val stalkWidth = minDim * 0.048f

    // Soft wind sway
    val swayOffset = sin(animClockSec * 1.2f) * (minDim * 0.015f)

    // 1. Draw Ambient Floating Bamboo Leaves (Strictly bounded to canvas)
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
            scale = pScale * 0.85f,
            rotation = pRot,
            fillColor = pColor,
            rimColor = pRim
        )
    }

    // 2. Draw Main Bamboo Culm (Segmented with Node Rings)
    val numSegments = 5
    val segmentHeight = stalkTotalHeight / numSegments

    for (i in 0 until numSegments) {
        val segBottomY = stalkBottomY - (i * segmentHeight)
        val segTopY = segBottomY - segmentHeight

        val segSwayBottom = swayOffset * (i.toFloat() / numSegments)
        val segSwayTop = swayOffset * ((i + 1).toFloat() / numSegments)

        val segPath = Path().apply {
            moveTo(stalkBaseX - stalkWidth / 2f + segSwayBottom, segBottomY)
            lineTo(stalkBaseX + stalkWidth / 2f + segSwayBottom, segBottomY)
            lineTo(stalkBaseX + stalkWidth * 0.46f + segSwayTop, segTopY)
            lineTo(stalkBaseX - stalkWidth * 0.46f + segSwayTop, segTopY)
            close()
        }

        // Bamboo culm gradient
        drawPath(
            path = segPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    primaryDark.copy(alpha = 0.95f),
                    primaryColor.copy(alpha = 0.95f),
                    primaryLight.copy(alpha = 0.95f),
                    primaryDark.copy(alpha = 0.95f)
                ),
                startX = stalkBaseX - stalkWidth / 2f,
                endX = stalkBaseX + stalkWidth / 2f
            )
        )

        // Node Ring Joint
        val nodeCenter = Offset(stalkBaseX + segSwayBottom, segBottomY)
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(primaryLight, primaryDark, primaryLight),
                startX = nodeCenter.x - stalkWidth * 0.65f,
                endX = nodeCenter.x + stalkWidth * 0.65f
            ),
            topLeft = Offset(nodeCenter.x - stalkWidth * 0.62f, nodeCenter.y - stalkWidth * 0.16f),
            size = Size(stalkWidth * 1.24f, stalkWidth * 0.32f)
        )

        // 3. Branches and Leaves Growing Outward from Nodes during Inhale
        if (i in 1..4) {
            val isRightBranch = (i % 2 == 1)
            val branchDir = if (isRightBranch) 1f else -1f
            val nodeX = stalkBaseX + segSwayBottom + (branchDir * stalkWidth * 0.5f)
            val nodeY = segBottomY

            // Branch growth calculation: grows outward and arches up with inhale
            val branchLength = minDim * 0.18f * (0.35f + 0.65f * growthFactor)
            val branchAngleDeg = if (isRightBranch) -38f + (swayOffset * 2f) else -142f - (swayOffset * 2f)
            val branchAngleRad = Math.toRadians(branchAngleDeg.toDouble()).toFloat()

            val branchTipX = nodeX + cos(branchAngleRad) * branchLength
            val branchTipY = nodeY + sin(branchAngleRad) * branchLength

            // Draw branch twig
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(primaryLight, primaryDark),
                    start = Offset(nodeX, nodeY),
                    end = Offset(branchTipX, branchTipY)
                ),
                start = Offset(nodeX, nodeY),
                end = Offset(branchTipX, branchTipY),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )

            // Clusters of Lanceolate Leaves Sprouting along Branch and Tip
            val leafScale = (minDim / 380f) * (0.25f + 0.75f * growthFactor)
            val leafCount = 3 + (i % 2)

            for (l in 0 until leafCount) {
                val tAlongBranch = (l + 1).toFloat() / (leafCount + 1)
                val leafOriginX = nodeX + (branchTipX - nodeX) * tAlongBranch
                val leafOriginY = nodeY + (branchTipY - nodeY) * tAlongBranch

                val fanAngle = (l - leafCount / 2f) * 22f * (0.5f + 0.5f * growthFactor)
                val leafRot = branchAngleDeg + fanAngle

                drawSingleBambooLeaf(
                    center = Offset(leafOriginX, leafOriginY),
                    scale = leafScale * (0.85f + (l * 0.15f)),
                    rotation = leafRot,
                    fillColor = primaryDark,
                    rimColor = primaryLight
                )
            }
        }
    }

    // Top Crown Bamboo Shoots
    val topX = stalkBaseX + swayOffset
    val topY = stalkTopY
    val crownScale = (minDim / 360f) * (0.35f + 0.65f * growthFactor)
    val crownAngles = listOf(-80f, -55f, -100f, -30f, -125f)
    for (ca in crownAngles) {
        drawSingleBambooLeaf(
            center = Offset(topX, topY),
            scale = crownScale,
            rotation = ca + (swayOffset * 1.5f),
            fillColor = primaryDark,
            rimColor = primaryLight
        )
    }
}

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
            moveTo(center.x, center.y) // Stalk attachment
            cubicTo(
                center.x - wid * 0.8f, center.y + len * 0.35f,
                center.x - wid, center.y + len * 0.75f,
                center.x, center.y + len // Leaf tip
            )
            cubicTo(
                center.x + wid, center.y + len * 0.75f,
                center.x + wid * 0.8f, center.y + len * 0.35f,
                center.x, center.y
            )
            close()
        }

        // Fill with deep bamboo jade
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
        // Elegant Leaf Spine Midrib
        drawLine(
            color = rimColor.copy(alpha = 0.80f),
            start = center,
            end = Offset(center.x, center.y + len * 0.92f),
            strokeWidth = 1.0f
        )
        // Leaf Outline
        drawPath(
            path = path,
            color = rimColor.copy(alpha = 0.75f),
            style = Stroke(width = 1.1f)
        )
    }
}

// =========================================================================
// 2. SACRED LOTUS: TRADITIONAL ASIAN ART MULTI-LOTUS & FLOATING PADS
// =========================================================================

private fun DrawScope.drawSacredLotusBreathing(
    center: Offset,
    minDim: Float,
    growthFactor: Float,
    currentPhase: BreathPhase,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    ambientParticles: List<AmbientBotanicalParticle>
) {
    // 1. Ambient Floating Lotus Petals (Strictly bounded to canvas)
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
            scale = pScale * 0.75f,
            rotation = pRot,
            petalColor = pColor,
            rimColor = pRim
        )
    }

    val baseY = center.y + minDim * 0.40f

    // 2. Broad Curved Lotus Pads (Lily Pads) at Base (Emerald Green with Vertical Gold Veins)
    // Left Lotus Pad
    drawAsianLotusPad(
        center = Offset(center.x - minDim * 0.28f, baseY - minDim * 0.05f),
        width = minDim * 0.46f,
        height = minDim * 0.26f,
        tiltDeg = -14f,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )

    // Right Lotus Pad
    drawAsianLotusPad(
        center = Offset(center.x + minDim * 0.26f, baseY - minDim * 0.02f),
        width = minDim * 0.44f,
        height = minDim * 0.24f,
        tiltDeg = 16f,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )

    // 3. Slender Rising Stems & Multi-Lotus Flowers at Varying Heights
    // (A) Top High Hero Lotus Flower (Rises high and blooms magnificently)
    val topStemX = center.x
    val topTargetY = center.y - minDim * 0.24f
    val topCurrentY = baseY - (baseY - topTargetY) * (0.75f + 0.25f * growthFactor)
    drawLotusStem(
        start = Offset(topStemX, baseY),
        end = Offset(topStemX, topCurrentY),
        stemColor = primaryDark,
        goldColor = primaryLight
    )
    drawMultiTierLotusBlossom(
        origin = Offset(topStemX, topCurrentY),
        scale = (minDim / 320f) * (0.70f + 0.35f * growthFactor),
        bloomRatio = growthFactor,
        animClockSec = animClockSec,
        primaryColor = primaryColor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )

    // (B) Left Mid-Tier Blooming Lotus (Elegantly cup-shaped bloom)
    val leftStemX = center.x - minDim * 0.24f
    val leftTargetY = center.y - minDim * 0.04f
    val leftCurrentY = baseY - (baseY - leftTargetY) * (0.60f + 0.40f * growthFactor)
    drawLotusStem(
        start = Offset(leftStemX + minDim * 0.04f, baseY),
        end = Offset(leftStemX, leftCurrentY),
        stemColor = primaryDark,
        goldColor = primaryLight
    )
    drawMultiTierLotusBlossom(
        origin = Offset(leftStemX, leftCurrentY),
        scale = (minDim / 340f) * (0.55f + 0.40f * growthFactor),
        bloomRatio = (growthFactor * 0.90f),
        animClockSec = animClockSec + 1.5f,
        primaryColor = primaryColor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )

    // (C) Right Mid-High Lotus Bud / Emerging Blossom (Rising on right)
    val rightStemX = center.x + minDim * 0.16f
    val rightTargetY = center.y - minDim * 0.10f
    val rightCurrentY = baseY - (baseY - rightTargetY) * (0.50f + 0.50f * growthFactor)
    drawLotusStem(
        start = Offset(rightStemX - minDim * 0.02f, baseY),
        end = Offset(rightStemX, rightCurrentY),
        stemColor = primaryDark,
        goldColor = primaryLight
    )
    drawLotusBud(
        origin = Offset(rightStemX, rightCurrentY),
        scale = (minDim / 350f) * (0.65f + 0.35f * growthFactor),
        openProgress = growthFactor,
        primaryColor = primaryColor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )

    // (D) Right Low Bud (Emerging above right pad)
    val lowRightStemX = center.x + minDim * 0.34f
    val lowRightTargetY = center.y + minDim * 0.10f
    val lowRightCurrentY = baseY - (baseY - lowRightTargetY) * (0.40f + 0.60f * growthFactor)
    drawLotusStem(
        start = Offset(lowRightStemX - minDim * 0.01f, baseY),
        end = Offset(lowRightStemX, lowRightCurrentY),
        stemColor = primaryDark,
        goldColor = primaryLight
    )
    drawLotusBud(
        origin = Offset(lowRightStemX, lowRightCurrentY),
        scale = (minDim / 380f) * (0.50f + 0.30f * growthFactor),
        openProgress = growthFactor * 0.70f,
        primaryColor = primaryColor,
        primaryLight = primaryLight,
        primaryDark = primaryDark
    )
}

private fun DrawScope.drawAsianLotusPad(
    center: Offset,
    width: Float,
    height: Float,
    tiltDeg: Float,
    primaryLight: Color,
    primaryDark: Color
) {
    rotate(degrees = tiltDeg, pivot = center) {
        val rect = Rect(center.x - width / 2f, center.y - height / 2f, center.x + width / 2f, center.y + height / 2f)
        val padPath = Path().apply {
            addOval(rect)
        }

        // Deep Teal-Emerald Lotus Pad Shading
        drawPath(
            path = padPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2D624F),
                    Color(0xFF1E4637),
                    Color(0xFF0F261E)
                ),
                center = center,
                radius = width * 0.55f
            )
        )

        // Radial Gold Vein Filigree
        val veinAngles = listOf(-150f, -120f, -90f, -60f, -30f, 0f, 30f, 60f, 90f, 120f, 150f)
        for (vDeg in veinAngles) {
            val vRad = Math.toRadians(vDeg.toDouble()).toFloat()
            val vx = center.x + cos(vRad) * (width * 0.46f)
            val vy = center.y + sin(vRad) * (height * 0.46f)
            drawLine(
                color = primaryLight.copy(alpha = 0.35f),
                start = center,
                end = Offset(vx, vy),
                strokeWidth = 1.1f
            )
        }

        // Outer Rim Accent
        drawPath(
            path = padPath,
            color = primaryLight.copy(alpha = 0.65f),
            style = Stroke(width = 1.3f)
        )
    }
}

private fun DrawScope.drawLotusStem(
    start: Offset,
    end: Offset,
    stemColor: Color,
    goldColor: Color
) {
    // Elegant slender stem
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(goldColor, stemColor, Color(0xFF132F24)),
            startY = end.y,
            endY = start.y
        ),
        start = start,
        end = end,
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawMultiTierLotusBlossom(
    origin: Offset,
    scale: Float,
    bloomRatio: Float,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    // Outer Tier Petals (Spread wide on bloom)
    val outerCount = 7
    for (i in 0 until outerCount) {
        val angle = (i - (outerCount - 1) / 2f) * (26f + 16f * bloomRatio)
        val sway = sin(animClockSec * 1.1f + i) * 2f
        val petalScale = scale * (0.80f + 0.35f * bloomRatio)
        drawSingleLotusPetal(
            tip = origin,
            scale = petalScale,
            rotation = angle + sway - 90f,
            petalColor = primaryDark,
            rimColor = primaryLight
        )
    }

    // Mid Tier Petals (Lush Coral/Rose Petals)
    val midCount = 5
    for (i in 0 until midCount) {
        val angle = (i - (midCount - 1) / 2f) * (20f + 12f * bloomRatio)
        val sway = sin(animClockSec * 1.1f + i + 2) * 1.5f
        val petalScale = scale * (0.90f + 0.25f * bloomRatio)
        drawSingleLotusPetal(
            tip = origin,
            scale = petalScale,
            rotation = angle + sway - 90f,
            petalColor = primaryColor,
            rimColor = primaryLight
        )
    }

    // Inner Stamen Glow Pod
    val podRadius = scale * 10f * (0.6f + 0.4f * bloomRatio)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primaryLight, primaryColor, primaryDark),
            center = origin,
            radius = podRadius
        ),
        radius = podRadius,
        center = origin
    )
    drawCircle(
        color = primaryLight.copy(alpha = 0.90f),
        radius = podRadius,
        center = origin,
        style = Stroke(width = 1.2f)
    )
}

private fun DrawScope.drawLotusBud(
    origin: Offset,
    scale: Float,
    openProgress: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    val budHeight = scale * 40f
    val budWidth = scale * 18f * (0.8f + 0.3f * openProgress)

    val path = Path().apply {
        moveTo(origin.x, origin.y)
        cubicTo(
            origin.x - budWidth, origin.y - budHeight * 0.4f,
            origin.x - budWidth * 0.6f, origin.y - budHeight,
            origin.x, origin.y - budHeight
        )
        cubicTo(
            origin.x + budWidth * 0.6f, origin.y - budHeight,
            origin.x + budWidth, origin.y - budHeight * 0.4f,
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
                primaryDark.copy(alpha = 0.90f)
            ),
            startY = origin.y - budHeight,
            endY = origin.y
        )
    )
    drawPath(
        path = path,
        color = primaryLight.copy(alpha = 0.85f),
        style = Stroke(width = 1.1f)
    )
}

private fun DrawScope.drawSingleLotusPetal(
    tip: Offset,
    scale: Float,
    rotation: Float,
    petalColor: Color,
    rimColor: Color
) {
    rotate(degrees = rotation, pivot = tip) {
        val len = scale * 48f
        val wid = scale * 19f

        val path = Path().apply {
            moveTo(tip.x, tip.y) // Base of petal
            cubicTo(
                tip.x - wid, tip.y + len * 0.45f,
                tip.x - wid * 0.65f, tip.y + len,
                tip.x, tip.y + len // Outer tip
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

// =========================================================================
// 3. SAKURA TWILIGHT: ORGANIC CHERRY BLOSSOM BRANCH WITH FLOWERING SHOOTS
// =========================================================================

private fun DrawScope.drawSakuraTwilightBreathing(
    center: Offset,
    minDim: Float,
    growthFactor: Float,
    currentPhase: BreathPhase,
    animClockSec: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    ambientParticles: List<AmbientBotanicalParticle>
) {
    // 1. Ambient Floating Sakura Petals (Strictly bounded to canvas)
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
            scale = pScale * 0.85f,
            rotation = pRot,
            petalColor = pColor,
            rimColor = pRim
        )
    }

    // 2. Organic Cherry Blossom Main Bough / Branch
    val branchStartX = center.x - minDim * 0.42f
    val branchStartY = center.y + minDim * 0.18f
    val branchMidX = center.x
    val branchMidY = center.y - minDim * 0.05f
    val branchEndX = center.x + minDim * 0.40f
    val branchEndY = center.y - minDim * 0.22f

    val boughPath = Path().apply {
        moveTo(branchStartX, branchStartY)
        quadraticTo(branchMidX, branchMidY, branchEndX, branchEndY)
    }

    // Draw Dark Wooden Sakura Bough
    drawPath(
        path = boughPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF382226), Color(0xFF523338), Color(0xFF2A161A)),
            start = Offset(branchStartX, branchStartY),
            end = Offset(branchEndX, branchEndY)
        ),
        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
    )

    // 3. Flowering Florets and Twigs Along the Branch that Bloom on Inhale
    data class SakuraFlowerSite(
        val posX: Float,
        val posY: Float,
        val twigAngle: Float,
        val twigLen: Float,
        val flowerBaseScale: Float,
        val bloomDelay: Float
    )

    val flowerSites = listOf(
        SakuraFlowerSite(center.x, center.y - minDim * 0.05f, -75f, minDim * 0.08f, 1.10f, 0.0f),       // Center Hero Blossom
        SakuraFlowerSite(center.x - minDim * 0.22f, center.y + minDim * 0.08f, -115f, minDim * 0.06f, 0.85f, 0.15f), // Left Blossom
        SakuraFlowerSite(center.x + minDim * 0.20f, center.y - minDim * 0.14f, -45f, minDim * 0.07f, 0.90f, 0.10f),  // Right Mid Blossom
        SakuraFlowerSite(center.x + minDim * 0.35f, center.y - minDim * 0.20f, -25f, minDim * 0.05f, 0.75f, 0.25f),  // Far Right Bud/Blossom
        SakuraFlowerSite(center.x - minDim * 0.10f, center.y + minDim * 0.02f, 105f, minDim * 0.06f, 0.80f, 0.20f)   // Lower Hanging Blossom
    )

    for ((idx, site) in flowerSites.withIndex()) {
        // Individual site bloom progress
        val siteBloom = ((growthFactor - site.bloomDelay) / (1.0f - site.bloomDelay)).coerceIn(0.2f, 1.0f)
        val twigRad = Math.toRadians(site.twigAngle.toDouble()).toFloat()
        val twigTipX = site.posX + cos(twigRad) * (site.twigLen * (0.6f + 0.4f * siteBloom))
        val twigTipY = site.posY + sin(twigRad) * (site.twigLen * (0.6f + 0.4f * siteBloom))

        // Twig line
        drawLine(
            color = Color(0xFF4A2D33),
            start = Offset(site.posX, site.posY),
            end = Offset(twigTipX, twigTipY),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )

        // 5-Petal Cherry Blossom Blooming at Twig Tip
        val floretScale = (minDim / 320f) * site.flowerBaseScale * (0.50f + 0.50f * siteBloom)
        drawSakura5PetalFlower(
            origin = Offset(twigTipX, twigTipY),
            scale = floretScale,
            bloomFactor = siteBloom,
            animClockSec = animClockSec + idx,
            primaryColor = primaryColor,
            primaryLight = primaryLight,
            primaryDark = primaryDark
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
    // 5 Notched Heart-Cleft Cherry Blossom Petals
    for (i in 0 until 5) {
        val baseAngle = i * 72f
        val sway = sin(animClockSec * 1.2f + i) * 2f
        val petalRot = baseAngle + sway

        rotate(degrees = petalRot, pivot = origin) {
            drawSakuraSingleHeartPetal(
                origin = origin,
                scale = scale,
                bloomFactor = bloomFactor,
                primaryColor = primaryColor,
                primaryLight = primaryLight,
                primaryDark = primaryDark
            )
        }
    }

    // Glowing Stamen Filaments & Anthers
    val stamenCount = 10
    for (s in 0 until stamenCount) {
        val sAngle = s * (360f / stamenCount)
        val sRad = Math.toRadians(sAngle.toDouble()).toFloat()
        val sLen = scale * 16f * (0.6f + 0.4f * bloomFactor)
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
            radius = scale * 2.0f,
            center = Offset(sx, sy)
        )
    }

    // Central Pistil Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF80AB), primaryColor, primaryDark),
            center = origin,
            radius = scale * 8f
        ),
        radius = scale * 8f,
        center = origin
    )
}

private fun DrawScope.drawSakuraSingleHeartPetal(
    origin: Offset,
    scale: Float,
    bloomFactor: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color
) {
    val len = scale * 42f * (0.80f + 0.20f * bloomFactor)
    val wid = scale * 20f * (0.85f + 0.15f * bloomFactor)
    val notch = len * 0.16f

    val path = Path().apply {
        moveTo(origin.x, origin.y)
        cubicTo(
            origin.x - wid * 1.1f, origin.y - len * 0.40f,
            origin.x - wid * 0.9f, origin.y - len,
            origin.x - wid * 0.45f, origin.y - len
        )
        // Distinctive Sakura Heart Notch Cleft at tip
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
        // Continuous smooth wrapped motion strictly within bounds
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
