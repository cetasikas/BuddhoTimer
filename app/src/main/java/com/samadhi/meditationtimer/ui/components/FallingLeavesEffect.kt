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
import androidx.compose.ui.graphics.drawscope.scale
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A single leaf particle with realistic aerodynamic gliding and in-place resting.
 */
private data class Leaf(
    // Position
    var xBase: Float,           // anchor X coordinate for sway
    var currentX: Float,        // actual drawn X coordinate
    var y: Float,               // normalised vertical position [0..1]
    // Aerodynamic motion
    var speedY: Float,          // vertical descent speed
    var driftVx: Float = 0f,    // steady lateral drift velocity (diagonal glide)
    val swayFreq: Float,        // frequency of back-and-forth wind flutter
    val swayAmp: Float,         // wide horizontal sweep amplitude (gliding arc)
    val swayPhase: Float,
    val tumbleFreq: Float,      // 3D flip flutter rate
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
    onBlowAwayComplete: () -> Unit = {}
) {
    val currentTheme = LocalZenTheme.current

    val leaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<Leaf>().apply {
            val seedCount = when {
                initialSettledLeavesCount > 0 -> initialSettledLeavesCount
                elapsedMeditationSeconds > 10 -> (elapsedMeditationSeconds / 8).coerceIn(1, 140)
                else -> 0
            }
            if (pileUpAtBottom && seedCount > 0) {
                addAll(generatePreRestingLeaves(seedCount, currentTheme.particlePalette))
            }
            addAll(generateFallingLeaves(leafCount, currentTheme.particlePalette, pileUpAtBottom))
        }
    }

    // Dynamic state synchronization: ensures leaf count updates when switching back from other apps
    LaunchedEffect(elapsedMeditationSeconds, pileUpAtBottom) {
        if (pileUpAtBottom && elapsedMeditationSeconds > 10) {
            val targetResting = (elapsedMeditationSeconds / 8).coerceIn(0, 140)
            val currentResting = leaves.count { it.isResting }
            if (currentResting < targetResting) {
                val needed = targetResting - currentResting
                val existingResting = leaves.filter { it.isResting }
                val newRestingLeaves = generatePreRestingLeaves(needed, currentTheme.particlePalette, existingResting)
                leaves.addAll(0, newRestingLeaves)
            }
        }
    }

    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            leaves.toList().filter { !it.isResting }.forEach { it.rotSpeed = (Random.nextFloat() * 16f - 8f) }
        }
    }

    // Wind blow-away on session exit
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

    val maxRestingLeaves = 150

    LaunchedEffect(currentTheme.id, pileUpAtBottom) {
        var lastTimeNanos = 0L
        var totalElapsedSec = 0f

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    totalElapsedSec += dt

                    val toAdd = mutableListOf<Leaf>()
                    val snapshot = leaves.toList()
                    val restingSnapshot = snapshot.filter { it.isResting }

                    snapshot.forEach { leaf ->
                        if (leaf.isResting) {
                            // Gentle ambient breathing sway for resting leaves
                            leaf.rotation += dt * 0.35f * sin(totalElapsedSec * 0.5f + leaf.restSwayPhase)
                            return@forEach
                        }

                        if (leaf.isSettling) {
                            // Smooth deceleration into resting position
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

                                // Spawn fresh airborne leaf at top
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

                        // --- Dynamic Gliding & Floating Flight Physics ---
                        // Natural diagonal drift & sweeping sway
                        leaf.xBase = (leaf.xBase + leaf.driftVx * dt).coerceIn(0.04f, 0.96f)
                        if (leaf.xBase <= 0.05f || leaf.xBase >= 0.95f) {
                            leaf.driftVx = -leaf.driftVx // bounce softly off edges
                        }

                        // Wide sweeping horizontal wave
                        val lateralSway = sin(totalElapsedSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp
                        leaf.currentX = (leaf.xBase + lateralSway).coerceIn(0.02f, 0.98f)

                        // Vertical float (slightly pauses at sway apex for realistic air resistance)
                        val floatFactor = (1.0f - 0.22f * kotlin.math.abs(sin(totalElapsedSec * leaf.swayFreq + leaf.swayPhase)))
                        leaf.y += leaf.speedY * floatFactor * dt

                        // Aerodynamic banking: leaf rotates into the direction of lateral glide
                        val lateralVelocity = (cos(totalElapsedSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp * leaf.swayFreq) + leaf.driftVx
                        val aerodynamicBank = lateralVelocity * 340f
                        leaf.rotation += (leaf.rotSpeed * dt) + (aerodynamicBank * 0.05f)

                        // Wrap when leaving bottom in non-pile mode
                        if (!pileUpAtBottom && leaf.y > 1.08f) {
                            leaf.y = -(Random.nextFloat() * 0.16f + 0.05f)
                            leaf.xBase = Random.nextFloat() * 0.84f + 0.08f
                            leaf.currentX = leaf.xBase
                            leaf.driftVx = (Random.nextFloat() - 0.5f) * 0.035f
                            return@forEach
                        }

                        // Pile mode: trigger smooth landing and tactile stacking on top of other leaves
                        if (pileUpAtBottom) {
                            val (stackedY, slopeTilt) = calculateStackY(leaf.currentX, restingSnapshot)
                            if (leaf.y >= (stackedY - 0.018f)) {
                                val restingCount = restingSnapshot.size
                                if (restingCount < maxRestingLeaves) {
                                    leaf.isSettling = true
                                    leaf.settleProgress = 0f
                                    leaf.settleDuration = Random.nextFloat() * 0.35f + 0.65f
                                    leaf.settleStartRot = leaf.rotation
                                    leaf.y = stackedY

                                    // Natural resting angle aligned with mound slope
                                    val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                    val distTo90 = kotlin.math.abs(normDeg - 90f).let { if (it > 180f) 360f - it else it }
                                    val distTo270 = kotlin.math.abs(normDeg - 270f).let { if (it > 180f) 360f - it else it }
                                    val targetNorm = if (distTo90 <= distTo270) 90f else 270f
                                    var delta = targetNorm - normDeg
                                    if (delta > 180f) delta -= 360f
                                    if (delta < -180f) delta += 360f
                                    leaf.settleTargetRot = leaf.rotation + delta + slopeTilt + (Random.nextFloat() * 22f - 11f)
                                    leaf.speedY = 0f
                                    leaf.driftVx = 0f
                                } else {
                                    leaf.y = -(Random.nextFloat() * 0.20f + 0.06f)
                                    leaf.xBase = Random.nextFloat() * 0.84f + 0.08f
                                    leaf.currentX = leaf.xBase
                                    leaf.driftVx = (Random.nextFloat() - 0.5f) * 0.035f
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

        val breathingWave = (sin(t * 1.3f) * 0.5f + 0.5f)
        val renderLeaves = leaves.toList()

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
                blowAwayProgress > 0f -> (if (leaf.isResting) leaf.restAlpha else leaf.baseAlpha * ambientAlpha) * (1f - blowAwayProgress)
                leaf.isResting || leaf.isSettling -> leaf.restAlpha
                else -> (leaf.baseAlpha * ambientAlpha).coerceIn(0.12f, 0.95f)
            }

            if (alpha < 0.01f) return@forEach

            // Realistic 3D tumbling flutter width modulation while airborne
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
                size = leaf.size * (w / 360f).coerceIn(0.90f, 1.30f),
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

// =========================================================================
// STACKING PHYSICS & HEIGHTMAP ENGINE
// =========================================================================

/**
 * Calculates the resting Y altitude and surface slope tilt angle for a leaf landing at [x].
 * Leaves stack vertically on top of overlapping leaves beneath them.
 */
private fun calculateStackY(
    x: Float,
    restingLeaves: List<Leaf>,
    baseFloorY: Float = 0.945f
): Pair<Float, Float> {
    if (restingLeaves.isEmpty()) {
        return Pair(baseFloorY, 0f)
    }

    // Nearby leaves within horizontal overlapping radius (~10% of screen width)
    val overlapRadius = 0.10f
    val nearby = restingLeaves.filter { kotlin.math.abs(it.currentX - x) < overlapRadius }
    val stackCount = nearby.size

    // Each overlapping leaf raises the landing altitude
    val stackLift = (stackCount * 0.018f).coerceAtMost(0.24f)
    val finalY = (baseFloorY - stackLift).coerceIn(0.70f, 0.96f)

    // Calculate natural mound slope tilt: tilts along the contour of the pile
    val leftCount = restingLeaves.count { it.currentX in (x - 0.14f)..<x }
    val rightCount = restingLeaves.count { it.currentX in (x + 0.001f)..(x + 0.14f) }
    val slopeTilt = ((leftCount - rightCount) * 4.2f).coerceIn(-28f, 28f)

    return Pair(finalY, slopeTilt)
}

// =========================================================================
// PARTICLE DRAWING WITH 3D FLUTTER SCALE & CONTACT SHADOW
// =========================================================================

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

            if (isResting) {
                // Subtle ambient contact shadow for realistic physical stacking depth
                drawPath(
                    path = shapePath,
                    color = Color.Black.copy(alpha = (0.28f * color.alpha).coerceIn(0.08f, 0.32f))
                )
            }

            if (isGlowing) {
                val haloAlpha = (0.16f + 0.18f * breathingGlowFactor).coerceIn(0.10f, 0.38f)
                drawPath(path = shapePath, color = glowColor.copy(alpha = haloAlpha), style = Stroke(width = 3.5f))
            }
            drawPath(path = shapePath, color = color)
            drawPath(
                path = detailPath,
                color = Color(0xFF041014).copy(alpha = (0.30f * color.alpha).coerceIn(0.08f, 0.40f)),
                style = Stroke(width = 1.2f)
            )
            // Subtle crisp edge contour
            drawPath(
                path = shapePath,
                color = Color.Black.copy(alpha = (0.18f * color.alpha).coerceIn(0.05f, 0.24f)),
                style = Stroke(width = 0.8f)
            )
        }
    }
}

// =========================================================================
// GENERATION HELPERS
// =========================================================================

private fun createSingleFallingLeaf(palette: List<Color>, spawnAbove: Boolean = true): Leaf {
    val x = Random.nextFloat() * 0.84f + 0.08f
    val initialY = if (spawnAbove) -(Random.nextFloat() * 0.22f + 0.06f) else Random.nextFloat() * 0.90f
    val a = Random.nextFloat() * 0.25f + 0.65f
    return Leaf(
        xBase = x,
        currentX = x,
        y = initialY,
        speedY = Random.nextFloat() * 0.015f + 0.024f, // visible, smooth meditative descent (~25-30s)
        driftVx = (Random.nextFloat() - 0.5f) * 0.035f, // natural lateral drift
        swayFreq = Random.nextFloat() * 0.40f + 0.45f,
        swayAmp = Random.nextFloat() * 0.07f + 0.07f, // wide organic sweep (7-14% screen width)
        swayPhase = Random.nextFloat() * 6.28f,
        tumbleFreq = Random.nextFloat() * 0.80f + 0.60f,
        tumblePhase = Random.nextFloat() * 6.28f,
        rotation = Random.nextFloat() * 360f,
        rotSpeed = (Random.nextFloat() - 0.5f) * 14f,
        size = Random.nextFloat() * 10f + 28f, // Enlarged 28..38dp
        baseAlpha = a,
        restAlpha = (a * 1.15f).coerceIn(0.75f, 0.96f),
        color = palette.random()
    )
}

private fun generateFallingLeaves(count: Int, palette: List<Color>, pileUpAtBottom: Boolean): List<Leaf> {
    return List(count) { i ->
        val x = Random.nextFloat() * 0.84f + 0.08f
        val initialY = if (pileUpAtBottom) -(i.toFloat() / count.toFloat()) * 0.90f - 0.05f
        else (i.toFloat() / count.toFloat()) * 1.1f - 0.1f
        val a = Random.nextFloat() * 0.25f + 0.65f
        Leaf(
            xBase = x,
            currentX = x,
            y = initialY,
            speedY = Random.nextFloat() * 0.015f + 0.024f,
            driftVx = (Random.nextFloat() - 0.5f) * 0.035f,
            swayFreq = Random.nextFloat() * 0.40f + 0.45f,
            swayAmp = Random.nextFloat() * 0.07f + 0.07f,
            swayPhase = Random.nextFloat() * 6.28f,
            tumbleFreq = Random.nextFloat() * 0.80f + 0.60f,
            tumblePhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 14f,
            size = Random.nextFloat() * 10f + 28f, // Enlarged 28..38dp
            baseAlpha = a,
            restAlpha = (a * 1.15f).coerceIn(0.75f, 0.96f),
            color = palette.random()
        )
    }
}

/** Pre-seeds resting leaves stacked realistically across the bottom for resume / summary screen. */
private fun generatePreRestingLeaves(
    count: Int,
    palette: List<Color>,
    existingResting: List<Leaf> = emptyList()
): List<Leaf> {
    val rng = Random(42 + existingResting.size)
    val result = mutableListOf<Leaf>()
    val allResting = existingResting.toMutableList()

    for (i in 0 until count) {
        val x = rng.nextFloat() * 0.82f + 0.09f
        val (stackedY, slopeTilt) = calculateStackY(x, allResting)
        val baseAngle = if (i % 2 == 0) 90f else 270f
        val a = rng.nextFloat() * 0.20f + 0.78f // Rich, vibrant resting leaves
        val leaf = Leaf(
            xBase = x,
            currentX = x,
            y = stackedY,
            speedY = 0f,
            driftVx = 0f,
            swayFreq = rng.nextFloat() * 0.45f + 0.35f,
            swayAmp = 0f,
            swayPhase = rng.nextFloat() * 6.28f,
            tumbleFreq = 0f,
            tumblePhase = 0f,
            rotation = baseAngle + slopeTilt + (rng.nextFloat() - 0.5f) * 24f,
            rotSpeed = 0f,
            size = rng.nextFloat() * 10f + 28f, // Enlarged 28..38dp
            baseAlpha = a,
            restAlpha = a,
            color = palette[i % palette.size],
            isResting = true,
            restSwayPhase = rng.nextFloat() * 6.28f
        )
        result.add(leaf)
        allResting.add(leaf)
    }
    return result
}
