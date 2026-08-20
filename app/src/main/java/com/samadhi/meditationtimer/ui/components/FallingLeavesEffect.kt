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
import androidx.compose.ui.graphics.lerp
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private enum class LeafDepth(
    val scaleMultiplier: Float,
    val alphaMultiplier: Float,
    val speedMultiplier: Float,
    val canAccumulate: Boolean
) {
    BACKGROUND(scaleMultiplier = 0.65f, alphaMultiplier = 0.40f, speedMultiplier = 0.70f, canAccumulate = false),
    MIDGROUND(scaleMultiplier = 1.00f, alphaMultiplier = 1.00f, speedMultiplier = 1.00f, canAccumulate = true),
    FOREGROUND(scaleMultiplier = 1.30f, alphaMultiplier = 1.15f, speedMultiplier = 1.25f, canAccumulate = true)
}

private class LeafParticle(
    var x: Float,
    var y: Float,
    var originX: Float,
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
    val depth: LeafDepth = LeafDepth.MIDGROUND,
    var isSettling: Boolean = false,
    var settleTimer: Float = 0f,
    var targetSettleRotation: Float = 0f,
    var isResting: Boolean = false,
    var restSwayPhase: Float = 0f,
    var spawnElapsedSec: Float = 1.2f
)

private const val HEIGHTMAP_BINS = 24
private const val FLOOR_Y = 0.948f
private const val LIFT_PER_LEAF = 0.0065f

private class GroundHeightMap(
    private val timerRadiusRatio: Float,
    private val timerCenterYRatio: Float,
    private val timerCenterXRatio: Float
) {
    val bins = FloatArray(HEIGHTMAP_BINS) { FLOOR_Y }
    val binCounts = IntArray(HEIGHTMAP_BINS) { 0 }

    fun sampleFloor(normalizedX: Float): Float {
        val binIdx = ((normalizedX - 0.08f) / 0.84f * HEIGHTMAP_BINS)
            .toInt()
            .coerceIn(0, HEIGHTMAP_BINS - 1)
        return bins[binIdx]
    }

    fun depositLeaf(landingX: Float): Triple<Float, Float, Float> {
        var binIdx = ((landingX - 0.08f) / 0.84f * HEIGHTMAP_BINS)
            .toInt()
            .coerceIn(0, HEIGHTMAP_BINS - 1)

        val leftIdx = (binIdx - 1).coerceAtLeast(0)
        val rightIdx = (binIdx + 1).coerceAtMost(HEIGHTMAP_BINS - 1)

        if (binCounts[leftIdx] < binCounts[binIdx] && binCounts[leftIdx] <= binCounts[rightIdx]) {
            binIdx = leftIdx
        } else if (binCounts[rightIdx] < binCounts[binIdx]) {
            binIdx = rightIdx
        }

        binCounts[binIdx]++

        val binWidth = 0.84f / HEIGHTMAP_BINS
        val settledX = 0.08f + (binIdx + 0.5f) * binWidth + (Random.nextFloat() * (binWidth * 0.45f) - binWidth * 0.225f)
        val settledY = bins[binIdx]

        bins[binIdx] -= LIFT_PER_LEAF
        if (binIdx > 0) bins[binIdx - 1] -= LIFT_PER_LEAF * 0.35f
        if (binIdx < HEIGHTMAP_BINS - 1) bins[binIdx + 1] -= LIFT_PER_LEAF * 0.35f

        val topCeilingY = if (timerRadiusRatio > 0f && timerCenterYRatio >= 0f) {
            val dx = settledX - timerCenterXRatio
            val outerRadius = timerRadiusRatio + 0.04f
            if (kotlin.math.abs(dx) < outerRadius) {
                val arcY = timerCenterYRatio + sqrt((outerRadius * outerRadius - dx * dx).coerceAtLeast(0f))
                arcY.coerceIn(timerCenterYRatio + timerRadiusRatio, FLOOR_Y)
            } else 0.70f
        } else 0.70f

        bins[binIdx] = bins[binIdx].coerceAtLeast(topCeilingY)

        val leftH = if (binIdx > 0) bins[binIdx - 1] else FLOOR_Y
        val rightH = if (binIdx < HEIGHTMAP_BINS - 1) bins[binIdx + 1] else FLOOR_Y
        val slope = ((leftH - rightH) / (binWidth * 2f)) * 14f
        val slopeTilt = slope.coerceIn(-18f, 18f)

        return Triple(settledX, settledY.coerceIn(topCeilingY, FLOOR_Y), slopeTilt)
    }
}

