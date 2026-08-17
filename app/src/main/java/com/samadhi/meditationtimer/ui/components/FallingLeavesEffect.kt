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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType
import kotlin.math.sin
import kotlin.random.Random

private data class ActiveLeaf(
    var xBase: Float,
    var y: Float,
    val speedY: Float,
    val swayFreq: Float,
    val swayAmp: Float,
    val swayPhase: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val size: Float,
    val alpha: Float,
    val color: Color,
    val isBurst: Boolean = false,
    var life: Float = 1.0f,
    // Landing / Settling transition state
    var isSettling: Boolean = false,
    var settleProgress: Float = 0.0f,
    var settleDuration: Float = 0.42f,
    var settleStartX: Float = 0f,
    var settleStartY: Float = 0f,
    var settleStartRot: Float = 0f,
    var settleTargetX: Float = 0f,
    var settleTargetY: Float = 0f,
    var settleTargetRot: Float = 0f
)

private data class SettledLeaf(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val size: Float,
    val alpha: Float,
    val color: Color,
    val depthLayer: Int = 0 // 0 = base bed, 1 = mid layer, 2 = top fresh
)

@Composable
fun FallingLeavesEffect(
    modifier: Modifier = Modifier,
    leafCount: Int = 16,
    triggerBurst: Int = 0,
    ambientAlpha: Float = 0.32f,
    isGlowing: Boolean = false,
    pileUpAtBottom: Boolean = false,
    sessionProgressRatio: Float = 0.0f
) {
    val currentTheme = LocalZenTheme.current

    // Active falling and settling leaves
    val activeLeaves = remember(currentTheme.id) {
        mutableStateListOf<ActiveLeaf>().apply {
            addAll(generateInitialActiveLeaves(leafCount, currentTheme.particlePalette))
        }
    }

    // Settled leaves on the ground (resting naturally)
    val settledLeaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<SettledLeaf>().apply {
            if (pileUpAtBottom && sessionProgressRatio >= 0.95f) {
                // Overview / Summary Screen: Show full stacked bed from completed meditation
                addAll(generatePreStackedMound(55, currentTheme.particlePalette))
            }
        }
    }

    // Dynamic ground elevation profile across 32 horizontal bins (0.95f is base floor)
    val groundProfile = remember(currentTheme.id) {
        FloatArray(32) { 0.95f }
    }

    // Burst trigger for user actions
    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            val bursts = generateBurstLeaves(12, currentTheme.particlePalette)
            activeLeaves.addAll(bursts)
        }
    }

    // Continuous monotonic clock ensuring immediate falling from frame 0
    val infiniteTransition = rememberInfiniteTransition(label = "leavesClock")
    val animClock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animClockVal"
    )

    // Physics update step with frame-rate independence and smooth settling cushioning
    LaunchedEffect(currentTheme.id, pileUpAtBottom) {
        var lastTimeNanos = 0L
        while (true) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    val timeSec = nowNanos / 1_000_000_000f
                    val iterator = activeLeaves.listIterator()

                    while (iterator.hasNext()) {
                        val leaf = iterator.next()

                        if (leaf.isSettling) {
                            // 🍂 Smooth Landing & Settling Phase (Decelerate, align to slope, cushion onto pile)
                            leaf.settleProgress += dt / leaf.settleDuration
                            val t = leaf.settleProgress.coerceIn(0f, 1f)
                            // Ease-out cubic for soft, organic touchdown
                            val ease = 1f - (1f - t) * (1f - t) * (1f - t)

                            leaf.y = leaf.settleStartY + (leaf.settleTargetY - leaf.settleStartY) * ease
                            leaf.rotation = leaf.settleStartRot + (leaf.settleTargetRot - leaf.settleStartRot) * ease

                            if (leaf.settleProgress >= 1f) {
                                // Transition is complete -> Permanently settle on the bed
                                if (settledLeaves.size < 130) {
                                    val bin = (leaf.settleTargetX * 31).toInt().coerceIn(0, 31)
                                    settledLeaves.add(
                                        SettledLeaf(
                                            x = leaf.settleTargetX,
                                            y = leaf.settleTargetY,
                                            rotation = leaf.settleTargetRot,
                                            size = leaf.size,
                                            alpha = (leaf.alpha * 1.25f).coerceIn(0.45f, 0.92f),
                                            color = leaf.color,
                                            depthLayer = if (settledLeaves.size < 40) 0 else if (settledLeaves.size < 85) 1 else 2
                                        )
                                    )

                                    // Organic Gaussian elevation rise so mounds build up smoothly
                                    val depositAmount = 0.010f
                                    groundProfile[bin] = (groundProfile[bin] - depositAmount).coerceAtLeast(0.65f)
                                    if (bin > 0) groundProfile[bin - 1] = (groundProfile[bin - 1] - depositAmount * 0.65f).coerceAtLeast(0.65f)
                                    if (bin < 31) groundProfile[bin + 1] = (groundProfile[bin + 1] - depositAmount * 0.65f).coerceAtLeast(0.65f)
                                    if (bin > 1) groundProfile[bin - 2] = (groundProfile[bin - 2] - depositAmount * 0.30f).coerceAtLeast(0.65f)
                                    if (bin < 30) groundProfile[bin + 2] = (groundProfile[bin + 2] - depositAmount * 0.30f).coerceAtLeast(0.65f)
                                }

                                // Recycle to top as a new falling leaf
                                leaf.isSettling = false
                                leaf.settleProgress = 0f
                                leaf.y = -(Random.nextFloat() * 0.12f + 0.04f)
                                leaf.xBase = Random.nextFloat()
                                leaf.rotation = Random.nextFloat() * 360f
                            }
                            continue
                        }

                        // Normal free-falling physics
                        leaf.y += leaf.speedY * dt
                        leaf.rotation += leaf.rotSpeed * dt

                        val currentX = (leaf.xBase + sin(timeSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp).coerceIn(0.02f, 0.98f)

                        if (leaf.isBurst) {
                            leaf.life -= dt * 0.35f
                            if (leaf.life <= 0f || leaf.y > 1.1f) {
                                iterator.remove()
                                continue
                            }
                        } else if (pileUpAtBottom) {
                            val bin = (currentX * 31).toInt().coerceIn(0, 31)
                            val groundY = groundProfile[bin]

                            // Trigger settling when leaf reaches ground surface
                            if (leaf.y >= groundY - 0.005f) {
                                leaf.isSettling = true
                                leaf.settleProgress = 0f
                                leaf.settleDuration = Random.nextFloat() * 0.15f + 0.35f // 0.35s - 0.50s cushioning
                                leaf.settleStartX = currentX
                                leaf.settleStartY = leaf.y
                                leaf.settleStartRot = leaf.rotation

                                // Slight horizontal drift on impact
                                val swayVel = (leaf.swayAmp * leaf.swayFreq)
                                leaf.settleTargetX = (currentX + (Random.nextFloat() * 0.016f - 0.008f)).coerceIn(0.03f, 0.97f)
                                leaf.settleTargetY = groundY + (Random.nextFloat() * 0.008f - 0.004f)

                                // Natural resting angle: leaves lie mostly horizontal, aligned with mound slope
                                val leftBin = (bin - 1).coerceAtLeast(0)
                                val rightBin = (bin + 1).coerceAtMost(31)
                                val slopeAngleDeg = (groundProfile[rightBin] - groundProfile[leftBin]) * 350f // Slope tilt

                                // Find nearest flat resting angle (around 0°/360° or 180°)
                                val normalizedRot = ((leaf.rotation % 360f) + 360f) % 360f
                                val baseRestAngle = if (normalizedRot in 90f..270f) 180f else if (normalizedRot > 270f) 360f else 0f
                                val restJitter = Random.nextFloat() * 26f - 13f // -13° to +13° natural tilt
                                val fullRotations = (leaf.rotation / 360f).toInt() * 360f

                                leaf.settleTargetRot = fullRotations + baseRestAngle + slopeAngleDeg + restJitter
                            }
                        } else {
                            // Standard ambient drift (recycle when exiting bottom)
                            if (leaf.y >= 1.06f) {
                                leaf.y = -(Random.nextFloat() * 0.10f + 0.04f)
                                leaf.xBase = Random.nextFloat()
                                leaf.rotation = Random.nextFloat() * 360f
                            }
                        }
                    }
                }
                lastTimeNanos = nowNanos
            }
        }
    }

    val shapePath = remember { Path() }
    val detailPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val currentTimeSec = animClock
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val breathingWave = (sin(currentTimeSec * 1.3f) * 0.5f + 0.5f)

        // 1. Draw Settled Leaves Stuck to Ground (Resting Naturally on Each Other with Organic Depth)
        if (pileUpAtBottom && settledLeaves.isNotEmpty()) {
            settledLeaves.forEach { settled ->
                val posX = settled.x * width
                val posY = settled.y * height
                
                // Subtle organic ambient micro-breeze across the resting bed
                val bedSway = sin(currentTimeSec * 0.7f + settled.x * 4.0f) * 0.75f
                val restingRot = settled.rotation + bedSway

                // Layer depth: base layer is slightly darker/deeper, top layer has full vibrant clarity
                val depthFactor = when (settled.depthLayer) {
                    0 -> 0.82f
                    1 -> 0.92f
                    else -> 1.0f
                }
                val effectiveAlpha = (settled.alpha * ambientAlpha * 1.35f * depthFactor).coerceIn(0.18f, 0.90f)

                drawThemeParticle(
                    particleType = currentTheme.particleType,
                    shapePath = shapePath,
                    detailPath = detailPath,
                    center = Offset(posX, posY),
                    size = settled.size,
                    rotation = restingRot,
                    color = settled.color.copy(alpha = effectiveAlpha),
                    glowColor = currentTheme.primaryLight,
                    isGlowing = isGlowing,
                    breathingGlowFactor = breathingWave,
                    isResting = true
                )
            }
        }

        // 2. Draw Active Falling & Settling Leaves
        activeLeaves.forEach { leaf ->
            val posX: Float
            val posY: Float
            val rot: Float

            if (leaf.isSettling) {
                // Use smooth settling interpolated coordinates
                posX = (leaf.settleStartX + (leaf.settleTargetX - leaf.settleStartX) * (leaf.settleProgress.coerceIn(0f, 1f))) * width
                posY = leaf.y * height
                rot = leaf.rotation
            } else {
                val sway = sin(currentTimeSec * leaf.swayFreq + leaf.swayPhase) * (leaf.swayAmp * width)
                posX = (leaf.xBase * width + sway).coerceIn(-30f, width + 30f)
                posY = leaf.y * height
                rot = leaf.rotation
            }

            val currentAlpha = (leaf.alpha * leaf.life * ambientAlpha).coerceIn(0f, 1f)

            if (currentAlpha > 0.01f && posY > -40f && posY < height + 40f) {
                drawThemeParticle(
                    particleType = currentTheme.particleType,
                    shapePath = shapePath,
                    detailPath = detailPath,
                    center = Offset(posX, posY),
                    size = leaf.size,
                    rotation = rot,
                    color = leaf.color.copy(alpha = currentAlpha),
                    glowColor = currentTheme.primaryLight,
                    isGlowing = isGlowing,
                    breathingGlowFactor = breathingWave,
                    isResting = leaf.isSettling
                )
            }
        }
    }
}

