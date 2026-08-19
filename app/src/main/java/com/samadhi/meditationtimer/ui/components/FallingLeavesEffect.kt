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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.scale
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A single leaf particle with realistic aerodynamic gliding and in-place resting.
 */
private data class Leaf(
    // Position
    var xBase: Float,
    var currentX: Float,
    var y: Float,
    // Aerodynamic motion
    var speedY: Float,
    var driftVx: Float = 0f,
    val swayFreq: Float,
    val swayAmp: Float,
    val swayPhase: Float,
    val tumbleFreq: Float,
    val tumblePhase: Float,
    // Rotation
    var rotation: Float,
    var rotSpeed: Float,
    // Appearance
    val size: Float,
    val baseAlpha: Float,
    val restAlpha: Float,
    val color: Color,
    // Landing / resting transition
    var isSettling: Boolean = false,
    var settleProgress: Float = 0f,
    var settleDuration: Float = 1.1f,
    var settleStartRot: Float = 0f,
    var settleTargetRot: Float = 0f,
    // Settled resting state
    var isResting: Boolean = false,
    val restSwayPhase: Float = Random.nextFloat() * 6.28f
)

@Composable
fun FallingLeavesEffect(
    modifier: Modifier = Modifier,
    leafCount: Int = 16,
    triggerBurst: Int = 0,
    dragVelocity: Float = 0f,
    ambientAlpha: Float = 0.35f,
    isGlowing: Boolean = false,
    pileUpAtBottom: Boolean = false,
    sessionProgressRatio: Float = 0.0f,
    initialSettledLeavesCount: Int = 0,
    elapsedMeditationSeconds: Int = 0,
    isBlowingAway: Boolean = false,
    onBlowAwayComplete: () -> Unit = {},
    timerCenterXRatio: Float = 0.5f,
    timerCenterYRatio: Float = -1f,
    timerRadiusRatio: Float = 0f
) {
    val currentTheme = LocalZenTheme.current
    val maxRestingLeaves = 160
    val secondsPerLeaf = 8

    // Calculate baseline resting count (survives rotation / recomposition)
    val startingRestingCount = remember(currentTheme.id, pileUpAtBottom) {
        if (pileUpAtBottom) {
            val fromElapsed = elapsedMeditationSeconds / secondsPerLeaf
            maxOf(fromElapsed, initialSettledLeavesCount).coerceAtMost(maxRestingLeaves)
        } else {
            0
        }
    }

    val leaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<Leaf>().apply {
            if (pileUpAtBottom && startingRestingCount > 0) {
                addAll(
                    generatePreRestingLeaves(
                        count = startingRestingCount,
                        palette = currentTheme.particlePalette,
                        timerCenterXRatio = timerCenterXRatio,
                        timerCenterYRatio = timerCenterYRatio,
                        timerRadiusRatio = timerRadiusRatio
                    )
                )
            }
            addAll(generateFallingLeaves(leafCount, currentTheme.particlePalette, pileUpAtBottom))
        }
    }

    // Only catch up if there's a big background gap (> 12s) to avoid popping leaves during live view
    var lastObservedSec by remember { mutableIntStateOf(elapsedMeditationSeconds) }
    LaunchedEffect(elapsedMeditationSeconds) {
        val gap = elapsedMeditationSeconds - lastObservedSec
        if (pileUpAtBottom && gap >= 12) {
            val targetCount = (elapsedMeditationSeconds / secondsPerLeaf).coerceAtMost(maxRestingLeaves)
            val currentResting = leaves.filter { it.isResting }
            val missing = targetCount - currentResting.size
            if (missing > 0) {
                val newResting = generatePreRestingLeaves(
                    count = missing,
                    palette = currentTheme.particlePalette,
                    existingResting = currentResting,
                    timerCenterXRatio = timerCenterXRatio,
                    timerCenterYRatio = timerCenterYRatio,
                    timerRadiusRatio = timerRadiusRatio
                )
                leaves.addAll(newResting)
            }
        }
        lastObservedSec = elapsedMeditationSeconds
    }

    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            leaves.toList().filter { !it.isResting }.forEach { it.rotSpeed = (Random.nextFloat() * 16f - 8f) }
        }
    }

    var blowAwayProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isBlowingAway) {
        if (isBlowingAway) {
            val startTime = System.currentTimeMillis()
            val durationMs = 1200L
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

    LaunchedEffect(currentTheme.id, pileUpAtBottom) {
        var lastTimeNanos = 0L
        var totalElapsedSec = 0f

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.050f)
                    totalElapsedSec += dt

                    val toAdd = mutableListOf<Leaf>()
                    val snapshot = leaves.toList()
                    val restingSnapshot = snapshot.filter { it.isResting || it.isSettling }

                    snapshot.forEach { leaf ->
                        if (leaf.isResting) {
                            leaf.rotation += dt * 0.35f * sin(totalElapsedSec * 0.5f + leaf.restSwayPhase)
                            return@forEach
                        }

                        if (leaf.isSettling) {
                            leaf.settleProgress += dt / leaf.settleDuration
                            val t = leaf.settleProgress.coerceIn(0f, 1f)
                            val ease = t * (2f - t)
                            leaf.rotation = leaf.settleStartRot + (leaf.settleTargetRot - leaf.settleStartRot) * ease

                            if (leaf.settleProgress >= 1f) {
                                leaf.isSettling = false
                                leaf.isResting = true
                                leaf.speedY = 0f
                                leaf.driftVx = 0f
                                leaf.rotSpeed = 0f

                                val restingCount = restingSnapshot.size + 1
                                if (restingCount < maxRestingLeaves) {
                                    toAdd.add(
                                        createSingleFallingLeaf(
                                            currentTheme.particlePalette,
                                            spawnAbove = true
                                        )
                                    )
                                }
                            }
                            return@forEach
                        }

                        // Soft edge repulsion
                        if (leaf.xBase < 0.12f) {
                            leaf.driftVx += 0.008f * dt
                        } else if (leaf.xBase > 0.88f) {
                            leaf.driftVx -= 0.008f * dt
                        }
                        leaf.driftVx = leaf.driftVx.coerceIn(-0.018f, 0.018f)
                        leaf.xBase = (leaf.xBase + leaf.driftVx * dt).coerceIn(0.06f, 0.94f)

                        val lateralSway = sin(totalElapsedSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp
                        leaf.currentX = (leaf.xBase + lateralSway).coerceIn(0.04f, 0.96f)

                        val floatFactor = (1.0f - 0.18f * kotlin.math.abs(sin(totalElapsedSec * leaf.swayFreq + leaf.swayPhase)))
                        leaf.y += leaf.speedY * floatFactor * dt

                        val lateralVelocity = cos(totalElapsedSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp * leaf.swayFreq + leaf.driftVx
                        val aerodynamicBank = lateralVelocity * 80f
                        leaf.rotation += (leaf.rotSpeed * dt) + (aerodynamicBank * 0.02f)

                        if (!pileUpAtBottom && leaf.y > 1.08f) {
                            leaf.y = -(Random.nextFloat() * 0.16f + 0.05f)
                            leaf.xBase = Random.nextFloat() * 0.84f + 0.08f
                            leaf.currentX = leaf.xBase
                            leaf.driftVx = (Random.nextFloat() - 0.5f) * 0.015f
                            return@forEach
                        }

                        if (pileUpAtBottom) {
                            val (settleX, settleY, slopeTilt) = calculateGravityStack(
                                x = leaf.currentX,
                                restingLeaves = restingSnapshot,
                                timerCenterXRatio = timerCenterXRatio,
                                timerCenterYRatio = timerCenterYRatio,
                                timerRadiusRatio = timerRadiusRatio
                            )

                            if (leaf.y >= (settleY - 0.006f)) {
                                val restingCount = restingSnapshot.size
                                if (restingCount < maxRestingLeaves) {
                                    leaf.isSettling = true
                                    leaf.settleProgress = 0f
                                    leaf.settleDuration = Random.nextFloat() * 0.20f + 0.35f
                                    leaf.settleStartRot = leaf.rotation
                                    leaf.xBase = settleX
                                    leaf.currentX = settleX
                                    leaf.y = settleY

                                    val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                    val baseTarget = if (normDeg in 0f..180f) 90f else 270f
                                    var delta = baseTarget - normDeg
                                    if (delta > 180f) delta -= 360f
                                    if (delta < -180f) delta += 360f

                                    val organicAngleJitter = (Random.nextFloat() - 0.5f) * 36f
                                    leaf.settleTargetRot = leaf.rotation + delta + slopeTilt + organicAngleJitter

                                    leaf.speedY = 0f
                                    leaf.driftVx = 0f
                                } else {
                                    leaf.y = -(Random.nextFloat() * 0.20f + 0.06f)
                                    leaf.xBase = Random.nextFloat() * 0.84f + 0.08f
                                    leaf.currentX = leaf.xBase
                                    leaf.driftVx = (Random.nextFloat() - 0.5f) * 0.015f
                                }
                            }
                        }
                    }

                    if (toAdd.isNotEmpty()) {
                        leaves.addAll(toAdd)
                    }
                }
                lastTimeNanos = nowNanos
            }
        }
    }

    val shapePath = remember { Path() }
    val detailPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = animClock
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Use smallest dimension to ensure consistent particle scale across portrait and landscape
        val baseDimension = min(w, h)
        val particleScale = (baseDimension / 390f).coerceIn(0.80f, 1.30f)

        val breathingWave = (sin(t * 1.3f) * 0.5f + 0.5f)

        val renderLeaves = leaves.toList().sortedWith(
            compareBy<Leaf> { !it.isResting && !it.isSettling }
                .thenByDescending { it.y }
        )

        renderLeaves.forEach { leaf ->
            val px = leaf.currentX * w
            val py = leaf.y * h

            val blowOffset = if (blowAwayProgress > 0f) {
                val p = blowAwayProgress
                val dir = if (leaf.isResting) (leaf.currentX - 0.5f).let { if (it == 0f) 1f else kotlin.math.sign(it) } else 1f
                Offset(w * dir * p * 1.4f, -h * p * p * 0.65f)
            } else Offset.Zero

            val blowRot = if (blowAwayProgress > 0f) blowAwayProgress * 340f else 0f

            val alpha = when {
                blowAwayProgress > 0f -> (leaf.baseAlpha * ambientAlpha) * (1f - blowAwayProgress)
                else -> (leaf.baseAlpha * ambientAlpha).coerceIn(0.12f, 0.85f)
            }

            if (alpha < 0.01f) return@forEach

            val tumbleScaleX = if (!leaf.isResting && !leaf.isSettling) {
                val wave = cos(t * leaf.tumbleFreq + leaf.tumblePhase)
                (wave * 0.45f + 0.55f).coerceIn(0.20f, 1.0f)
            } else {
                1.0f
            }

            drawIndividualParticle(
                shapePath = shapePath,
                detailPath = detailPath,
                center = Offset(px + blowOffset.x, py + blowOffset.y),
                size = leaf.size * particleScale,
                scaleX = tumbleScaleX,
                rotation = leaf.rotation + blowRot,
                color = leaf.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                glowColor = currentTheme.primaryLight,
                particleType = currentTheme.particleType,
                isGlowing = isGlowing && !leaf.isResting,
                breathingGlowFactor = breathingWave,
                isResting = leaf.isResting || leaf.isSettling
            )
        }
    }
}

