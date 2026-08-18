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

/**
 * A single leaf particle. Both falling and resting leaves use this type.
 * When isResting = true the leaf has settled on the ground in-place and
 * simply sways gently.  No separate SettledLeaf list is needed, which
 * eliminates the "disappear / reappear" flash.
 */
private data class Leaf(
    // Position
    var xBase: Float,           // base X used for sway calculation while falling
    var currentX: Float,        // actual drawn X
    var y: Float,               // normalised [0..1], >1 = above viewport top
    // Motion (zeroed when resting)
    var speedY: Float,
    var vx: Float = 0f,
    // Sway parameters (reused as ambient sway while resting)
    val swayFreq: Float,
    val swayAmp: Float,
    val swayPhase: Float,
    // Rotation
    var rotation: Float,
    var rotSpeed: Float,
    // Appearance
    val size: Float,
    val baseAlpha: Float,       // alpha while falling (ambient, semi-transparent)
    val restAlpha: Float,       // alpha while resting (fuller, more opaque)
    val color: Color,
    // Landing / rock-to-rest transition
    var isSettling: Boolean = false,
    var settleProgress: Float = 0f,
    var settleDuration: Float = 1.1f,
    var settleStartRot: Float = 0f,
    var settleTargetRot: Float = 0f,
    // Settled state
    var isResting: Boolean = false,
    val restSwayPhase: Float = Random.nextFloat() * 6.28f
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

    /**
     * Single unified leaf list.
     *  - Falling leaves: isResting = false, isSettling = false
     *  - Settling leaves: isResting = false, isSettling = true
     *  - Resting leaves:  isResting = true
     *
     * Created ONCE per session (remember keys: theme + pileUpAtBottom).
     * elapsedMeditationSeconds is used only in the initializer to
     * pre-seed the correct number of resting leaves on Activity recreation.
     */
    val leaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<Leaf>().apply {
            // Seed pre-resting leaves for resume / summary screen
            val seedCount = when {
                initialSettledLeavesCount > 0  -> initialSettledLeavesCount
                elapsedMeditationSeconds > 15  -> (elapsedMeditationSeconds / 20).coerceIn(1, 85)
                else -> 0
            }
            if (pileUpAtBottom && seedCount > 0) {
                addAll(generatePreRestingLeaves(seedCount, currentTheme.particlePalette, ambientAlpha))
            }

            // Active falling leaves staggered above the viewport
            addAll(generateFallingLeaves(leafCount, currentTheme.particlePalette, pileUpAtBottom, ambientAlpha))
        }
    }

    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            leaves.filter { !it.isResting }.forEach { it.rotSpeed = (Random.nextFloat() * 16f - 8f) }
        }
    }

    // Blow-away on exit
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

    // Max resting leaves allowed on the floor at once
    val maxRestingLeaves = 90

    LaunchedEffect(currentTheme.id, pileUpAtBottom) {
        var lastTimeNanos = 0L
        var totalElapsedSec = 0f

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    totalElapsedSec += dt

                    leaves.forEachIndexed { idx, leaf ->

                        if (leaf.isResting) {
                            // Resting: gentle ambient rock-in-place only
                            leaf.rotation += dt * 0.35f * sin(totalElapsedSec * 0.5f + leaf.restSwayPhase)
                            return@forEachIndexed
                        }

                        if (leaf.isSettling) {
                            // Rock gently to a flat resting angle
                            leaf.settleProgress += dt / leaf.settleDuration
                            val t = leaf.settleProgress.coerceIn(0f, 1f)
                            val ease = t * (2f - t)
                            leaf.rotation = leaf.settleStartRot + (leaf.settleTargetRot - leaf.settleStartRot) * ease

                            if (leaf.settleProgress >= 1f) {
                                // Mark as fully resting — leaf STAYS exactly where it is
                                leaf.isSettling = false
                                leaf.isResting = true
                                leaf.speedY = 0f
                                leaf.vx = 0f
                                leaf.rotSpeed = 0f

                                // Spawn a fresh falling leaf to keep the sky populated
                                val restingCount = leaves.count { it.isResting }
                                if (restingCount < maxRestingLeaves) {
                                    leaves.add(
                                        Leaf(
                                            xBase = Random.nextFloat() * 0.84f + 0.08f,
                                            currentX = 0f,
                                            y = -(Random.nextFloat() * 0.22f + 0.06f),
                                            speedY = Random.nextFloat() * 0.009f + 0.008f,
                                            swayFreq = Random.nextFloat() * 0.45f + 0.55f,
                                            swayAmp = Random.nextFloat() * 0.022f + 0.018f,
                                            swayPhase = Random.nextFloat() * 6.28f,
                                            rotation = Random.nextFloat() * 360f,
                                            rotSpeed = (Random.nextFloat() - 0.5f) * 12f,
                                            size = Random.nextFloat() * 10f + 18f,
                                            baseAlpha = Random.nextFloat() * 0.30f + 0.55f,
                                            restAlpha = Random.nextFloat() * 0.30f + 0.55f,
                                            color = currentTheme.particlePalette.random()
                                        ).also { it.currentX = it.xBase }
                                    )
                                }
                            }
                            return@forEachIndexed
                        }

                        // --- Falling physics ---
                        leaf.y += leaf.speedY * dt
                        leaf.rotation += leaf.rotSpeed * dt
                        leaf.vx *= (1f - dt * 1.2f).coerceAtLeast(0f)
                        leaf.rotSpeed *= (1f - dt * 0.8f)

                        val sway = sin(totalElapsedSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp
                        leaf.currentX = (leaf.xBase + sway + leaf.vx * dt).coerceIn(0.02f, 0.98f)

                        // Non-pile mode: wrap at the bottom
                        if (!pileUpAtBottom && leaf.y > 1.08f) {
                            leaf.y = -(Random.nextFloat() * 0.12f + 0.04f)
                            leaf.xBase = Random.nextFloat()
                            leaf.currentX = leaf.xBase
                            return@forEachIndexed
                        }

                        // Pile mode: trigger settle when leaf reaches ground level
                        if (pileUpAtBottom && leaf.y >= 0.92f) {
                            val restingCount = leaves.count { it.isResting }
                            if (restingCount < maxRestingLeaves) {
                                leaf.isSettling = true
                                leaf.settleProgress = 0f
                                leaf.settleDuration = Random.nextFloat() * 0.5f + 0.8f
                                leaf.settleStartRot = leaf.rotation
                                leaf.y = (leaf.y).coerceIn(0.88f, 0.96f)
                                leaf.currentX = leaf.currentX // freeze X in place

                                // Rock to a natural horizontal tilt
                                val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                val distTo90 = kotlin.math.abs(normDeg - 90f).let { if (it > 180f) 360f - it else it }
                                val distTo270 = kotlin.math.abs(normDeg - 270f).let { if (it > 180f) 360f - it else it }
                                val targetNorm = if (distTo90 <= distTo270) 90f else 270f
                                var delta = targetNorm - normDeg
                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f
                                leaf.settleTargetRot = leaf.rotation + delta + (Random.nextFloat() * 28f - 14f)
                                leaf.speedY = 0f
                                leaf.vx = 0f
                            } else {
                                // Floor is full — recycle this leaf back to top
                                leaf.y = -(Random.nextFloat() * 0.22f + 0.06f)
                                leaf.xBase = Random.nextFloat() * 0.84f + 0.08f
                                leaf.currentX = leaf.xBase
                                leaf.rotSpeed = (Random.nextFloat() - 0.5f) * 12f
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
        val t = animClock
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val breathingWave = (sin(t * 1.3f) * 0.5f + 0.5f)

        leaves.forEach { leaf ->
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
                else -> (leaf.baseAlpha * ambientAlpha).coerceIn(0.10f, 0.90f)
            }

            if (alpha < 0.01f) return@forEach

            drawIndividualParticle(
                shapePath = shapePath,
                detailPath = detailPath,
                center = Offset(px + blowOffset.x, py + blowOffset.y),
                size = leaf.size * (w / 360f).coerceIn(0.85f, 1.25f),
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
// PARTICLE DRAWING
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
                shapePath.moveTo(center.x, center.y - halfH)
                shapePath.cubicTo(center.x + halfW * 1.05f, center.y - halfH * 0.32f, center.x + halfW * 0.85f, center.y + halfH * 0.38f, center.x, center.y + halfH)
                shapePath.cubicTo(center.x - halfW * 0.85f, center.y + halfH * 0.38f, center.x - halfW * 1.05f, center.y - halfH * 0.32f, center.x, center.y - halfH)
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
                shapePath.cubicTo(center.x + halfW * 1.35f, center.y - halfH * 0.40f, center.x + halfW * 1.25f, center.y + halfH * 0.45f, center.x, center.y + halfH)
                shapePath.cubicTo(center.x - halfW * 1.25f, center.y + halfH * 0.45f, center.x - halfW * 1.35f, center.y - halfH * 0.40f, center.x, center.y - halfH * 0.95f)
                shapePath.close()
                detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                detailPath.quadraticTo(center.x + halfW * 0.20f, center.y, center.x, center.y + halfH * 0.85f)
            }
            ParticleType.SAKURA_PETAL -> {
                val topY = center.y - halfH * 0.95f
                val cleft = halfH * 0.08f
                shapePath.moveTo(center.x, center.y + halfH * 0.90f)
                shapePath.cubicTo(center.x - halfW * 0.95f, center.y + halfH * 0.25f, center.x - halfW * 1.05f, center.y - halfH * 0.55f, center.x - halfW * 0.35f, topY)
                shapePath.quadraticTo(center.x, topY + cleft, center.x + halfW * 0.35f, topY)
                shapePath.cubicTo(center.x + halfW * 1.05f, center.y - halfH * 0.55f, center.x + halfW * 0.95f, center.y + halfH * 0.25f, center.x, center.y + halfH * 0.90f)
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
        drawPath(path = detailPath, color = Color(0xFF041014).copy(alpha = (0.24f * color.alpha).coerceIn(0.06f, 0.35f)), style = Stroke(width = 1.0f))
    }
}

// =========================================================================
// GENERATION HELPERS
// =========================================================================

private fun generateFallingLeaves(count: Int, palette: List<Color>, pileUpAtBottom: Boolean, ambientAlpha: Float): List<Leaf> {
    return List(count) { i ->
        val x = Random.nextFloat() * 0.84f + 0.08f
        val initialY = if (pileUpAtBottom) -(i.toFloat() / count.toFloat()) * 0.85f - 0.06f
        else (i.toFloat() / count.toFloat()) * 1.1f - 0.1f
        val a = Random.nextFloat() * 0.30f + 0.55f
        Leaf(
            xBase = x, currentX = x, y = initialY,
            speedY = Random.nextFloat() * 0.009f + 0.008f,
            swayFreq = Random.nextFloat() * 0.45f + 0.55f,
            swayAmp = Random.nextFloat() * 0.022f + 0.018f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 12f,
            size = Random.nextFloat() * 10f + 18f,
            baseAlpha = a,
            restAlpha = a,
            color = palette.random()
        )
    }
}

/** Pre-seeds resting leaves scattered across the bottom for resume / summary screen. */
private fun generatePreRestingLeaves(count: Int, palette: List<Color>, ambientAlpha: Float): List<Leaf> {
    val rng = Random(42)
    return List(count) { i ->
        val x = rng.nextFloat() * 0.84f + 0.08f
        val y = rng.nextFloat() * 0.07f + 0.89f
        val baseAngle = if (i % 2 == 0) 90f else 270f
        val a = rng.nextFloat() * 0.32f + 0.52f
        Leaf(
            xBase = x, currentX = x, y = y,
            speedY = 0f, vx = 0f,
            swayFreq = rng.nextFloat() * 0.45f + 0.35f,
            swayAmp = 0f,
            swayPhase = rng.nextFloat() * 6.28f,
            rotation = baseAngle + (rng.nextFloat() - 0.5f) * 28f,
            rotSpeed = 0f,
            size = rng.nextFloat() * 9f + 18f,
            baseAlpha = a, restAlpha = a,
            color = palette[i % palette.size],
            isResting = true,
            restSwayPhase = rng.nextFloat() * 6.28f
        )
    }
}
