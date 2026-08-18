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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Slot metadata for a petal or leaf in the Sacred Zen Botanical Mandala.
 */
private data class MandalaSlot(
    val slotIndex: Int,
    val relX: Float,
    val relY: Float,
    val targetRotation: Float,
    val sizeScale: Float,
    val layer: Int
)

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
    // Nesting transition into mandala slot
    var isSettling: Boolean = false,
    var settleProgress: Float = 0.0f,
    var settleDuration: Float = 1.1f,
    var settleStartX: Float = 0f,
    var settleStartY: Float = 0f,
    var settleStartRot: Float = 0f,
    var settleTargetX: Float = 0f,
    var settleTargetY: Float = 0f,
    var settleTargetRot: Float = 0f,
    var targetSlotIndex: Int = -1
)

private data class SettledLeaf(
    val slotIndex: Int,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val size: Float,
    val alpha: Float,
    val color: Color,
    val depthLayer: Int = 0 // 0 = outer foundation, 1 = mid interlocking, 2 = inner crown, 3 = heart core
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

    // Precompute 26 Sacred Zen Mandala slots centered at bottom (X = 0.50f, Y = 0.865f)
    val mandalaSlots = remember {
        generateMandalaSlots(centerX = 0.50f, centerY = 0.865f)
    }

    // Active falling leaves (staggered from above the top at session start)
    val activeLeaves = remember(currentTheme.id, leafCount) {
        mutableStateListOf<ActiveLeaf>().apply {
            addAll(generateInitialActiveLeaves(leafCount, currentTheme.particlePalette, pileUpAtBottom))
        }
    }

    // Settled mandala leaves: Created ONCE per session lifecycle
    val settledLeaves = remember(currentTheme.id, pileUpAtBottom) {
        mutableStateListOf<SettledLeaf>().apply {
            if (pileUpAtBottom && initialSettledLeavesCount > 0) {
                addAll(generatePreStackedMandala(initialSettledLeavesCount, mandalaSlots, currentTheme.particlePalette))
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

    // Wind blow-away animation controller (Spiral whirlwind dissipation on exit)
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

    // Continuous monotonic clock for ambient mandala breathing and living sway
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

    // Physics update step: Falling trajectory with gentle spiritual guidance towards mandala slots
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
                            // 🍂 Gentle Nesting Phase: Smooth deceleration into sacred mandala slot
                            leaf.settleProgress += dt / leaf.settleDuration
                            val t = leaf.settleProgress.coerceIn(0f, 1f)
                            val ease = t * (2f - t) // Smooth natural cushioning into position

                            leaf.currentX = leaf.settleStartX + (leaf.settleTargetX - leaf.settleStartX) * ease
                            leaf.y = leaf.settleStartY + (leaf.settleTargetY - leaf.settleStartY) * ease
                            leaf.rotation = leaf.settleStartRot + (leaf.settleTargetRot - leaf.settleStartRot) * ease

                            if (leaf.settleProgress >= 1f) {
                                val slotIdx = if (leaf.targetSlotIndex >= 0) leaf.targetSlotIndex else settledLeaves.size % mandalaSlots.size
                                val slot = mandalaSlots[slotIdx]

                                settledLeaves.add(
                                    SettledLeaf(
                                        slotIndex = slotIdx,
                                        x = slot.relX,
                                        y = slot.relY,
                                        rotation = slot.targetRotation,
                                        size = leaf.size * slot.sizeScale,
                                        alpha = (leaf.alpha * 1.15f).coerceIn(0.70f, 0.98f),
                                        color = leaf.color,
                                        depthLayer = slot.layer
                                    )
                                )

                                // Recycle leaf to spawn high above the viewport for next gentle descent
                                leaf.isSettling = false
                                leaf.settleProgress = 0f
                                leaf.vx = 0f
                                leaf.vy = 0f
                                leaf.targetSlotIndex = -1
                                leaf.y = -(Random.nextFloat() * 0.18f + 0.06f)
                                leaf.xBase = Random.nextFloat() * 0.84f + 0.08f
                                leaf.currentX = leaf.xBase
                                leaf.rotation = Random.nextFloat() * 360f
                            }
                            continue
                        }

                        if (pileUpAtBottom) {
                            // Next target slot in the Sacred Mandala
                            val targetSlotIdx = settledLeaves.size % mandalaSlots.size
                            val targetSlot = mandalaSlots[targetSlotIdx]

                            // Gentle spiritual wind drift guiding leaf toward next open mandala slot
                            if (leaf.y > 0.25f) {
                                val dx = (targetSlot.relX - leaf.xBase)
                                val steerForce = dx * 0.035f * dt
                                leaf.vx += steerForce
                                leaf.vx = leaf.vx.coerceIn(-0.030f, 0.030f)
                            }

                            // Trigger nesting when leaf enters the mandala basin
                            if (leaf.y >= targetSlot.relY - 0.020f) {
                                leaf.isSettling = true
                                leaf.targetSlotIndex = targetSlotIdx
                                leaf.settleProgress = 0f
                                leaf.settleDuration = Random.nextFloat() * 0.25f + 0.95f // 0.95s - 1.2s serene nesting
                                leaf.settleStartX = leaf.currentX
                                leaf.settleStartY = leaf.y
                                leaf.settleStartRot = leaf.rotation

                                leaf.settleTargetX = targetSlot.relX
                                leaf.settleTargetY = targetSlot.relY

                                // Rotate into exact mandala angle
                                val normDeg = ((leaf.rotation % 360f) + 360f) % 360f
                                val targetNorm = ((targetSlot.targetRotation % 360f) + 360f) % 360f
                                var delta = targetNorm - normDeg
                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f
                                leaf.settleTargetRot = leaf.rotation + delta
                            }
                        }

                        leaf.xBase = (leaf.xBase + leaf.vx * dt).coerceIn(0.02f, 0.98f)
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
        val mandalaCenterOffset = Offset(width * 0.50f, height * 0.865f)

        // 1. Draw Faint Minimalist Sacred Mandala Guideline Bed
        if (pileUpAtBottom) {
            val bedRadius = width * 0.16f
            val bedAlpha = 0.08f + 0.04f * breathingWave
            drawCircle(
                color = currentTheme.primaryLight.copy(alpha = bedAlpha),
                radius = bedRadius,
                center = mandalaCenterOffset,
                style = Stroke(width = 1.2f)
            )
            drawCircle(
                color = currentTheme.primary.copy(alpha = bedAlpha * 0.6f),
                radius = bedRadius * 0.60f,
                center = mandalaCenterOffset,
                style = Stroke(width = 1.0f)
            )
        }

        // 2. Draw Settled Sacred Zen Botanical Mandala (Symmetrical Blossoming Lotus/Mandala)
        if (pileUpAtBottom && settledLeaves.isNotEmpty()) {
            settledLeaves.forEach { settled ->
                val posX = settled.x * width
                val posY = settled.y * height

                // Soft harmonic breathing sway
                val bedSway = sin(currentTimeSec * 0.8f + settled.slotIndex * 0.6f) * 0.8f
                val restingRot = settled.rotation + bedSway

                // Whirlwind blow-away on session completion exit
                val blowOffset = if (blowAwayProgress > 0f) {
                    val progress = blowAwayProgress
                    val blowX = width * (0.8f * progress + (settled.x - 0.50f) * 0.6f)
                    val blowY = -height * (0.50f * progress * progress)
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
                    restingRot + blowAwayProgress * 420f
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
                    isGlowing = (settled.depthLayer >= 2),
                    breathingGlowFactor = breathingWave,
                    isResting = true
                )
            }

            // Central Glowing Jewel / Pistil when Inner Layers Bloom
            if (settledLeaves.size >= 16 && blowAwayProgress <= 0.01f) {
                val coreAlpha = (0.25f + 0.15f * breathingWave).coerceIn(0.15f, 0.45f)
                drawCircle(
                    color = currentTheme.primaryLight.copy(alpha = coreAlpha),
                    radius = width * 0.022f,
                    center = mandalaCenterOffset
                )
                drawCircle(
                    color = Color.White.copy(alpha = coreAlpha * 0.8f),
                    radius = width * 0.012f,
                    center = mandalaCenterOffset
                )
            }
        }

        // 3. Draw Active Falling Leaves
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
                style = Stroke(width = if (isResting) 2.6f else 3.5f)
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
        val initialY = if (pileUpAtBottom) {
            -(i.toFloat() / count.toFloat()) * 0.85f - 0.06f
        } else {
            (i.toFloat() / count.toFloat()) * 1.1f - 0.1f
        }

        ActiveLeaf(
            xBase = x,
            currentX = x,
            y = initialY,
            speedY = Random.nextFloat() * 0.009f + 0.008f, // Peaceful meditative descent (~20-25s)
            swayFreq = Random.nextFloat() * 0.45f + 0.55f,
            swayAmp = Random.nextFloat() * 0.022f + 0.018f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 12f,
            size = Random.nextFloat() * 8f + 18f,
            alpha = Random.nextFloat() * 0.30f + 0.55f,
            color = palette.random()
        )
    }
}

/**
 * Generates 26 concentric symmetrical slots for the Sacred Zen Botanical Mandala.
 */
private fun generateMandalaSlots(centerX: Float, centerY: Float): List<MandalaSlot> {
    val slots = mutableListOf<MandalaSlot>()
    var slotIdx = 0

    // Layer 0: Outer Foundation Ring (8 petals, R = 0.135f relative)
    val count0 = 8
    val rad0 = 0.135f
    for (i in 0 until count0) {
        val angleDeg = i * (360f / count0)
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val x = centerX + cos(angleRad) * (rad0 * 0.70f) // Aspect ratio corrected
        val y = centerY + sin(angleRad) * rad0
        slots.add(
            MandalaSlot(
                slotIndex = slotIdx++,
                relX = x,
                relY = y,
                targetRotation = angleDeg + 90f,
                sizeScale = 1.05f,
                layer = 0
            )
        )
    }

    // Layer 1: Interlocking Mid Ring (8 petals offset by 22.5°, R = 0.090f)
    val count1 = 8
    val rad1 = 0.090f
    for (i in 0 until count1) {
        val angleDeg = i * (360f / count1) + 22.5f
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val x = centerX + cos(angleRad) * (rad1 * 0.70f)
        val y = centerY + sin(angleRad) * rad1
        slots.add(
            MandalaSlot(
                slotIndex = slotIdx++,
                relX = x,
                relY = y,
                targetRotation = angleDeg + 90f,
                sizeScale = 0.88f,
                layer = 1
            )
        )
    }

    // Layer 2: Inner Crown (8 petals, R = 0.048f)
    val count2 = 8
    val rad2 = 0.048f
    for (i in 0 until count2) {
        val angleDeg = i * (360f / count2)
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val x = centerX + cos(angleRad) * (rad2 * 0.70f)
        val y = centerY + sin(angleRad) * rad2
        slots.add(
            MandalaSlot(
                slotIndex = slotIdx++,
                relX = x,
                relY = y,
                targetRotation = angleDeg + 90f,
                sizeScale = 0.70f,
                layer = 2
            )
        )
    }

    // Layer 3: Heart Core (2 central florets)
    for (i in 0 until 2) {
        slots.add(
            MandalaSlot(
                slotIndex = slotIdx++,
                relX = centerX,
                relY = centerY,
                targetRotation = i * 90f,
                sizeScale = 0.55f,
                layer = 3
            )
        )
    }

    return slots
}

/**
 * Pre-populates the Sacred Zen Botanical Mandala for the practice summary screen.
 */
private fun generatePreStackedMandala(
    count: Int,
    mandalaSlots: List<MandalaSlot>,
    palette: List<Color>
): List<SettledLeaf> {
    val totalAvailable = mandalaSlots.size
    val numToTake = count.coerceIn(1, totalAvailable)

    return List(numToTake) { i ->
        val slot = mandalaSlots[i]
        SettledLeaf(
            slotIndex = i,
            x = slot.relX,
            y = slot.relY,
            rotation = slot.targetRotation,
            size = 20f * slot.sizeScale,
            alpha = 0.88f,
            color = palette[i % palette.size],
            depthLayer = slot.layer
        )
    }
}