private fun calculateGravityStack(
    x: Float,
    restingLeaves: List<Leaf>,
    baseFloorY: Float = 0.948f,
    timerCenterXRatio: Float = 0.5f,
    timerCenterYRatio: Float = -1f,
    timerRadiusRatio: Float = 0f
): Triple<Float, Float, Float> {
    var landingX = x.coerceIn(0.08f, 0.92f)

    val topCeilingY = if (timerRadiusRatio > 0f && timerCenterYRatio >= 0f) {
        val dx = landingX - timerCenterXRatio
        val outerRadius = timerRadiusRatio + 0.04f
        if (kotlin.math.abs(dx) < outerRadius) {
            val arcY = timerCenterYRatio + sqrt((outerRadius * outerRadius - dx * dx).coerceAtLeast(0f))
            arcY.coerceIn(timerCenterYRatio + timerRadiusRatio, baseFloorY)
        } else 0.70f
    } else 0.70f

    val leafSpanX = 0.055f
    val liftPerLeaf = 0.0035f

    fun getSurfaceHeightAt(testX: Float): Float {
        var highestY = baseFloorY
        for (other in restingLeaves) {
            val dx = kotlin.math.abs(testX - other.currentX)
            if (dx < leafSpanX) {
                val influence = 1.0f - (dx / leafSpanX)
                val leafTop = other.y - (liftPerLeaf * influence)
                if (leafTop < highestY) {
                    highestY = leafTop
                }
            }
        }
        return highestY
    }

    val curY = getSurfaceHeightAt(landingX)
    val leftY = getSurfaceHeightAt((landingX - 0.035f).coerceIn(0.08f, 0.92f))
    val rightY = getSurfaceHeightAt((landingX + 0.035f).coerceIn(0.08f, 0.92f))

    if (leftY > curY + 0.0015f && leftY >= rightY) {
        landingX = (landingX - 0.024f).coerceIn(0.08f, 0.92f)
    } else if (rightY > curY + 0.0015f) {
        landingX = (landingX + 0.024f).coerceIn(0.08f, 0.92f)
    }

    val finalSettleY = (getSurfaceHeightAt(landingX) + (Random.nextFloat() * 0.0015f - 0.0007f))
        .coerceIn(topCeilingY, baseFloorY)

    val slope = (getSurfaceHeightAt(landingX - 0.025f) - getSurfaceHeightAt(landingX + 0.025f)) / 0.050f
    val slopeTilt = (slope * 45f).coerceIn(-18f, 18f)

    return Triple(landingX, finalSettleY, slopeTilt)
}

