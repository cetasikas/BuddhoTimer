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
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private class LeafParticle(
    var x: Float,
    var y: Float,
    var originX: Float,
    var startY: Float,
    var fallSpeed: Float,
    var swayAmplitude: Float,
    var swayFrequency: Float,
    var swayPhase: Float,
    var tumbleFrequency: Float,
    var tumblePhase: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val size: Float,
    val alpha: Float,
    val color: Color,
    var isResting: Boolean = false,
    var restSwayPhase: Float = 0f
)

private const val HEIGHTMAP_BINS = 28
private const val FLOOR_Y = 0.948f
private const val LIFT_PER_LEAF = 0.0032f

private class GroundHeightMap(
    private val timerRadiusRatio: Float,
    private val timerCenterYRatio: Float,
    private val timerCenterXRatio: Float
) {
    val bins = FloatArray(HEIGHTMAP_BINS) { FLOOR_Y }

    fun sampleFloor(normalizedX: Float): Float {
        val binIdx = ((normalizedX - 0.06f) / 0.88f * HEIGHTMAP_BINS)
            .toInt()
            .coerceIn(0, HEIGHTMAP_BINS - 1)
        return bins[binIdx]
    }

    fun depositLeaf(landingX: Float): Triple<Float, Float, Float> {
        var binIdx = ((landingX - 0.06f) / 0.88f * HEIGHTMAP_BINS)
            .toInt()
            .coerceIn(0, HEIGHTMAP_BINS - 1)

        // Angle of repose: roll into lower adjacent trough if current bin is a peak
        val currentH = bins[binIdx]
        val leftH = if (binIdx > 0) bins[binIdx - 1] else FLOOR_Y
        val rightH = if (binIdx < HEIGHTMAP_BINS - 1) bins[binIdx + 1] else FLOOR_Y

        if (leftH > currentH + 0.0018f && leftH >= rightH && binIdx > 0) {
            binIdx--
        } else if (rightH > currentH + 0.0018f && binIdx < HEIGHTMAP_BINS - 1) {
            binIdx++
        }

        val settledX = 0.06f + (binIdx + 0.5f) * (0.88f / HEIGHTMAP_BINS) + (Random.nextFloat() * 0.016f - 0.008f)
        val settledY = bins[binIdx]

        // Smooth elevation Gaussian stamp across adjacent bins
        bins[binIdx] -= LIFT_PER_LEAF
        if (binIdx > 0) bins[binIdx - 1] -= LIFT_PER_LEAF * 0.45f
        if (binIdx < HEIGHTMAP_BINS - 1) bins[binIdx + 1] -= LIFT_PER_LEAF * 0.45f

        // Respect timer bounding ceiling
        val topCeilingY = if (timerRadiusRatio > 0f && timerCenterYRatio >= 0f) {
            val dx = settledX - timerCenterXRatio
            val outerRadius = timerRadiusRatio + 0.04f
            if (kotlin.math.abs(dx) < outerRadius) {
                val arcY = timerCenterYRatio + sqrt((outerRadius * outerRadius - dx * dx).coerceAtLeast(0f))
                arcY.coerceIn(timerCenterYRatio + timerRadiusRatio, FLOOR_Y)
            } else 0.70f
        } else 0.70f

        bins[binIdx] = bins[binIdx].coerceAtLeast(topCeilingY)

        val slope = ((leftH - rightH) / (0.88f / HEIGHTMAP_BINS * 2f)) * 18f
        val slopeTilt = slope.coerceIn(-20f, 20f)

        return Triple(settledX, settledY.coerceIn(topCeilingY, FLOOR_Y), slopeTilt)
    }
}

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

    val groundMap = remember(currentTheme.id, pileUpAtBottom) {
        GroundHeightMap(timerRadiusRatio, timerCenterYRatio, timerCenterXRatio)
    }

    val restingLeaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<LeafParticle>().apply {
            if (pileUpAtBottom) {
                val targetCount = maxOf(elapsedMeditationSeconds / secondsPerLeaf, initialSettledLeavesCount)
                    .coerceAtMost(maxRestingLeaves)
                val rng = Random(1337)
                for (i in 0 until targetCount) {
                    val dropX = rng.nextFloat() * 0.84f + 0.08f
                    val (x, y, slopeTilt) = groundMap.depositLeaf(dropX)
                    val baseRot = if (rng.nextBoolean()) 90f else 270f
                    add(
                        LeafParticle(
                            x = x,
                            y = y,
                            originX = x,
                            startY = y,
                            fallSpeed = 0f,
                            swayAmplitude = 0f,
                            swayFrequency = 0f,
                            swayPhase = 0f,
                            tumbleFrequency = 0f,
                            tumblePhase = 0f,
                            rotation = baseRot + slopeTilt + (rng.nextFloat() * 36f - 18f),
                            rotationSpeed = 0f,
                            size = rng.nextFloat() * 6f + 16f,
                            alpha = rng.nextFloat() * 0.20f + 0.65f,
                            color = currentTheme.particlePalette.random(rng),
                            isResting = true,
                            restSwayPhase = rng.nextFloat() * 6.28f
                        )
                    )
                }
            }
        }
    }

    val fallingLeaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<LeafParticle>().apply {
            val rng = Random(42)
            for (i in 0 until leafCount) {
                val ox = rng.nextFloat() * 0.84f + 0.08f
                val startY = if (pileUpAtBottom) {
                    -(i.toFloat() / leafCount.toFloat()) * 1.8f - 0.08f
                } else {
                    (i.toFloat() / leafCount.toFloat()) * 1.1f - 0.1f
                }
                add(
                    LeafParticle(
                        x = ox,
                        y = startY,
                        originX = ox,
                        startY = startY,
                        fallSpeed = rng.nextFloat() * 0.008f + 0.011f,
                        swayAmplitude = rng.nextFloat() * 0.025f + 0.030f,
                        swayFrequency = rng.nextFloat() * 0.25f + 0.35f,
                        swayPhase = rng.nextFloat() * 6.28f,
                        tumbleFrequency = rng.nextFloat() * 0.45f + 0.40f,
                        tumblePhase = rng.nextFloat() * 6.28f,
                        rotation = rng.nextFloat() * 360f,
                        rotationSpeed = (rng.nextFloat() - 0.5f) * 4.0f,
                        size = rng.nextFloat() * 6f + 16f,
                        alpha = rng.nextFloat() * 0.20f + 0.65f,
                        color = currentTheme.particlePalette.random(rng),
                        isResting = false
                    )
                )
            }
        }
    }

    // Synchronize pile if app resumes from background after a long gap (>12s)
    LaunchedEffect(elapsedMeditationSeconds) {
        if (pileUpAtBottom && elapsedMeditationSeconds > 0) {
            val targetCount = (elapsedMeditationSeconds / secondsPerLeaf).coerceAtMost(maxRestingLeaves)
            val missing = targetCount - restingLeaves.size
            if (missing > 0) {
                val rng = Random(System.currentTimeMillis())
                for (i in 0 until missing) {
                    val dropX = rng.nextFloat() * 0.84f + 0.08f
                    val (x, y, slopeTilt) = groundMap.depositLeaf(dropX)
                    val baseRot = if (rng.nextBoolean()) 90f else 270f
                    restingLeaves.add(
                        LeafParticle(
                            x = x,
                            y = y,
                            originX = x,
                            startY = y,
                            fallSpeed = 0f,
                            swayAmplitude = 0f,
                            swayFrequency = 0f,
                            swayPhase = 0f,
                            tumbleFrequency = 0f,
                            tumblePhase = 0f,
                            rotation = baseRot + slopeTilt + (rng.nextFloat() * 36f - 18f),
                            rotationSpeed = 0f,
                            size = rng.nextFloat() * 6f + 16f,
                            alpha = rng.nextFloat() * 0.20f + 0.65f,
                            color = currentTheme.particlePalette.random(rng),
                            isResting = true,
                            restSwayPhase = rng.nextFloat() * 6.28f
                        )
                    )
                }
            }
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

                    // Animate airborne leaves with continuous parametric flight
                    fallingLeaves.forEach { leaf ->
                        leaf.y += leaf.fallSpeed * dt
                        val lateralSway = sin(totalElapsedSec * leaf.swayFrequency + leaf.swayPhase) * leaf.swayAmplitude
                        leaf.x = (leaf.originX + lateralSway).coerceIn(0.06f, 0.94f)
                        leaf.rotation += leaf.rotationSpeed * dt

                        val floorContactY = groundMap.sampleFloor(leaf.x)

                        if (pileUpAtBottom && leaf.y >= floorContactY) {
                            if (restingLeaves.size < maxRestingLeaves) {
                                val (settledX, settledY, slopeTilt) = groundMap.depositLeaf(leaf.x)
                                val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                val baseTarget = if (normDeg in 0f..180f) 90f else 270f
                                val restingRot = baseTarget + slopeTilt + (Random.nextFloat() * 32f - 16f)

                                restingLeaves.add(
                                    LeafParticle(
                                        x = settledX,
                                        y = settledY,
                                        originX = settledX,
                                        startY = settledY,
                                        fallSpeed = 0f,
                                        swayAmplitude = 0f,
                                        swayFrequency = 0f,
                                        swayPhase = 0f,
                                        tumbleFrequency = 0f,
                                        tumblePhase = 0f,
                                        rotation = restingRot,
                                        rotationSpeed = 0f,
                                        size = leaf.size,
                                        alpha = leaf.alpha,
                                        color = leaf.color,
                                        isResting = true,
                                        restSwayPhase = Random.nextFloat() * 6.28f
                                    )
                                )
                            }
                            // Respawn airborne leaf high above viewport
                            leaf.y = -(Random.nextFloat() * 0.22f + 0.08f)
                            leaf.originX = Random.nextFloat() * 0.84f + 0.08f
                            leaf.x = leaf.originX
                            leaf.rotation = Random.nextFloat() * 360f
                        } else if (!pileUpAtBottom && leaf.y > 1.08f) {
                            leaf.y = -(Random.nextFloat() * 0.16f + 0.05f)
                            leaf.originX = Random.nextFloat() * 0.84f + 0.08f
                            leaf.x = leaf.originX
                        }
                    }

                    // Gentle breathing oscillation for grounded leaves
                    restingLeaves.forEach { leaf ->
                        leaf.rotation += dt * 0.30f * sin(totalElapsedSec * 0.5f + leaf.restSwayPhase)
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

        val baseDimension = min(w, h)
        val particleScale = (baseDimension / 390f).coerceIn(0.80f, 1.30f)
        val breathingWave = (sin(t * 1.3f) * 0.5f + 0.5f)

        // Draw ground pile first (base layers to top layers), then airborne leaves
        val drawList = buildList {
            addAll(restingLeaves)
            addAll(fallingLeaves)
        }

        drawList.forEach { leaf ->
            val px = leaf.x * w
            val py = leaf.y * h

            val blowOffset = if (blowAwayProgress > 0f) {
                val p = blowAwayProgress
                val dir = if (leaf.isResting) (leaf.x - 0.5f).let { if (it == 0f) 1f else kotlin.math.sign(it) } else 1f
                Offset(w * dir * p * 1.4f, -h * p * p * 0.65f)
            } else Offset.Zero

            val blowRot = if (blowAwayProgress > 0f) blowAwayProgress * 340f else 0f

            val alpha = when {
                blowAwayProgress > 0f -> (leaf.alpha * ambientAlpha) * (1f - blowAwayProgress)
                else -> (leaf.alpha * ambientAlpha).coerceIn(0.12f, 0.85f)
            }

            if (alpha < 0.01f) return@forEach

            val tumbleScaleX = if (!leaf.isResting) {
                val wave = cos(t * leaf.tumbleFrequency + leaf.tumblePhase)
                (wave * 0.45f + 0.55f).coerceIn(0.20f, 1.0f)
            } else 1.0f

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
                breathingGlowFactor = breathingWave
            )
        }
    }
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
    breathingGlowFactor: Float
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