private fun DrawScope.drawThemeParticle(
    particleType: ParticleType,
    shapePath: Path,
    detailPath: Path,
    center: Offset,
    size: Float,
    rotation: Float,
    color: Color,
    glowColor: Color,
    isGlowing: Boolean,
    breathingGlowFactor: Float,
    isResting: Boolean
) {
    rotate(degrees = rotation, pivot = center) {
        shapePath.reset()
        detailPath.reset()

        val halfW = size * 0.40f
        val halfH = size * 0.88f

        when (particleType) {
            ParticleType.BAMBOO_LEAF -> {
                // 🎋 Pointed Bamboo / Bodhi Leaf with Stem & Natural Veins
                shapePath.moveTo(center.x, center.y - halfH)
                shapePath.cubicTo(
                    center.x + halfW * 1.05f, center.y - halfH * 0.32f,
                    center.x + halfW * 0.85f, center.y + halfH * 0.38f,
                    center.x, center.y + halfH
                )
                shapePath.cubicTo(
                    center.x - halfW * 0.85f, center.y + halfH * 0.38f,
                    center.x - halfW * 1.05f, center.y - halfH * 0.32f,
                    center.x, center.y - halfH
                )
                shapePath.close()

                detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                detailPath.lineTo(center.x, center.y + halfH * 0.88f)
                val stepY = halfH * 0.38f
                detailPath.moveTo(center.x, center.y - stepY * 0.6f)
                detailPath.quadraticTo(center.x + halfW * 0.5f, center.y - stepY * 0.85f, center.x + halfW * 0.65f, center.y - stepY * 0.7f)
                detailPath.moveTo(center.x, center.y - stepY * 0.6f)
                detailPath.quadraticTo(center.x - halfW * 0.5f, center.y - stepY * 0.85f, center.x - halfW * 0.65f, center.y - stepY * 0.7f)
            }

            ParticleType.LOTUS_PETAL -> {
                // 🪷 Broad Cupped Sacred Lotus Petal
                shapePath.moveTo(center.x, center.y - halfH * 0.95f)
                shapePath.cubicTo(
                    center.x + halfW * 1.35f, center.y - halfH * 0.40f,
                    center.x + halfW * 1.25f, center.y + halfH * 0.45f,
                    center.x, center.y + halfH
                )
                shapePath.cubicTo(
                    center.x - halfW * 1.25f, center.y + halfH * 0.45f,
                    center.x - halfW * 1.35f, center.y - halfH * 0.40f,
                    center.x, center.y - halfH * 0.95f
                )
                shapePath.close()

                detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                detailPath.quadraticTo(center.x + halfW * 0.20f, center.y, center.x, center.y + halfH * 0.85f)
            }

            ParticleType.SAKURA_PETAL -> {
                // 🌸 Iconic Notched Cherry Blossom Petal
                val topY = center.y - halfH * 0.95f
                shapePath.moveTo(center.x, center.y + halfH)
                shapePath.cubicTo(
                    center.x - halfW * 1.25f, center.y + halfH * 0.35f,
                    center.x - halfW * 1.15f, center.y - halfH * 0.45f,
                    center.x - halfW * 0.60f, topY
                )
                shapePath.quadraticTo(
                    center.x, topY + halfH * 0.24f,
                    center.x + halfW * 0.60f, topY
                )
                shapePath.cubicTo(
                    center.x + halfW * 1.15f, center.y - halfH * 0.45f,
                    center.x + halfW * 1.25f, center.y + halfH * 0.35f,
                    center.x, center.y + halfH
                )
                shapePath.close()

                detailPath.moveTo(center.x, topY + halfH * 0.3f)
                detailPath.lineTo(center.x, center.y + halfH * 0.8f)
            }
        }

        // Subtle breathing aura
        if (isGlowing) {
            val haloAlpha = (0.16f + 0.18f * breathingGlowFactor).coerceIn(0.10f, 0.38f)
            drawPath(
                path = shapePath,
                color = glowColor.copy(alpha = haloAlpha),
                style = Stroke(width = if (isResting) 3.0f else 3.8f)
            )
        }

        // Fill Particle Body
        drawPath(path = shapePath, color = color)

        // Draw Details
        val detailColor = Color(0xFF041014).copy(alpha = (0.24f * color.alpha).coerceIn(0.06f, 0.35f))
        drawPath(
            path = detailPath,
            color = detailColor,
            style = Stroke(width = 1.0f)
        )
    }
}

