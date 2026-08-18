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
import kotlin.math.exp
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
    var settleDuration: Float = 1.2f,
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
    elapsedMeditationSeconds: Int = 0,
    isBlowingAway: Boolean = false,
    onBlowAwayComplete: () -> Unit = {}
) {
    val currentTheme = LocalZenTheme.current

    // Active falling and settling leaves (staggered from above the top at session start)
    val activeLeaves = remember(currentTheme.id, leafCount) {
        mutableStateListOf<ActiveLeaf>().apply {
            addAll(generateInitialActiveLeaves(leafCount, currentTheme.particlePalette, pileUpAtBottom))
        }
    }

    // Dynamic ground elevation profile across 32 horizontal bins (0.94f is base floor)
    val groundProfile = remember(currentTheme.id) {
        FloatArray(32) { 0.94f }
    }

    // Determine initial mound count (from explicitly passed count or elapsed meditation seconds)
    val effectiveSeedCount = remember(initialSettledLeavesCount, elapsedMeditationSeconds) {
        if (initialSettledLeavesCount > 0) {
            initialSettledLeavesCount
        } else if (elapsedMeditationSeconds > 10) {
            (elapsedMeditationSeconds / 18).coerceIn(1, 85)
        } else {
            0
        }
    }

    // Settled leaves on the ground: Starts completely empty (0 leaves) at meditation start,
    // or loaded with accurate count if app was resumed or on summary overview screen
    val settledLeaves = remember(currentTheme.id, pileUpAtBottom, effectiveSeedCount) {
        mutableStateListOf<SettledLeaf>().apply {
            if (pileUpAtBottom && effectiveSeedCount > 0) {
                val initialPile = generatePreStackedMound(effectiveSeedCount, currentTheme.particlePalette)
                addAll(initialPile)

                initialPile.forEach { leaf ->
                    val bin = (leaf.x * 31).toInt().coerceIn(0, 31)
                    groundProfile[bin] = (groundProfile[bin] - 0.0035f).coerceAtLeast(0.76f)
                }
            }
        }
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
            val durationMs = 1100L
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

    // Physics update step: Inward breeze toward center pile, organic landing & stacking
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
                            // 🍂 Gentle, realistic landing cushioning (no teleporting or fast sideways darting)
                            leaf.settleProgress += dt / leaf.settleDuration
                            val t = leaf.settleProgress.coerceIn(0f, 1f)
                            // Ease-out quad curve for soft, natural deceleration into stillness
                            val ease = t * (2f - t)

                            leaf.currentX = leaf.settleStartX + (leaf.settleTargetX - leaf.settleStartX) * ease
                            leaf.y = leaf.settleStartY + (leaf.settleTargetY - leaf.settleStartY) * ease
                            leaf.rotation = leaf.settleStartRot + (leaf.settleTargetRot - leaf.settleStartRot) * ease

                            if (leaf.settleProgress >= 1f) {
                                if (settledLeaves.size < 95) {
                                    val bin = (leaf.settleTargetX * 31).toInt().coerceIn(0, 31)
                                    val layer = when {
                                        settledLeaves.size < 16 -> 0
                                        settledLeaves.size < 36 -> 1
                                        settledLeaves.size < 60 -> 2
                                        else -> 3
                                    }

                                    settledLeaves.add(
                                        SettledLeaf(
                                            x = leaf.settleTargetX,
                                            y = leaf.settleTargetY,
                                            rotation = leaf.settleTargetRot,
                                            size = leaf.size,
                                            alpha = (leaf.alpha * 1.15f).coerceIn(0.55f, 0.95f),
                                            color = leaf.color,
                                            depthLayer = layer
                                        )
                                    )

                                    // Organic pile growth profile (peaks in center)
                                    val depositAmount = 0.0055f
                                    groundProfile[bin] = (groundProfile[bin] - depositAmount).coerceAtLeast(0.76f)
                                    if (bin > 0) groundProfile[bin - 1] = (groundProfile[bin - 1] - depositAmount * 0.70f).coerceAtLeast(0.76f)
                                    if (bin < 31) groundProfile[bin + 1] = (groundProfile[bin + 1] - depositAmount * 0.70f).coerceAtLeast(0.76f)
                                }

                                // Recycle settled leaf to spawn at the top after a peaceful interval
                                leaf.isSettling = false
                                leaf.settleProgress = 0f
                                leaf.vx = 0f
                                leaf.vy = 0f
                                leaf.y = -(Random.nextFloat() * 0.18f + 0.05f)
                                leaf.xBase = Random.nextFloat() * 0.84f + 0.08f
                                leaf.currentX = leaf.xBase
                                leaf.rotation = Random.nextFloat() * 360f
                            }
                            continue
                        }

                        // Gentle natural inward breeze toward center while falling in lower half of screen
                        if (pileUpAtBottom && leaf.y > 0.20f) {
                            val inwardBias = (0.50f - leaf.xBase) * 0.022f * dt
                            leaf.vx += inwardBias
                            leaf.vx = leaf.vx.coerceIn(-0.025f, 0.025f)
                        }

                        leaf.xBase = (leaf.xBase + leaf.vx * dt).coerceIn(0.04f, 0.96f)
                        leaf.y += leaf.speedY * dt
                        leaf.rotation += leaf.rotSpeed * dt

                        // Velocity damping for smooth organic wind deceleration
                        leaf.vx *= (1f - dt * 1.5f).coerceAtLeast(0f)
                        leaf.rotSpeed *= (1f - dt * 1.2f)

                        val sway = sin(totalElapsedSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp
                        leaf.currentX = (leaf.xBase + sway).coerceIn(0.02f, 0.98f)

                        // Wrap falling leaves when leaving viewport bottom to top (if not piling)
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
                            if (leaf.y >= groundY - 0.008f) {
                                leaf.isSettling = true
                                leaf.settleProgress = 0f
                                leaf.settleDuration = Random.nextFloat() * 0.40f + 1.10f // 1.1s - 1.5s gentle deceleration
                                leaf.settleStartX = leaf.currentX
                                leaf.settleStartY = leaf.y
                                leaf.settleStartRot = leaf.rotation

                                // Land right where it touched down without snapping horizontally
                                leaf.settleTargetX = (leaf.currentX + (Random.nextFloat() * 0.010f - 0.005f)).coerceIn(0.04f, 0.96f)
                                leaf.settleTargetY = (groundY - 0.002f + (Random.nextFloat() * 0.006f - 0.003f)).coerceIn(0.76f, 0.95f)

                                // Natural resting angle: leaves gently settle into a horizontal tilt (±14°)
                                val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                val distTo90 = kotlin.math.abs(normDeg - 90f).let { if (it > 180f) 360f - it else it }
                                val distTo270 = kotlin.math.abs(normDeg - 270f).let { if (it > 180f) 360f - it else it }
                                val targetNorm = if (distTo90 <= distTo270) 90f else 270f

                                var delta = (targetNorm - normDeg)
                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f

                                val restJitter = Random.nextFloat() * 24f - 12f // -12° to +12° organic tilt
                                leaf.settleTargetRot = leaf.rotation + delta + restJitter
                            }
                        } else {
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

        // 1. Draw Settled Leaves on Ground (Organic Central Mound with Natural Overlap)
        if (pileUpAtBottom && settledLeaves.isNotEmpty()) {
            settledLeaves.forEach { settled ->
                val posX = settled.x * width
                val posY = settled.y * height

                // Gentle ambient breeze across resting mound
                val bedSway = sin(currentTimeSec * 0.6f + settled.x * 5.0f) * 0.6f
                val restingRot = settled.rotation + bedSway

                // Wind blow-away animation
                val blowOffset = if (blowAwayProgress > 0f) {
                    val progress = blowAwayProgress
                    val blowX = width * (0.8f * progress + settled.x * 0.4f)
                    val blowY = -height * (0.45f * progress * progress)
                    Offset(blowX, blowY)
                } else {
                    Offset.Zero
                }

                val finalAlpha = if (blowAwayProgress > 0f) {
                    settled.alpha * (1f - blowAwayProgress).coerceIn(0f, 1f)
                } else {
                    settled.alpha
                }

                val finalRotation = if (blowAwayProgress > 0f) {
                    restingRot + blowAwayProgress * 360f
                } else {
                    restingRot
                }

                drawIndividualParticle(
                    shapePath = shapePath,
                    detailPath = detailPath,
                    center = Offset(posX + blowOffset.x, posY + blowOffset.y),
                    size = settled.size * (width / 360f).coerceIn(0.85f, 1.25f),
                    rotation = finalRotation,
                    color = settled.color.copy(alpha = finalAlpha),
                    glowColor = currentTheme.primaryLight,
                    particleType = currentTheme.particleType,
                    isGlowing = false,
                    breathingGlowFactor = 0f,
                    isResting = true
                )
            }
        }

        // 2. Draw Active Falling Leaves
        activeLeaves.forEach { leaf ->
            val posX = leaf.currentX * width
            val posY = leaf.y * height

            val blowOffset = if (blowAwayProgress > 0f) {
                val progress = blowAwayProgress
                val blowX = width * (0.8f * progress)
                val blowY = -height * (0.45f * progress * progress)
                Offset(blowX, blowY)
            } else {
                Offset.Zero
            }

            val finalAlpha = if (blowAwayProgress > 0f) {
                leaf.alpha * (1f - blowAwayProgress).coerceIn(0f, 1f)
            } else {
                (leaf.alpha * ambientAlpha).coerceIn(0.12f, 0.90f)
            }

            val finalRotation = if (blowAwayProgress > 0f) {
                leaf.rotation + blowAwayProgress * 360f
            } else {
                leaf.rotation
            }

            drawIndividualParticle(
                shapePath = shapePath,
                detailPath = detailPath,
                center = Offset(posX + blowOffset.x, posY + blowOffset.y),
                size = leaf.size * (width / 360f).coerceIn(0.85f, 1.25f),
                rotation = finalRotation,
                color = leaf.color.copy(alpha = finalAlpha),
                glowColor = currentTheme.primaryLight,
                particleType = currentTheme.particleType,
                isGlowing = isGlowing,
                breathingGlowFactor = breathingWave,
                isResting = false
            )
        }
    }
}