private fun DrawScope.drawIndividualParticle(
    shapePath: Path,
    detailPath: Path,
    center: Offset,
    size: Float,
    scaleX: Float,
    rotation: Float,
    color: Color,
    glowColor: Color,
    particleType: ParticleType,
    isGlowing: Boolean,
    breathingGlowFactor: Float,
    isResting: Boolean
) {
    rotate(degrees = rotation, pivot = center) {
        scale(scaleX = scaleX, scaleY = 1.0f, pivot = center) {
            shapePath.reset()
            detailPath.reset()

            val halfW = size * 0.44f
            val halfH = size * 0.94f

            when (particleType) {
                ParticleType.BAMBOO_LEAF -> {
                    shapePath.moveTo(center.x, center.y - halfH)
                    shapePath.cubicTo(center.x + halfW * 1.08f, center.y - halfH * 0.32f, center.x + halfW * 0.88f, center.y + halfH * 0.38f, center.x, center.y + halfH)
                    shapePath.cubicTo(center.x - halfW * 0.88f, center.y + halfH * 0.38f, center.x - halfW * 1.08f, center.y - halfH * 0.32f, center.x, center.y - halfH)
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
                    shapePath.moveTo(center.x, center.y - halfH * 0.95f)
                    shapePath.cubicTo(center.x + halfW * 1.38f, center.y - halfH * 0.40f, center.x + halfW * 1.28f, center.y + halfH * 0.45f, center.x, center.y + halfH)
                    shapePath.cubicTo(center.x - halfW * 1.28f, center.y + halfH * 0.45f, center.x - halfW * 1.38f, center.y - halfH * 0.40f, center.x, center.y - halfH * 0.95f)
                    shapePath.close()
                    detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                    detailPath.quadraticTo(center.x + halfW * 0.20f, center.y, center.x, center.y + halfH * 0.85f)
                }
                ParticleType.SAKURA_PETAL -> {
                    val topY = center.y - halfH * 0.95f
                    val cleft = halfH * 0.08f
                    shapePath.moveTo(center.x, center.y + halfH * 0.90f)
                    shapePath.cubicTo(center.x - halfW * 0.98f, center.y + halfH * 0.25f, center.x - halfW * 1.08f, center.y - halfH * 0.55f, center.x - halfW * 0.35f, topY)
                    shapePath.quadraticTo(center.x, topY + cleft, center.x + halfW * 0.35f, topY)
                    shapePath.cubicTo(center.x + halfW * 1.08f, center.y - halfH * 0.55f, center.x + halfW * 0.98f, center.y + halfH * 0.25f, center.x, center.y + halfH * 0.90f)
                    shapePath.close()
                    detailPath.moveTo(center.x, topY + cleft * 1.5f)
                    detailPath.lineTo(center.x, center.y + halfH * 0.75f)
                }
            }

            if (isGlowing) {
                val haloAlpha = (0.16f + 0.18f * breathingGlowFactor).coerceIn(0.10f, 0.38f)
                drawPath(path = shapePath, color = glowColor.copy(alpha = haloAlpha), style = Stroke(width = 3.5f))
            }

            drawPath(path = shapePath, color = color)

            drawPath(
                path = detailPath,
                color = Color(0xFF041014).copy(alpha = (0.28f * color.alpha).coerceIn(0.06f, 0.35f)),
                style = Stroke(width = 1.0f)
            )

            drawPath(
                path = shapePath,
                color = Color.Black.copy(alpha = (0.15f * color.alpha).coerceIn(0.04f, 0.20f)),
                style = Stroke(width = 0.7f)
            )
        }
    }
}

