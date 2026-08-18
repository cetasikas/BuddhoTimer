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
    // Settle fade-in state
    var isSettling: Boolean = false,
    var settleProgress: Float = 0f,
    var settleDuration: Float = 1.0f,
    var settleStartRot: Float = 0f,
    var settleTargetRot: Float = 0f,
    var settleX: Float = 0f,
    var settleY: Float = 0f
)

private data class SettledLeaf(
    val x: Float,
    val y: Float,
    var rotation: Float,
    val size: Float,
    var alpha: Float,          // grows from 0 to final over fadeInProgress
    val finalAlpha: Float,
    val color: Color,
    val swayPhase: Float,
    var fadeInProgress: Float = 0f
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

    val activeLeaves = remember(currentTheme.id, leafCount) {
        mutableStateListOf<ActiveLeaf>().apply {
            addAll(generateInitialActiveLeaves(leafCount, currentTheme.particlePalette, pileUpAtBottom))
        }
    }

    // Scattered settled leaves — land wherever they fall and fade softly into place
    val settledLeaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<SettledLeaf>().apply {
            if (pileUpAtBottom && initialSettledLeavesCount > 0) {
                addAll(generatePreScatteredLeaves(initialSettledLeavesCount, currentTheme.particlePalette))
            }
        }
    }

    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            activeLeaves.forEach { leaf -> leaf.rotSpeed = (Random.nextFloat() * 16f - 8f) }
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
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    totalElapsedSec += dt

                    // Fade-in newly settled leaves
                    settledLeaves.forEachIndexed { idx, settled ->
                        if (settled.fadeInProgress < 1f) {
                            settled.fadeInProgress = (settled.fadeInProgress + dt / 1.8f).coerceIn(0f, 1f)
                            settled.alpha = settled.finalAlpha * settled.fadeInProgress
                        }
                        // Gentle ambient sway while resting
                        settled.rotation += dt * 0.4f * sin(totalElapsedSec * 0.5f + settled.swayPhase)
                    }

                    val iterator = activeLeaves.listIterator()
                    while (iterator.hasNext()) {
                        val leaf = iterator.next()

                        if (leaf.isSettling) {
                            leaf.settleProgress += dt / leaf.settleDuration
                            val t = (leaf.settleProgress).coerceIn(0f, 1f)
                            val ease = t * (2f - t)
                            leaf.rotation = leaf.settleStartRot + (leaf.settleTargetRot - leaf.settleStartRot) * ease

                            if (leaf.settleProgress >= 1f) {
                                // Commit as a permanent scattered leaf on the forest floor
                                if (settledLeaves.size < 90) {
                                    val finalAlpha = (leaf.alpha * 0.85f).coerceIn(0.35f, 0.82f)
                                    settledLeaves.add(
                                        SettledLeaf(
                                            x = leaf.settleX,
                                            y = leaf.settleY,
                                            rotation = leaf.settleTargetRot,
                                            size = leaf.size,
                                            alpha = 0f,
                                            finalAlpha = finalAlpha,
                                            color = leaf.color,
                                            swayPhase = Random.nextFloat() * 6.28f,
                                            fadeInProgress = 0f
                                        )
                                    )
                                }

                                // Recycle: respawn this leaf high above the top
                                leaf.isSettling = false
                                leaf.settleProgress = 0f
                                leaf.vx = 0f
                                leaf.y = -(Random.nextFloat() * 0.22f + 0.06f)
                                leaf.xBase = Random.nextFloat() * 0.88f + 0.06f
                                leaf.currentX = leaf.xBase
                                leaf.rotation = Random.nextFloat() * 360f
                                leaf.rotSpeed = (Random.nextFloat() - 0.5f) * 14f
                            }
                            continue
                        }

                        // Natural descent
                        leaf.y += leaf.speedY * dt
                        leaf.rotation += leaf.rotSpeed * dt
                        leaf.vx *= (1f - dt * 1.2f).coerceAtLeast(0f)
                        leaf.rotSpeed *= (1f - dt * 0.8f)

                        val sway = sin(totalElapsedSec * leaf.swayFreq + leaf.swayPhase) * leaf.swayAmp
                        leaf.currentX = (leaf.xBase + sway + leaf.vx * dt).coerceIn(0.02f, 0.98f)

                        if (!pileUpAtBottom && leaf.y > 1.08f && !leaf.isBurst) {
                            leaf.y = -(Random.nextFloat() * 0.12f + 0.04f)
                            leaf.xBase = Random.nextFloat()
                            leaf.currentX = leaf.xBase
                        }

                        if (leaf.isBurst) {
                            leaf.life -= dt * 0.35f
                            if (leaf.life <= 0f || leaf.y > 1.1f) {
                                iterator.remove()
                                continue
                            }
                        } else if (pileUpAtBottom && leaf.y >= 0.92f) {
                            // Land wherever the leaf currently is — simple and organic
                            leaf.isSettling = true
                            leaf.settleProgress = 0f
                            leaf.settleDuration = Random.nextFloat() * 0.5f + 0.8f
                            leaf.settleStartRot = leaf.rotation
                            leaf.settleX = leaf.currentX
                            leaf.settleY = (leaf.y + (Random.nextFloat() * 0.04f)).coerceIn(0.88f, 0.96f)

                            // Rock gently to a horizontal resting angle
                            val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                            val distTo90 = kotlin.math.abs(normDeg - 90f).let { if (it > 180f) 360f - it else it }
                            val distTo270 = kotlin.math.abs(normDeg - 270f).let { if (it > 180f) 360f - it else it }
                            val targetNorm = if (distTo90 <= distTo270) 90f else 270f
                            var delta = targetNorm - normDeg
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            leaf.settleTargetRot = leaf.rotation + delta + (Random.nextFloat() * 28f - 14f)

                        } else if (!pileUpAtBottom && leaf.y >= 1.06f) {
                            leaf.y = -(Random.nextFloat() * 0.10f + 0.04f)
                            leaf.xBase = Random.nextFloat()
                            leaf.currentX = leaf.xBase
                            leaf.rotation = Random.nextFloat() * 360f
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

        // 1. Scattered settled forest floor leaves
        if (pileUpAtBottom) {
            settledLeaves.forEach { settled ->
                val posX = settled.x * width
                val posY = settled.y * height

                val blowOffset = if (blowAwayProgress > 0f) {
                    val p = blowAwayProgress
                    val dir = (settled.x - 0.5f).let { if (it == 0f) 1f else kotlin.math.sign(it) }
                    Offset(width * dir * p * 1.4f, -height * p * p * 0.6f)
                } else Offset.Zero

                val finalAlpha = if (blowAwayProgress > 0f) {
                    settled.alpha * (1f - blowAwayProgress).coerceIn(0f, 1f)
                } else {
                    settled.alpha
                }

                val finalRot = if (blowAwayProgress > 0f) {
                    settled.rotation + blowAwayProgress * 320f
                } else {
                    settled.rotation
                }

                if (finalAlpha > 0.01f) {
                    drawIndividualParticle(
                        shapePath = shapePath,
                        detailPath = detailPath,
                        center = Offset(posX + blowOffset.x, posY + blowOffset.y),
                        size = settled.size * (width / 360f).coerceIn(0.85f, 1.25f),
                        rotation = finalRot,
                        color = settled.color.copy(alpha = finalAlpha),
                        glowColor = currentTheme.primaryLight,
                        particleType = currentTheme.particleType,
                        isGlowing = false,
                        breathingGlowFactor = 0f,
                        isResting = true
                    )
                }
            }
        }

        // 2. Active falling leaves
        activeLeaves.forEach { leaf ->
            val posX = leaf.currentX * width
            val posY = leaf.y * height

            val blowOffset = if (blowAwayProgress > 0f) {
                val p = blowAwayProgress
                Offset(width * p * 0.9f, -height * p * p * 0.5f)
            } else Offset.Zero

            val finalAlpha = if (blowAwayProgress > 0f) {
                leaf.alpha * (1f - blowAwayProgress).coerceIn(0f, 1f)
            } else {
                (leaf.alpha * ambientAlpha).coerceIn(0.10f, 0.90f)
            }

            drawIndividualParticle(
                shapePath = shapePath,
                detailPath = detailPath,
                center = Offset(posX + blowOffset.x, posY + blowOffset.y),
                size = leaf.size * (width / 360f).coerceIn(0.85f, 1.25f),
                rotation = if (blowAwayProgress > 0f) leaf.rotation + blowAwayProgress * 360f else leaf.rotation,
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
            drawPath(path = shapePath, color = glowColor.copy(alpha = haloAlpha), style = Stroke(width = if (isResting) 2.6f else 3.5f))
        }

        drawPath(path = shapePath, color = color)
        drawPath(path = detailPath, color = Color(0xFF041014).copy(alpha = (0.24f * color.alpha).coerceIn(0.06f, 0.35f)), style = Stroke(width = 1.0f))
    }
}

private fun generateInitialActiveLeaves(count: Int, palette: List<Color>, pileUpAtBottom: Boolean): List<ActiveLeaf> {
    return List(count) { i ->
        val x = Random.nextFloat() * 0.84f + 0.08f
        val initialY = if (pileUpAtBottom) -(i.toFloat() / count.toFloat()) * 0.85f - 0.06f
        else (i.toFloat() / count.toFloat()) * 1.1f - 0.1f
        ActiveLeaf(
            xBase = x, currentX = x, y = initialY,
            speedY = Random.nextFloat() * 0.009f + 0.008f,
            swayFreq = Random.nextFloat() * 0.45f + 0.55f,
            swayAmp = Random.nextFloat() * 0.022f + 0.018f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 12f,
            size = Random.nextFloat() * 10f + 18f,
            alpha = Random.nextFloat() * 0.30f + 0.55f,
            color = palette.random()
        )
    }
}

private fun generatePreScatteredLeaves(count: Int, palette: List<Color>): List<SettledLeaf> {
    val rng = Random(42)
    return List(count) { i ->
        val x = rng.nextFloat() * 0.84f + 0.08f
        val y = rng.nextFloat() * 0.08f + 0.88f
        val baseAngle = if (i % 2 == 0) 90f else 270f
        val finalAlpha = rng.nextFloat() * 0.32f + 0.42f
        SettledLeaf(
            x = x, y = y,
            rotation = baseAngle + (rng.nextFloat() - 0.5f) * 28f,
            size = rng.nextFloat() * 9f + 18f,
            alpha = finalAlpha,
            finalAlpha = finalAlpha,
            color = palette[i % palette.size],
            swayPhase = rng.nextFloat() * 6.28f,
            fadeInProgress = 1f
        )
    }
}
