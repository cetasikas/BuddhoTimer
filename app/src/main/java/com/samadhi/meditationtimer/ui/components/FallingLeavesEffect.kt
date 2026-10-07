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
import androidx.compose.runtime.rememberUpdatedState
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
import kotlin.math.abs
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
    val swayMultiplier: Float,
    val canAccumulate: Boolean
) {
    BACKGROUND(scaleMultiplier = 0.78f, alphaMultiplier = 0.40f, speedMultiplier = 0.70f, swayMultiplier = 0.70f, canAccumulate = false),
    MIDGROUND(scaleMultiplier = 1.00f, alphaMultiplier = 1.00f, speedMultiplier = 1.00f, swayMultiplier = 0.85f, canAccumulate = true),
    FOREGROUND(scaleMultiplier = 1.18f, alphaMultiplier = 1.15f, speedMultiplier = 1.25f, swayMultiplier = 0.60f, canAccumulate = true)
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
    var settleStartX: Float = 0f,
    var settleStartY: Float = 0f,
    var targetSettleX: Float = 0f,
    var targetSettleY: Float = 0f,
    var shapeAsymmetry: Float = 0f,
    var shapeStretch: Float = 1f,
    var isVanishing: Boolean = false,
    var vanishTimer: Float = 0f,
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

/** Spawns airborne leaves along the left/right edges so the screen center stays calm. */
private fun randomEdgeSpawnX(rng: Random): Float {
    return if (rng.nextBoolean()) 0.06f + rng.nextFloat() * 0.20f
    else 0.74f + rng.nextFloat() * 0.20f
}

/**
 * A rare, coordinated "calm wave": a ~44s window every ~4 minutes during which the
 * whole canopy slows, softens its sway, and glows a touch brighter.
 */
private fun calmEventBlend(t: Float): Float {
    val cycle = 250f
    val local = t % cycle
    if (local > 44f) return 0f
    return smoothProgress((local / 8f).coerceIn(0f, 1f)) * (1f - smoothProgress(((local - 32f) / 12f).coerceIn(0f, 1f)))
}

private fun weightedLeafColor(palette: List<Color>, rng: Random): Color {
    if (palette.isEmpty()) return Color.Gray
    return if (rng.nextFloat() < 0.65f) palette[0]
    else palette[(1 + rng.nextInt((palette.size - 1).coerceAtLeast(1))).coerceAtMost(palette.size - 1)]
}

@Composable
fun FallingLeavesEffect(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
    if (!enabled) return

    val currentTheme = LocalZenTheme.current
    val maxRestingLeaves = 160
    val currentElapsedMeditationSeconds by rememberUpdatedState(elapsedMeditationSeconds)
    val currentProgressRatio by rememberUpdatedState(sessionProgressRatio)

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
                            size = rng.nextFloat().pow(1.3f) * 5f + 13f,
                            alpha = rng.nextFloat() * 0.15f + 0.70f,
                            color = weightedLeafColor(currentTheme.particlePalette, rng),
                            shapeAsymmetry = (rng.nextFloat() - 0.5f) * 2f,
                            shapeStretch = 0.85f + rng.nextFloat() * 0.30f,
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
                val ox = randomEdgeSpawnX(rng)
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
                        // Calming, weightless descent with wide floating glide
                        fallSpeed = (rng.nextFloat() * 0.003f + 0.0055f) * depth.speedMultiplier * (if (isHero) 0.80f else 1f),
                        swayAmplitude = (rng.nextFloat() * 0.05f + 0.075f) * depth.swayMultiplier * (if (isHero) 1.25f else 1f),
                        swayFrequency = rng.nextFloat() * 0.035f + 0.045f, // ~14-18s sweeping glide period
                        swayPhase = rng.nextFloat() * 6.28f,
                        tumbleFrequency = rng.nextFloat() * 0.20f + 0.15f,
                        tumblePhase = rng.nextFloat() * 6.28f,
                        rotation = 0f,
                        rotationSpeed = 0f,
                        size = (rng.nextFloat().pow(1.3f) * 5f + 13f) * depth.scaleMultiplier * (if (isHero) 1.15f else 1f),
                        alpha = (rng.nextFloat() * 0.15f + 0.70f) * depth.alphaMultiplier * (if (isHero) 1.2f else 1f),
                        color = weightedLeafColor(currentTheme.particlePalette, rng),
                        depth = depth,
                        shapeAsymmetry = (rng.nextFloat() - 0.5f) * 1.5f,
                        shapeStretch = 0.90f + rng.nextFloat() * 0.20f,
                        isResting = false,
                        spawnElapsedSec = 1.2f
                    )
                )
            }
        }
    }

    // Leaves only settle on the ground through natural physical contact in the frame loop below

    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            // Accelerate descent smoothly without teleporting coordinates or rotation
            fallingLeaves.forEach { leaf ->
                leaf.fallSpeed = (leaf.fallSpeed * 1.25f).coerceAtMost(0.018f)
            }
        }
    }

    var blowAwayProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isBlowingAway) {
        if (isBlowingAway) {
            val startTime = System.currentTimeMillis()
            val durationMs = 1000L
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

                    val calm = calmEventBlend(totalElapsedSec)
                    val swayRelax = 1f - 0.22f * calm
                    val fallRelax = 1f - 0.35f * calm

                    val macroWind = sin(totalElapsedSec / 38.0f) * 0.012f + sin(totalElapsedSec / 19.0f) * 0.004f

                    val expectedRestingCount = (currentElapsedMeditationSeconds / 12).coerceIn(0, maxRestingLeaves)

                    fallingLeaves.forEach { leaf ->
                        leaf.spawnElapsedSec += dt

                        // Meditative pendulum swing with gentle aerodynamic drift
                        val swingPhase = totalElapsedSec * leaf.swayFrequency + leaf.swayPhase
                        val primarySway = sin(swingPhase) * leaf.swayAmplitude * swayRelax
                        val flutterSway = sin(swingPhase * 2.1f + leaf.tumblePhase) * leaf.swayAmplitude * 0.14f * swayRelax
                        val lateralSway = primarySway + flutterSway + macroWind
                        leaf.x = (leaf.originX + lateralSway).coerceIn(0.06f, 0.94f)

                        // Natural aerodynamic banking angle (Kung Fu Panda ascending peach petal breeze):
                        // When gliding right (cos > 0), the leaf banks naturally right; when gliding left (cos < 0), it banks left.
                        val naturalBank = cos(swingPhase) * 65f
                        val organicFlutter = sin(swingPhase * 2.2f + leaf.tumblePhase) * 6f
                        leaf.rotation = naturalBank + organicFlutter

                        // Aerodynamic lift & cushion: broad horizontal glides slow descent naturally
                        val rad = Math.toRadians(naturalBank.toDouble())
                        val glideLift = kotlin.math.abs(sin(rad)).toFloat()
                        val aerodynamicDrag = (1.0f - 0.45f * glideLift).coerceIn(0.55f, 1.0f)

                        // Gust pause modulation: gentle meditative breath rhythm
                        val gustPeriod = 22f + (leaf.swayPhase % 8f)
                        val gustMultiplier = 0.80f + 0.20f * (sin(totalElapsedSec * (6.283f / gustPeriod) + leaf.swayPhase) * 0.5f + 0.5f)

                        leaf.y += leaf.fallSpeed * gustMultiplier * aerodynamicDrag * fallRelax * dt

                        val floorContactY = groundMap.sampleFloor(leaf.x)

                        if (pileUpAtBottom && leaf.depth.canAccumulate && leaf.y >= floorContactY) {
                            val behindSchedule = restingLeaves.size < expectedRestingCount
                            val settleChance = if (behindSchedule) 0.95f else 0.30f

                            if (restingLeaves.size < maxRestingLeaves && (behindSchedule || Random.nextFloat() < settleChance)) {
                                val (settledX, settledY, slopeTilt) = groundMap.depositLeaf(leaf.x)
                                val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                val baseTarget = if (normDeg in 0f..180f) 90f else 270f
                                val finalAngle = baseTarget + slopeTilt + (Random.nextFloat() * 24f - 12f)

                                restingLeaves.add(
                                    LeafParticle(
                                        x = leaf.x,
                                        y = floorContactY,
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
                                        settleStartX = leaf.x,
                                        settleStartY = floorContactY,
                                        targetSettleX = settledX,
                                        targetSettleY = settledY,
                                        shapeAsymmetry = leaf.shapeAsymmetry,
                                        shapeStretch = leaf.shapeStretch,
                                        isVanishing = false,
                                        vanishTimer = 0f,
                                        isResting = false,
                                        restSwayPhase = Random.nextFloat() * 6.28f,
                                        spawnElapsedSec = 1.2f
                                    )
                                )
                            }

                            // Cycle falling particle back to top
                            leaf.y = -(Random.nextFloat() * 0.18f + 0.08f)
                            leaf.originX = randomEdgeSpawnX(Random)
                            leaf.x = leaf.originX
                            leaf.rotation = Random.nextFloat() * 360f
                            leaf.spawnElapsedSec = 0f
                        } else if (leaf.y > 1.08f) {
                            leaf.y = -(Random.nextFloat() * 0.18f + 0.08f)
                            leaf.originX = randomEdgeSpawnX(Random)
                            leaf.x = leaf.originX
                            leaf.rotation = Random.nextFloat() * 360f
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

                            // Roll from the exact touchdown spot to the assigned bin slot
                            leaf.x = leaf.settleStartX + (leaf.targetSettleX - leaf.settleStartX) * t
                            leaf.y = leaf.settleStartY + (leaf.targetSettleY - leaf.settleStartY) * t

                            if (t >= 1.0f) {
                                leaf.isSettling = false
                                leaf.isResting = true
                                leaf.rotation = leaf.targetSettleRotation
                                leaf.x = leaf.targetSettleX
                                leaf.y = leaf.targetSettleY
                            }
                        } else if (leaf.isResting) {
                            leaf.settleTimer += dt
                            val idleFade = (1f - (leaf.settleTimer / 90f)).coerceIn(0f, 1f)
                            if (idleFade > 0f) {
                                // Synced with the 8-second meditative breath cycle (4s in, 4s out -> 2*PI / 8s ≈ 0.785 rad/s)
                                val breathSyncWave = sin(totalElapsedSec * 0.785f + leaf.restSwayPhase)
                                leaf.rotation += dt * 0.22f * idleFade * breathSyncWave
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
        val calm = calmEventBlend(t)
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val baseDimension = min(w, h)
        val particleScale = (baseDimension / 390f).coerceIn(0.80f, 1.15f)
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

            // Organic Zen Wind Gust Animation
            val leafStagger = if (leaf.isResting) {
                ((leaf.x - 0.08f) / 0.84f * 0.22f).coerceIn(0f, 0.22f)
            } else {
                (leaf.depth.scaleMultiplier * 0.08f).coerceIn(0f, 0.15f)
            }
            val localProgress = if (blowAwayProgress > 0f) {
                ((blowAwayProgress - leafStagger) / (1f - leafStagger)).coerceIn(0f, 1f)
            } else 0f

            val blowOffset = if (localProgress > 0f) {
                val p = localProgress
                val dir = if (leaf.isResting) {
                    val side = leaf.x - 0.5f
                    if (side >= 0f) 1.15f else -1.15f
                } else {
                    if (leaf.swayPhase % 2f > 1f) 1.25f else -1.25f
                }
                val lateralGust = w * dir * (p.pow(1.3f) * 1.1f + sin(p * 3.1415f) * 0.12f)
                val upwardLift = -h * (p.pow(1.4f) * 0.75f + sin(p * 3.1415f + leaf.swayPhase) * 0.08f)
                Offset(lateralGust, upwardLift)
            } else Offset.Zero

            val blowRot = if (localProgress > 0f) {
                val rotDir = if (leaf.x >= 0.5f) 1f else -1f
                localProgress * 260f * rotDir
            } else 0f

            // Staggered spawn fade + top margin ether ingress
            val spawnFade = (leaf.spawnElapsedSec / 1.2f).coerceIn(0f, 1f)
            val topFade = if (!leaf.isResting && !leaf.isSettling) {
                ((leaf.y + 0.08f) / 0.14f).coerceIn(0f, 1f) * spawnFade
            } else 1.0f

            val vanishFade = if (leaf.isVanishing) (1f - (leaf.vanishTimer / 0.4f)).coerceIn(0f, 1f) else 1f
            val baseTargetAlpha = leaf.alpha * ambientAlpha * topFade
            val rawAlpha = when {
                localProgress > 0f -> baseTargetAlpha * (1f - localProgress.pow(1.3f))
                else -> (baseTargetAlpha * vanishFade).coerceIn(0f, 0.85f)
            }
            if (rawAlpha < 0.005f) return@forEach

            // Airborne leaves fade out near the horizontal center so the drifting
            // movement never competes with the timer/presets behind the middle.
            val centerFade = if (!leaf.isResting && !leaf.isSettling) {
                val dxCenter = abs(px - w / 2f)
                val edgeProximity = ((w * 0.28f - dxCenter) / (w * 0.28f)).coerceIn(0f, 1f)
                1f - 0.50f * edgeProximity
            } else 1f
            // The rare calm wave makes the canopy glow a touch brighter.
            val alpha = (rawAlpha * centerFade * (0.85f + 0.25f * calm)).coerceIn(0f, 0.9f)

            // Desaturate / blend background depth layer toward the active theme background
            val drawColor = if (leaf.depth == LeafDepth.BACKGROUND) {
                lerp(leaf.color, currentTheme.background, 0.35f)
            } else leaf.color

            val tumbleScaleX = if (localProgress > 0f) {
                val wave = cos(localProgress * 8.0f + leaf.tumblePhase)
                (wave * 0.35f + 0.65f).coerceIn(0.30f, 1.0f)
            } else if (!leaf.isResting && !leaf.isSettling) {
                val wave = cos(t * leaf.tumbleFrequency + leaf.tumblePhase)
                (wave * 0.15f + 0.85f).coerceIn(0.70f, 1.0f)
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
                isResting = leaf.isResting || leaf.isSettling,
                shapeAsymmetry = leaf.shapeAsymmetry,
                shapeStretch = leaf.shapeStretch
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
    isResting: Boolean,
    shapeAsymmetry: Float = 0f,
    shapeStretch: Float = 1f
) {
    rotate(degrees = rotation, pivot = center) {
        scale(scaleX = scaleX, scaleY = 1.0f, pivot = center) {
            shapePath.reset()
            detailPath.reset()

            val halfW = size * 0.44f * shapeStretch
            val halfH = size * 0.94f / shapeStretch
            val asym = halfW * 0.14f * shapeAsymmetry

            when (particleType) {
                ParticleType.BAMBOO_LEAF -> {
                    shapePath.moveTo(center.x, center.y - halfH)
                    shapePath.cubicTo(center.x + halfW * 1.08f + asym, center.y - halfH * 0.32f, center.x + halfW * 0.88f + asym, center.y + halfH * 0.38f, center.x, center.y + halfH)
                    shapePath.cubicTo(center.x - halfW * 0.88f + asym, center.y + halfH * 0.38f, center.x - halfW * 1.08f + asym, center.y - halfH * 0.32f, center.x, center.y - halfH)
                    shapePath.close()

                    detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                    detailPath.quadraticTo(center.x + asym * 0.4f, center.y, center.x, center.y + halfH * 0.88f)
                    val branches = 3
                    for (i in 1..branches) {
                        val fy = center.y - halfH * 0.7f + (halfH * 1.4f) * (i / (branches + 1f))
                        val fx = center.x + asym * 0.4f * (i / (branches + 1f))
                        detailPath.moveTo(fx, fy)
                        detailPath.lineTo(fx + halfW * 0.55f, fy - halfH * 0.12f)
                        detailPath.moveTo(fx, fy)
                        detailPath.lineTo(fx - halfW * 0.55f, fy - halfH * 0.12f)
                    }
                }
                ParticleType.BODHI_LEAF -> {
                    // Classic Zen Forest Leaf (Graceful Elliptic / Ovate Leaf):
                    // Pointed apex, smooth organic swelling body, and softly tapered base with tiny petiole stem
                    val tipY = center.y - halfH * 0.96f
                    val baseY = center.y + halfH * 0.92f
                    val stemTipY = center.y + halfH * 1.06f

                    shapePath.moveTo(center.x, tipY)
                    // Right leaf margin: smooth elliptic curve with subtle organic asymmetry
                    shapePath.cubicTo(
                        center.x + halfW * 0.98f + asym, center.y - halfH * 0.42f,
                        center.x + halfW * 1.12f + asym, center.y + halfH * 0.22f,
                        center.x + halfW * 0.18f, baseY
                    )
                    // Tiny petiole stem base
                    shapePath.lineTo(center.x, stemTipY)
                    shapePath.lineTo(center.x - halfW * 0.18f, baseY)
                    // Left leaf margin: smooth symmetric/organic return curve
                    shapePath.cubicTo(
                        center.x - halfW * 1.12f + asym, center.y + halfH * 0.22f,
                        center.x - halfW * 0.98f + asym, center.y - halfH * 0.42f,
                        center.x, tipY
                    )
                    shapePath.close()

                    // Fine central midrib extending from stem to tip
                    detailPath.moveTo(center.x, tipY + halfH * 0.05f)
                    detailPath.quadraticTo(center.x + asym * 0.20f, center.y, center.x, stemTipY - halfH * 0.04f)

                    // Delicate pinnate secondary lateral veins
                    val branches = 4
                    for (i in 1..branches) {
                        val frac = i / (branches + 1f)
                        val fy = center.y - halfH * 0.50f + (halfH * 1.25f) * frac
                        val fx = center.x + asym * 0.20f * frac
                        val span = halfW * (0.62f - frac * 0.12f)

                        // Right side vein
                        detailPath.moveTo(fx, fy)
                        detailPath.quadraticTo(fx + span * 0.55f, fy - halfH * 0.08f, fx + span, fy - halfH * 0.04f)

                        // Left side vein
                        detailPath.moveTo(fx, fy)
                        detailPath.quadraticTo(fx - span * 0.55f, fy - halfH * 0.08f, fx - span, fy - halfH * 0.04f)
                    }
                }
                ParticleType.SAKURA_PETAL -> {
                    val topY = center.y - halfH * 0.95f
                    val cleft = halfH * 0.08f
                    shapePath.moveTo(center.x, center.y + halfH * 0.90f)
                    shapePath.cubicTo(center.x - halfW * 0.98f + asym, center.y + halfH * 0.25f, center.x - halfW * 1.08f + asym, center.y - halfH * 0.55f, center.x - halfW * 0.35f + asym, topY)
                    shapePath.quadraticTo(center.x, topY + cleft, center.x + halfW * 0.35f + asym, topY)
                    shapePath.cubicTo(center.x + halfW * 1.08f + asym, center.y - halfH * 0.55f, center.x + halfW * 0.98f + asym, center.y + halfH * 0.25f, center.x, center.y + halfH * 0.90f)
                    shapePath.close()

                    detailPath.moveTo(center.x, topY + cleft * 1.5f)
                    detailPath.lineTo(center.x, center.y + halfH * 0.75f)
                    val branches = 2
                    for (i in 1..branches) {
                        val fy = center.y - halfH * 0.4f + (halfH * 0.9f) * (i / (branches + 1f))
                        val fx = center.x + asym * 0.3f * (i / (branches + 1f))
                        detailPath.moveTo(fx, fy)
                        detailPath.lineTo(fx + halfW * 0.40f, fy - halfH * 0.06f)
                        detailPath.moveTo(fx, fy)
                        detailPath.lineTo(fx - halfW * 0.40f, fy - halfH * 0.06f)
                    }
                }
            }

            if (isGlowing) {
                val haloAlpha = (0.16f + 0.18f * breathingGlowFactor).coerceIn(0.10f, 0.38f)
                drawPath(path = shapePath, color = glowColor.copy(alpha = haloAlpha), style = Stroke(width = 3.5f))
            }

            drawPath(path = shapePath, color = color)

            val veinAlpha = if (isResting) (0.28f * color.alpha).coerceIn(0.08f, 0.35f)
                            else (0.22f * color.alpha).coerceIn(0.05f, 0.28f)
            drawPath(
                path = detailPath,
                color = Color.Black.copy(alpha = veinAlpha),
                style = Stroke(width = if (isResting) 1.1f else 0.9f)
            )

            drawPath(
                path = shapePath,
                color = Color.Black.copy(alpha = (0.15f * color.alpha).coerceIn(0.03f, 0.20f)),
                style = Stroke(width = 0.8f)
            )
        }
    }
}