private fun createSingleFallingLeaf(palette: List<Color>, spawnAbove: Boolean = true): Leaf {
    val x = Random.nextFloat() * 0.84f + 0.08f
    val initialY = if (spawnAbove) -(Random.nextFloat() * 0.18f + 0.05f) else Random.nextFloat() * 0.90f
    val a = Random.nextFloat() * 0.20f + 0.65f
    return Leaf(
        xBase = x,
        currentX = x,
        y = initialY,
        speedY = Random.nextFloat() * 0.008f + 0.011f,
        driftVx = (Random.nextFloat() - 0.5f) * 0.015f,
        swayFreq = Random.nextFloat() * 0.25f + 0.35f,
        swayAmp = Random.nextFloat() * 0.025f + 0.030f,
        swayPhase = Random.nextFloat() * 6.28f,
        tumbleFreq = Random.nextFloat() * 0.45f + 0.40f,
        tumblePhase = Random.nextFloat() * 6.28f,
        rotation = Random.nextFloat() * 360f,
        rotSpeed = (Random.nextFloat() - 0.5f) * 4.0f,
        size = Random.nextFloat() * 6f + 16f,
        baseAlpha = a,
        restAlpha = a,
        color = palette.random()
    )
}

private fun generateFallingLeaves(count: Int, palette: List<Color>, pileUpAtBottom: Boolean): List<Leaf> {
    return List(count) { i ->
        val x = Random.nextFloat() * 0.84f + 0.08f
        val initialY = if (pileUpAtBottom) {
            -(i.toFloat() / count.toFloat()) * 2.20f - 0.08f
        } else {
            (i.toFloat() / count.toFloat()) * 1.1f - 0.1f
        }
        val a = Random.nextFloat() * 0.20f + 0.65f
        Leaf(
            xBase = x,
            currentX = x,
            y = initialY,
            speedY = Random.nextFloat() * 0.008f + 0.011f,
            driftVx = (Random.nextFloat() - 0.5f) * 0.015f,
            swayFreq = Random.nextFloat() * 0.25f + 0.35f,
            swayAmp = Random.nextFloat() * 0.025f + 0.030f,
            swayPhase = Random.nextFloat() * 6.28f,
            tumbleFreq = Random.nextFloat() * 0.45f + 0.40f,
            tumblePhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 4.0f,
            size = Random.nextFloat() * 6f + 16f,
            baseAlpha = a,
            restAlpha = a,
            color = palette.random()
        )
    }
}