private fun smoothProgress(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

@Composable
fun FallingLeavesEffect(
    modifier: Modifier = Modifier,
    leafCount: Int = 18,
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

    val groundMap = remember(currentTheme.id, pileUpAtBottom) {
        GroundHeightMap(timerRadiusRatio, timerCenterYRatio, timerCenterXRatio)
    }

    val restingLeaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<LeafParticle>().apply {
            if (pileUpAtBottom && initialSettledLeavesCount > 0) {
                val count = initialSettledLeavesCount.coerceAtMost(maxRestingLeaves)
                val rng = Random(1337)
                for (i in 0 until count) {
                    val dropX = rng.nextFloat() * 0.84f + 0.08f
                    val (x, y, slopeTilt) = groundMap.depositLeaf(dropX)
                    val baseRot = if (rng.nextBoolean()) 90f else 270f
                    add(
                        LeafParticle(
                            x = x,
                            y = y,
                            originX = x,
                            fallSpeed = 0f,
                            swayAmplitude = 0f,
                            swayFrequency = 0f,
                            swayPhase = 0f,
                            tumbleFrequency = 0f,
                            tumblePhase = 0f,
                            rotation = baseRot + slopeTilt + (rng.nextFloat() * 28f - 14f),
                            rotationSpeed = 0f,
                            size = rng.nextFloat() * 5f + 16f,
                            alpha = rng.nextFloat() * 0.15f + 0.70f,
                            color = currentTheme.particlePalette.random(rng),
                            isResting = true,
                            restSwayPhase = rng.nextFloat() * 6.28f,
                            spawnElapsedSec = 1.2f
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
                val isHero = i == 0
                val ox = rng.nextFloat() * 0.84f + 0.08f
                val startY = (i.toFloat() / leafCount.toFloat()) * 1.15f - 0.15f

                val depth = when {
                    isHero -> LeafDepth.FOREGROUND
                    i % 4 == 0 -> LeafDepth.BACKGROUND
                    i % 5 == 0 -> LeafDepth.FOREGROUND
                    else -> LeafDepth.MIDGROUND
                }

                add(
                    LeafParticle(
                        x = ox,
                        y = startY,
                        originX = ox,
                        fallSpeed = (rng.nextFloat() * 0.006f + 0.012f) * depth.speedMultiplier * (if (isHero) 0.7f else 1f),
                        swayAmplitude = (rng.nextFloat() * 0.05f + 0.06f) * depth.scaleMultiplier * (if (isHero) 1.15f else 1f),
                        swayFrequency = rng.nextFloat() * 0.12f + 0.10f,
                        swayPhase = rng.nextFloat() * 6.28f,
                        tumbleFrequency = rng.nextFloat() * 0.40f + 0.35f,
                        tumblePhase = rng.nextFloat() * 6.28f,
                        rotation = rng.nextFloat() * 360f,
                        rotationSpeed = (rng.nextFloat() - 0.5f) * 0.7f,
                        size = (rng.nextFloat() * 5f + 16f) * depth.scaleMultiplier * (if (isHero) 1.35f else 1f),
                        alpha = (rng.nextFloat() * 0.15f + 0.70f) * depth.alphaMultiplier * (if (isHero) 1.2f else 1f),
                        color = currentTheme.particlePalette.random(rng),
                        depth = depth,
                        isResting = false,
                        spawnElapsedSec = 1.2f
                    )
                )
            }
        }
    }

    LaunchedEffect(sessionProgressRatio, pileUpAtBottom) {
        if (!pileUpAtBottom) return@LaunchedEffect
        val target = (smoothProgress(sessionProgressRatio) * maxRestingLeaves).toInt()
        val missing = target - restingLeaves.size
        if (missing <= 0) return@LaunchedEffect

        // A big gap only happens on first load or resuming after being away —
        // catch up instantly there. Otherwise this fires ~once a second as
        // progress ticks, so cap it to a slow trickle rather than a burst.
        val toAdd = if (missing > 6) missing else 1

        val rng = Random(System.currentTimeMillis())
        repeat(toAdd) {
            val dropX = rng.nextFloat() * 0.84f + 0.08f
            val (x, y, slopeTilt) = groundMap.depositLeaf(dropX)
            val baseRot = if (rng.nextBoolean()) 90f else 270f
            restingLeaves.add(
                LeafParticle(
                    x = x,
                    y = y,
                    originX = x,
                    fallSpeed = 0f,
                    swayAmplitude = 0f,
                    swayFrequency = 0f,
                    swayPhase = 0f,
                    tumbleFrequency = 0f,
                    tumblePhase = 0f,
                    rotation = baseRot + slopeTilt + (rng.nextFloat() * 28f - 14f),
                    rotationSpeed = 0f,
                    size = rng.nextFloat() * 5f + 16f,
                    alpha = rng.nextFloat() * 0.15f + 0.70f,
                    color = currentTheme.particlePalette.random(rng),
                    isResting = true,
                    restSwayPhase = rng.nextFloat() * 6.28f,
                    spawnElapsedSec = 1.2f
                )
            )
        }
    }

    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            fallingLeaves.forEach { it.rotationSpeed = (Random.nextFloat() * 16f - 8f) }
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

                    val macroWind = sin(totalElapsedSec / 38.0f) * 0.012f + sin(totalElapsedSec / 19.0f) * 0.004f

                    val expectedRestingCount = (smoothProgress(sessionProgressRatio) * maxRestingLeaves).toInt()

                    fallingLeaves.forEach { leaf ->
                        leaf.spawnElapsedSec += dt

                        // Pendulum swing with subtle flutter
                        val swingPhase = totalElapsedSec * leaf.swayFrequency + leaf.swayPhase
                        val primarySway = sin(swingPhase) * leaf.swayAmplitude
                        val flutterSway = sin(swingPhase * 2.7f + leaf.tumblePhase) * leaf.swayAmplitude * 0.18f
                        val lateralSway = primarySway + flutterSway + macroWind
                        leaf.x = (leaf.originX + lateralSway).coerceIn(0.06f, 0.94f)

                        // Weathervane pendulum tilt aligned with swing direction
                        val swingTilt = cos(swingPhase) * 60f
                        leaf.rotation = swingTilt + leaf.rotationSpeed * totalElapsedSec

                        // Aerodynamic drag
                        val rad = Math.toRadians(leaf.rotation.toDouble())
                        val orientationTilt = kotlin.math.abs(cos(rad)).toFloat()
                        val aerodynamicDrag = (1.0f - 0.48f * orientationTilt).coerceIn(0.52f, 1.0f)

                        // Gust pause modulation: breaks metronomic constant fall
                        val gustPeriod = 18f + (leaf.swayPhase % 7f)
                        val gustMultiplier = 0.7f + 0.3f * (sin(totalElapsedSec * (6.283f / gustPeriod) + leaf.swayPhase) * 0.5f + 0.5f)

                        leaf.y += leaf.fallSpeed * gustMultiplier * aerodynamicDrag * dt

                        val floorContactY = groundMap.sampleFloor(leaf.x)

                        if (pileUpAtBottom && leaf.depth.canAccumulate && leaf.y >= floorContactY) {
                            if (restingLeaves.size < expectedRestingCount && restingLeaves.size < maxRestingLeaves) {
                                val (settledX, settledY, slopeTilt) = groundMap.depositLeaf(leaf.x)
                                val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                val baseTarget = if (normDeg in 0f..180f) 90f else 270f
                                val finalAngle = baseTarget + slopeTilt + (Random.nextFloat() * 24f - 12f)

                                restingLeaves.add(
                                    LeafParticle(
                                        x = settledX,
                                        y = settledY,
                                        originX = settledX,
                                        fallSpeed = 0f,
                                        swayAmplitude = 0f,
                                        swayFrequency = 0f,
                                        swayPhase = 0f,
                                        tumbleFrequency = 0f,
                                        tumblePhase = 0f,
                                        rotation = leaf.rotation,
                                        rotationSpeed = 0f,
                                        size = leaf.size,
                                        alpha = leaf.alpha,
                                        color = leaf.color,
                                        depth = LeafDepth.MIDGROUND,
                                        isSettling = true,
                                        settleTimer = 0f,
                                        targetSettleRotation = finalAngle,
                                        isResting = false,
                                        restSwayPhase = Random.nextFloat() * 6.28f,
                                        spawnElapsedSec = 1.2f
                                    )
                                )
                            }
                            leaf.y = -(Random.nextFloat() * 0.18f + 0.08f)
                            leaf.originX = Random.nextFloat() * 0.84f + 0.08f
                            leaf.x = leaf.originX
                            leaf.rotation = Random.nextFloat() * 360f
                            leaf.spawnElapsedSec = 0f
                        } else if (leaf.y > 1.08f) {
                            leaf.y = -(Random.nextFloat() * 0.18f + 0.08f)
                            leaf.originX = Random.nextFloat() * 0.84f + 0.08f
                            leaf.x = leaf.originX
                            leaf.spawnElapsedSec = 0f
                        }
                    }

                    restingLeaves.forEach { leaf ->
                        if (leaf.isSettling) {
                            leaf.settleTimer += dt
                            val t = (leaf.settleTimer / 0.85f).coerceIn(0f, 1f)
                            val decay = exp(-4.2f * t)
                            val flutter = sin(14f * t) * (8f * (1f - t))
                            val lerped = leaf.rotation + (leaf.targetSettleRotation - leaf.rotation) * t
                            leaf.rotation = lerped + flutter * decay

                            if (t >= 1.0f) {
                                leaf.isSettling = false
                                leaf.isResting = true
                                leaf.rotation = leaf.targetSettleRotation
                            }
                        } else if (leaf.isResting) {
                            leaf.rotation += dt * 0.22f * sin(totalElapsedSec * 0.45f + leaf.restSwayPhase)
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

        val baseDimension = min(w, h)
        val particleScale = (baseDimension / 390f).coerceIn(0.80f, 1.30f)
        val breathingWave = (sin(t * 1.3f) * 0.5f + 0.5f)

        // Draw order: Background -> Resting pile -> Foreground -> Hero leaf on top
        val hero = fallingLeaves.firstOrNull()
        val otherFalling = if (fallingLeaves.isNotEmpty()) fallingLeaves.drop(1) else emptyList()
        val bgAirborne = otherFalling.filter { it.depth == LeafDepth.BACKGROUND }
        val fgAirborne = otherFalling.filter { it.depth != LeafDepth.BACKGROUND }

        val drawList = buildList {
            addAll(bgAirborne)
            addAll(restingLeaves)
            addAll(fgAirborne)
            hero?.let { add(it) }
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

            // Staggered spawn fade + top margin ether ingress
            val spawnFade = (leaf.spawnElapsedSec / 1.2f).coerceIn(0f, 1f)
            val topFade = if (!leaf.isResting && !leaf.isSettling) {
                ((leaf.y + 0.08f) / 0.14f).coerceIn(0f, 1f) * spawnFade
            } else 1.0f

            val baseTargetAlpha = leaf.alpha * ambientAlpha * topFade
            val alpha = when {
                blowAwayProgress > 0f -> baseTargetAlpha * (1f - blowAwayProgress)
                else -> baseTargetAlpha.coerceIn(0f, 0.85f)
            }

            if (alpha < 0.01f) return@forEach

            // Desaturate / blend background depth layer toward dark ambient
            val drawColor = if (leaf.depth == LeafDepth.BACKGROUND) {
                lerp(leaf.color, Color(0xFF090D10), 0.35f)
            } else leaf.color

            val tumbleScaleX = if (!leaf.isResting && !leaf.isSettling) {
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
                color = drawColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                glowColor = currentTheme.primaryLight,
                particleType = currentTheme.particleType,
                isGlowing = isGlowing && !leaf.isResting && !leaf.isSettling,
                breathingGlowFactor = breathingWave,
                isResting = leaf.isResting || leaf.isSettling
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
                drawPath(path = shapePath, color = Color(0xFF070B0E))
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
                color = Color.Black.copy(alpha = (0.20f * color.alpha).coerceIn(0.04f, 0.28f)),
                style = Stroke(width = 0.8f)
            )
        }
    }
}