private fun generateInitialActiveLeaves(count: Int, palette: List<Color>): List<ActiveLeaf> {
    return List(count) { i ->
        ActiveLeaf(
            xBase = Random.nextFloat(),
            y = (i.toFloat() / count.toFloat()) * 1.1f - 0.1f, // Staggered initial heights so leaves are actively falling from moment 0
            speedY = Random.nextFloat() * 0.045f + 0.028f,
            swayFreq = Random.nextFloat() * 1.4f + 1.1f,
            swayAmp = Random.nextFloat() * 0.035f + 0.018f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 45f,
            size = Random.nextFloat() * 10f + 18f,
            alpha = Random.nextFloat() * 0.30f + 0.50f,
            color = palette.random()
        )
    }
}

private fun generateBurstLeaves(count: Int, palette: List<Color>): List<ActiveLeaf> {
    return List(count) {
        ActiveLeaf(
            xBase = Random.nextFloat() * 0.8f + 0.1f,
            y = Random.nextFloat() * 0.35f,
            speedY = Random.nextFloat() * 0.09f + 0.05f,
            swayFreq = Random.nextFloat() * 2.2f + 1.6f,
            swayAmp = Random.nextFloat() * 0.05f + 0.025f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 80f,
            size = Random.nextFloat() * 12f + 18f,
            alpha = Random.nextFloat() * 0.35f + 0.55f,
            color = palette.random(),
            isBurst = true
        )
    }
}

private fun generatePreStackedMound(count: Int, palette: List<Color>): List<SettledLeaf> {
    val random = Random(42)
    return List(count) { i ->
        val layer = when {
            i < count * 0.35f -> 0
            i < count * 0.70f -> 1
            else -> 2
        }
        val baseY = when (layer) {
            0 -> 0.950f + random.nextFloat() * 0.025f
            1 -> 0.900f + random.nextFloat() * 0.028f
            else -> 0.840f + random.nextFloat() * 0.032f
        }
        val goldX = ((i * 0.6180339887f) % 1.0f) * 0.90f + 0.05f
        val xJitter = (random.nextFloat() - 0.5f) * 0.035f
        val xRatio = (goldX + xJitter).coerceIn(0.04f, 0.96f)

        val baseAngle = if (i % 2 == 0) 0f else 180f
        val tilt = (random.nextFloat() - 0.5f) * 36f

        SettledLeaf(
            x = xRatio,
            y = baseY,
            rotation = baseAngle + tilt,
            size = random.nextFloat() * 8f + 20f,
            alpha = random.nextFloat() * 0.25f + 0.55f,
            color = palette[i % palette.size],
            depthLayer = layer
        )
    }
}