private fun generatePreRestingLeaves(
    count: Int,
    palette: List<Color>,
    existingResting: List<Leaf> = emptyList(),
    timerCenterXRatio: Float = 0.5f,
    timerCenterYRatio: Float = -1f,
    timerRadiusRatio: Float = 0f
): List<Leaf> {
    val rng = Random(42 + existingResting.size)
    val result = mutableListOf<Leaf>()
    val allResting = existingResting.toMutableList()

    for (i in 0 until count) {
        val x = rng.nextFloat() * 0.84f + 0.08f
        val (settleX, stackedY, slopeTilt) = calculateGravityStack(
            x = x,
            restingLeaves = allResting,
            timerCenterXRatio = timerCenterXRatio,
            timerCenterYRatio = timerCenterYRatio,
            timerRadiusRatio = timerRadiusRatio
        )
        val baseAngle = if (rng.nextBoolean()) 90f else 270f
        val a = rng.nextFloat() * 0.20f + 0.65f
        val leaf = Leaf(
            xBase = settleX,
            currentX = settleX,
            y = stackedY,
            speedY = 0f,
            driftVx = 0f,
            swayFreq = rng.nextFloat() * 0.25f + 0.35f,
            swayAmp = 0f,
            swayPhase = rng.nextFloat() * 6.28f,
            tumbleFreq = 0f,
            tumblePhase = 0f,
            rotation = baseAngle + slopeTilt + (rng.nextFloat() - 0.5f) * 36f,
            rotSpeed = 0f,
            size = rng.nextFloat() * 6f + 16f,
            baseAlpha = a,
            restAlpha = a,
            color = palette.random(rng),
            isResting = true,
            restSwayPhase = rng.nextFloat() * 6.28f
        )
        result.add(leaf)
        allResting.add(leaf)
    }
    return result
}