// =========================================================================
// PARTICLE DRAWING FUNCTIONS
// =========================================================================

private fun DrawScope.drawIndividualParticle(
    shapePath: Path,
    detailPath: Path,
    center: Offset,
    size: Float,
    rotation: Float,
    color: Color,
    glowColor: Color,
    particleType: ParticleType,
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
                // 🌸 Realistic Ovate Cherry Blossom Petal with subtle natural tip cleft
                val topY = center.y - halfH * 0.95f
                val cleft = halfH * 0.08f
                shapePath.moveTo(center.x, center.y + halfH * 0.90f)
                shapePath.cubicTo(
                    center.x - halfW * 0.95f, center.y + halfH * 0.25f,
                    center.x - halfW * 1.05f, center.y - halfH * 0.55f,
                    center.x - halfW * 0.35f, topY
                )
                shapePath.quadraticTo(
                    center.x, topY + cleft,
                    center.x + halfW * 0.35f, topY
                )
                shapePath.cubicTo(
                    center.x + halfW * 1.05f, center.y - halfH * 0.55f,
                    center.x + halfW * 0.95f, center.y + halfH * 0.25f,
                    center.x, center.y + halfH * 0.90f
                )
                shapePath.close()

                detailPath.moveTo(center.x, topY + cleft * 1.5f)
                detailPath.lineTo(center.x, center.y + halfH * 0.75f)
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

private fun generateInitialActiveLeaves(count: Int, palette: List<Color>, pileUpAtBottom: Boolean): List<ActiveLeaf> {
    return List(count) { i ->
        val x = Random.nextFloat() * 0.84f + 0.08f
        // Stagger leaves so they start above the viewport and enter gently one by one
        val initialY = if (pileUpAtBottom) {
            -(i.toFloat() / count.toFloat()) * 0.85f - 0.06f
        } else {
            (i.toFloat() / count.toFloat()) * 1.1f - 0.1f
        }

        ActiveLeaf(
            xBase = x,
            currentX = x,
            y = initialY,
            speedY = Random.nextFloat() * 0.009f + 0.008f, // Peaceful, slow, meditative descent (~20-25s)
            swayFreq = Random.nextFloat() * 0.45f + 0.55f,
            swayAmp = Random.nextFloat() * 0.022f + 0.018f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 12f,
            size = Random.nextFloat() * 10f + 18f,
            alpha = Random.nextFloat() * 0.30f + 0.50f,
            color = palette.random()
        )
    }
}

/**
 * Generates an organic, natural Gaussian-mound leaf pile at the bottom center of the screen
 * for the session summary / overview screen.
 */
private fun generatePreStackedMound(count: Int, palette: List<Color>): List<SettledLeaf> {
    val random = Random(42)
    return List(count) { i ->
        val layer = when {
            i < count * 0.30f -> 0
            i < count * 0.62f -> 1
            i < count * 0.85f -> 2
            else -> 3
        }

        // Gaussian distributed X coordinates centered at 0.50f (spread sigma ~ 0.14f)
        val u1 = random.nextFloat().coerceAtLeast(0.0001f)
        val u2 = random.nextFloat()
        val z0 = kotlin.math.sqrt(-2.0f * kotlin.math.ln(u1)) * kotlin.math.cos(6.283185f * u2)
        val xPos = (0.50f + z0 * 0.13f).coerceIn(0.18f, 0.82f)

        // Mound elevation: Gaussian bell curve highest at x=0.50f, tapering to floor at flanks
        val distFromCenter = kotlin.math.abs(xPos - 0.50f)
        val moundHeight = exp(-(distFromCenter * distFromCenter) / (2f * 0.14f * 0.14f))

        val baseFloor = 0.945f
        val layerElev = when (layer) {
            0 -> 0.00f
            1 -> 0.035f * moundHeight
            2 -> 0.070f * moundHeight
            else -> 0.105f * moundHeight
        }
        val yPos = (baseFloor - layerElev - (random.nextFloat() * 0.015f)).coerceIn(0.76f, 0.95f)

        // Natural horizontal resting orientation with organic tilt
        val baseAngle = if (i % 2 == 0) 90f else 270f
        val tilt = (random.nextFloat() - 0.5f) * 26f

        SettledLeaf(
            x = xPos,
            y = yPos,
            rotation = baseAngle + tilt,
            size = random.nextFloat() * 8f + 20f,
            alpha = random.nextFloat() * 0.25f + 0.60f,
            color = palette[i % palette.size],
            depthLayer = layer
        )
    }
}
