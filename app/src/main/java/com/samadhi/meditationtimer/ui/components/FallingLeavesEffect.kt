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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var currentX: Float,
    var y: Float,
    val speedY: Float,
    val swayFreq: Float,
    val swayAmp: Float,
    val swayPhase: Float,
    var rotation: Float,
    var rotSpeed: Float,
    val size: Float,
    val alpha: Float,
    val color: Color,
    val isBurst: Boolean = false,
    var life: Float = 1.0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    // Landing / Settling transition state
    var isSettling: Boolean = false,
    var settleProgress: Float = 0.0f,
    var settleDuration: Float = 0.55f,
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
    dragVelocity: Float = 0f,
    ambientAlpha: Float = 0.32f,
    isGlowing: Boolean = false,
    pileUpAtBottom: Boolean = false,
    sessionProgressRatio: Float = 0.0f,
    initialSettledLeavesCount: Int = 0,
    isBlowingAway: Boolean = false,
    onBlowAwayComplete: () -> Unit = {}
) {
    val currentTheme = LocalZenTheme.current

    // Active falling and settling leaves
    val activeLeaves = remember(currentTheme.id, leafCount) {
        mutableStateListOf<ActiveLeaf>().apply {
            addAll(generateInitialActiveLeaves(leafCount, currentTheme.particlePalette))
        }
    }

    // Settled leaves on the ground (resting naturally & stacking in multi-tier mounds)
    val settledLeaves = remember(currentTheme.id, pileUpAtBottom, initialSettledLeavesCount) {
        mutableStateListOf<SettledLeaf>().apply {
            if (pileUpAtBottom) {
                if (initialSettledLeavesCount > 0) {
                    addAll(generatePreStackedMound(initialSettledLeavesCount, currentTheme.particlePalette))
                } else if (sessionProgressRatio >= 0.95f) {
                    addAll(generatePreStackedMound(55, currentTheme.particlePalette))
                }
            }
        }
    }

    // Dynamic ground elevation profile across 32 horizontal bins (0.95f is base floor)
    val groundProfile = remember(currentTheme.id) {
        FloatArray(32) { 0.95f }
    }

    // Gentle ambient flutter on preset / setting change
    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            activeLeaves.forEach { leaf ->
                leaf.rotSpeed = (Random.nextFloat() * 16f - 8f)
            }
        }
    }

    // Wind blow-away animation controller
    var blowAwayProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isBlowingAway) {
        if (isBlowingAway) {
            val startTime = System.currentTimeMillis()
            val durationMs = 950L
            while (true) {
                withFrameNanos { _ ->
                    val elapsed = System.currentTimeMillis() - startTime
                    blowAwayProgress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                }
                if (blowAwayProgress >= 1f) break
            }
            onBlowAwayComplete()
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

    // Physics update step with multi-tier stacking, serene gentle drift, and ultra-smooth cushioning
    LaunchedEffect(currentTheme.id, pileUpAtBottom) {
        var lastTimeNanos = 0L
        var totalElapsedSec = 0f

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    totalElapsedSec += dt
                    val iterator = activeLeaves.listIterator()

                    while (iterator.hasNext()) {
                        val leaf = iterator.next()

                        if (leaf.isSettling) {
                            // 🍂 Smooth Landing & Settling Phase (Decelerate, rotate to horizontal, cushion onto pile)
                            leaf.settleProgress += dt / leaf.settleDuration
                            val t = leaf.settleProgress.coerceIn(0f, 1f)
                            val ease = 1f - (1f - t) * (1f - t) * (1f - t)

                            leaf.currentX = leaf.settleStartX + (leaf.settleTargetX - leaf.settleStartX) * ease
                            leaf.y = leaf.settleStartY + (leaf.settleTargetY - leaf.settleStartY) * ease
                            leaf.rotation = leaf.settleStartRot + (leaf.settleTargetRot - leaf.settleStartRot) * ease

                            if (leaf.settleProgress >= 1f) {
                                if (settledLeaves.size < 130) {
                                    val bin = (leaf.settleTargetX * 31).toInt().coerceIn(0, 31)
                                    val layer = when {
                                        settledLeaves.size < 12 -> 0
                                        settledLeaves.size < 28 -> 1
                                        settledLeaves.size < 50 -> 2
                                        else -> 3
                                    }

                                    settledLeaves.add(
                                        SettledLeaf(
                                            x = leaf.settleTargetX,
                                            y = leaf.settleTargetY,
                                            rotation = leaf.settleTargetRot,
                                            size = leaf.size,
                                            alpha = (leaf.alpha * 1.25f).coerceIn(0.45f, 0.92f),
                                            color = leaf.color,
                                            depthLayer = layer
                                        )
                                    )

                                    val depositAmount = 0.024f
                                    groundProfile[bin] = (groundProfile[bin] - depositAmount).coerceAtLeast(0.52f)
                                    if (bin > 0) groundProfile[bin - 1] = (groundProfile[bin - 1] - depositAmount * 0.72f).coerceAtLeast(0.52f)
                                    if (bin < 31) groundProfile[bin + 1] = (groundProfile[bin + 1] - depositAmount * 0.72f).coerceAtLeast(0.52f)
                                    if (bin > 1) groundProfile[bin - 2] = (groundProfile[bin - 2] - depositAmount * 0.38f).coerceAtLeast(0.52f)
                                    if (bin < 30) groundProfile[bin + 2] = (groundProfile[bin + 2] - depositAmount * 0.38f).coerceAtLeast(0.52f)
                                }

                                leaf.isSettling = false
                                leaf.settleProgress = 0f
                                leaf.vx = 0f
                                leaf.vy = 0f
                                leaf.y = -(Random.nextFloat() * 0.12f + 0.04f)
                                leaf.xBase = Random.nextFloat()
                                leaf.currentX = leaf.xBase
                                leaf.rotation = Random.nextFloat() * 360f
                            }
                            continue
                        }

                        // Peaceful downward drift with gentle horizontal breeze
                        leaf.xBase = (leaf.xBase + leaf.vx * dt).coerceIn(0.01f, 0.99f)
                        leaf.y += leaf.speedY * dt
                        leaf.rotation += leaf.rotSpeed * dt

                        // Velocity damping for smooth organic wind deceleration
                        leaf.vx *= (1f - dt * 2.0f).coerceAtLeast(0f)
                        leaf.rotSpeed *= (1f - dt * 1.5f)

                        val sway = sin(totalElapsedSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp
                        leaf.currentX = (leaf.xBase + sway).coerceIn(0.01f, 0.99f)

                        // Wrap falling leaves when leaving viewport bottom to top
                        if (leaf.y > 1.08f && !pileUpAtBottom && !leaf.isBurst) {
                            leaf.y = -(Random.nextFloat() * 0.12f + 0.04f)
                            leaf.xBase = Random.nextFloat()
                            leaf.currentX = leaf.xBase
                            leaf.vx = 0f
                            leaf.vy = 0f
                        }

                        if (leaf.isBurst) {
                            leaf.life -= dt * 0.35f
                            if (leaf.life <= 0f || leaf.y > 1.1f) {
                                iterator.remove()
                                continue
                            }
                        } else if (pileUpAtBottom) {
                            val bin = (leaf.currentX * 31).toInt().coerceIn(0, 31)
                            val groundY = groundProfile[bin]

                            // Trigger settling when leaf reaches ground surface
                            if (leaf.y >= groundY - 0.004f) {
                                leaf.isSettling = true
                                leaf.settleProgress = 0f
                                leaf.settleDuration = Random.nextFloat() * 0.20f + 0.45f // 0.45s - 0.65s gentle cushioning
                                leaf.settleStartX = leaf.currentX
                                leaf.settleStartY = leaf.y
                                leaf.settleStartRot = leaf.rotation

                                // Resting Y corresponds to top of accumulated mound
                                leaf.settleTargetX = (leaf.currentX + (Random.nextFloat() * 0.012f - 0.006f)).coerceIn(0.03f, 0.97f)
                                leaf.settleTargetY = (groundY - 0.005f + (Random.nextFloat() * 0.006f - 0.003f)).coerceIn(0.52f, 0.95f)

                                // Natural resting angle: leaves lie HORIZONTALLY (90° or 270°)
                                val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                val distTo90 = kotlin.math.abs(normDeg - 90f).let { if (it > 180f) 360f - it else it }
                                val distTo270 = kotlin.math.abs(normDeg - 270f).let { if (it > 180f) 360f - it else it }
                                val targetNorm = if (distTo90 <= distTo270) 90f else 270f

                                var delta = (targetNorm - normDeg)
                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f

                                val leftBin = (bin - 1).coerceAtLeast(0)
                                val rightBin = (bin + 1).coerceAtMost(31)
                                val slopeAngleDeg = (groundProfile[rightBin] - groundProfile[leftBin]) * 240f // Slope tilt
                                val restJitter = Random.nextFloat() * 16f - 8f // -8° to +8° natural organic tilt

                                leaf.settleTargetRot = leaf.rotation + delta + slopeAngleDeg + restJitter
                            }
                        } else {
                            // Standard ambient drift (recycle when exiting bottom)
                            if (leaf.y >= 1.06f) {
                                leaf.y = -(Random.nextFloat() * 0.10f + 0.04f)
                                leaf.xBase = Random.nextFloat()
                                leaf.currentX = leaf.xBase
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

        // 1. Draw Settled Leaves Stuck to Ground (Multi-tier Stacking Mounds & Gentle Wind Blow-Away)
        if (pileUpAtBottom && settledLeaves.isNotEmpty()) {
            settledLeaves.forEach { settled ->
                val posX = settled.x * width
                val posY = settled.y * height
                
                // Subtle organic ambient micro-breeze across the resting bed
                val bedSway = sin(currentTimeSec * 0.7f + settled.x * 4.0f) * 0.75f
                val restingRot = settled.rotation + bedSway

                // Multi-tier layer depth: base layer is deeper, top mound tiers have vibrant clarity
                val depthFactor = when (settled.depthLayer) {
                    0 -> 0.76f
                    1 -> 0.88f
                    2 -> 0.96f
                    else -> 1.0f
                }
                val baseEffectiveAlpha = (settled.alpha * ambientAlpha * 1.35f * depthFactor).coerceIn(0.18f, 0.90f)

                // Blow-away gust wind kinematics
                val lift = if (blowAwayProgress > 0f) {
                    (blowAwayProgress * blowAwayProgress * (height * 1.15f)) * (1.0f + sin(currentTimeSec * 2.5f + settled.x * 4f) * 0.20f)
                } else 0f
                val driftX = if (blowAwayProgress > 0f) {
                    (blowAwayProgress * (width * 0.60f)) + sin(currentTimeSec * 2f + settled.x * 5f) * 35f
                } else 0f
                val finalRot = if (blowAwayProgress > 0f) {
                    restingRot + blowAwayProgress * 240f * (if (settled.x > 0.5f) 1f else -1f)
                } else restingRot
                val finalAlpha = if (blowAwayProgress > 0f) {
                    (baseEffectiveAlpha * (1.0f - blowAwayProgress * 1.15f)).coerceIn(0f, 1f)
                } else baseEffectiveAlpha

                if (finalAlpha > 0.01f) {
                    drawThemeParticle(
                        particleType = currentTheme.particleType,
                        shapePath = shapePath,
                        detailPath = detailPath,
                        center = Offset(posX + driftX, posY - lift),
                        size = settled.size,
                        rotation = finalRot,
                        color = settled.color.copy(alpha = finalAlpha),
                        glowColor = currentTheme.primaryLight,
                        isGlowing = isGlowing,
                        breathingGlowFactor = breathingWave,
                        isResting = true
                    )
                }
            }
        }

        // 2. Draw Active Falling & Settling Leaves
        activeLeaves.forEach { leaf ->
            val posX = leaf.currentX * width
            val posY = leaf.y * height
            val rot = leaf.rotation

            val lift = if (blowAwayProgress > 0f) {
                (blowAwayProgress * blowAwayProgress * (height * 0.9f))
            } else 0f
            val driftX = if (blowAwayProgress > 0f) {
                (blowAwayProgress * (width * 0.5f))
            } else 0f
            val finalAlpha = if (blowAwayProgress > 0f) {
                (leaf.alpha * leaf.life * ambientAlpha * (1.0f - blowAwayProgress * 1.15f)).coerceIn(0f, 1f)
            } else (leaf.alpha * leaf.life * ambientAlpha).coerceIn(0f, 1f)

            if (finalAlpha > 0.01f && (posY - lift) > -40f && (posY - lift) < height + 40f) {
                drawThemeParticle(
                    particleType = currentTheme.particleType,
                    shapePath = shapePath,
                    detailPath = detailPath,
                    center = Offset(posX + driftX, posY - lift),
                    size = leaf.size,
                    rotation = rot,
                    color = leaf.color.copy(alpha = finalAlpha),
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
        val x = Random.nextFloat()
        ActiveLeaf(
            xBase = x,
            currentX = x,
            y = (i.toFloat() / count.toFloat()) * 1.1f - 0.1f, // Staggered initial heights so leaves are actively falling from moment 0
            speedY = Random.nextFloat() * 0.010f + 0.009f, // Calm, peaceful, meditative descent
            swayFreq = Random.nextFloat() * 0.5f + 0.6f,
            swayAmp = Random.nextFloat() * 0.025f + 0.020f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 14f, // Gentle organic rocking
            size = Random.nextFloat() * 10f + 18f,
            alpha = Random.nextFloat() * 0.30f + 0.50f,
            color = palette.random()
        )
    }
}

private fun generateBurstLeaves(count: Int, palette: List<Color>): List<ActiveLeaf> {
    return List(count) {
        val x = Random.nextFloat() * 0.8f + 0.1f
        ActiveLeaf(
            xBase = x,
            currentX = x,
            y = Random.nextFloat() * 0.35f,
            speedY = Random.nextFloat() * 0.022f + 0.016f,
            swayFreq = Random.nextFloat() * 0.9f + 0.8f,
            swayAmp = Random.nextFloat() * 0.035f + 0.020f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 26f,
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
            i < count * 0.28f -> 0
            i < count * 0.58f -> 1
            i < count * 0.84f -> 2
            else -> 3
        }
        val baseY = when (layer) {
            0 -> 0.950f + random.nextFloat() * 0.018f
            1 -> 0.895f + random.nextFloat() * 0.022f
            2 -> 0.835f + random.nextFloat() * 0.025f
            else -> 0.770f + random.nextFloat() * 0.028f
        }
        val goldX = ((i * 0.6180339887f) % 1.0f) * 0.90f + 0.05f
        val xJitter = (random.nextFloat() - 0.5f) * 0.035f
        val xRatio = (goldX + xJitter).coerceIn(0.04f, 0.96f)

        // Flat horizontal orientation (90° pointing right, 270° pointing left)
        val baseAngle = if (i % 2 == 0) 90f else 270f
        val tilt = (random.nextFloat() - 0.5f) * 20f

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
