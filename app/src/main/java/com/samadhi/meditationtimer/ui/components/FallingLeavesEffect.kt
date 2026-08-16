package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleState(
    var xRatio: Float,
    var yRatio: Float,
    val speedY: Float,
    val swayFreq: Float,
    val swayAmpRatio: Float,
    val swayPhase: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val sizePx: Float,
    val alpha: Float,
    val color: Color,
    val isBlossomFlower: Boolean = false,
    val isBurst: Boolean = false,
    var lifeRatio: Float = 1.0f
)

private data class SettledParticle(
    val xRatio: Float,
    val yRatio: Float,
    val rotation: Float,
    val sizePx: Float,
    val alpha: Float,
    val color: Color,
    val isBlossomFlower: Boolean = false,
    val layer: Int
)

@Composable
fun FallingLeavesEffect(
    modifier: Modifier = Modifier,
    leafCount: Int = 16,
    triggerBurst: Int = 0,
    ambientAlpha: Float = 0.32f,
    isGlowing: Boolean = false,
    pileUpAtBottom: Boolean = false,
    sessionProgressRatio: Float = 0.0f
) {
    val currentTheme = LocalZenTheme.current

    val particles = remember(currentTheme.id) {
        mutableStateListOf<ParticleState>().apply {
            addAll(generateAmbientParticles(leafCount, currentTheme.particleType, currentTheme.particlePalette))
        }
    }

    // Evenly distributed 3-tier settled bed across the screen width (90 particles)
    val settledBed = remember(currentTheme.id) {
        generateEvenlyDistributedBed(90, currentTheme.particleType, currentTheme.particlePalette)
    }

    // Trigger burst particles on action
    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            val burstParticles = generateBurstParticles(14, currentTheme.particleType, currentTheme.particlePalette)
            particles.addAll(burstParticles)
        }
    }

    var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    val iterator = particles.listIterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.yRatio += p.speedY * dt
                        p.rotation += p.rotationSpeed * dt

                        if (p.isBurst) {
                            p.lifeRatio -= dt * 0.28f
                            if (p.lifeRatio <= 0f || p.yRatio > 1.15f) {
                                iterator.remove()
                                continue
                            }
                        } else {
                            if (p.yRatio >= 1.05f) {
                                p.yRatio = -0.08f
                                p.xRatio = Random.nextFloat()
                            }
                        }
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    val shapePath = remember { Path() }
    val detailPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val currentTimeSec = (lastFrameTimeNanos / 1_000_000_000f)
        // Gentle 4.8s breathing wave
        val breathingCycle = (sin(currentTimeSec * 1.3f) * 0.5f + 0.5f)

        // 1. Draw settled stacked particles (Starts completely blank at 0% progress and slowly fills evenly)
        if (pileUpAtBottom && sessionProgressRatio > 0.005f) {
            val totalVisible = (sessionProgressRatio.coerceIn(0f, 1.0f) * settledBed.size).toInt().coerceIn(0, settledBed.size)

            for (i in 0 until totalVisible) {
                val settled = settledBed[i]
                val posX = settled.xRatio * width
                val posY = settled.yRatio * height
                val currentAlpha = (settled.alpha * ambientAlpha * 1.35f).coerceIn(0.15f, 0.75f)

                drawThemeParticle(
                    particleType = currentTheme.particleType,
                    shapePath = shapePath,
                    detailPath = detailPath,
                    center = Offset(posX, posY),
                    size = settled.sizePx,
                    rotation = settled.rotation,
                    color = settled.color.copy(alpha = currentAlpha),
                    glowColor = currentTheme.primaryLight,
                    isBlossomFlower = settled.isBlossomFlower,
                    isGlowing = isGlowing,
                    breathingGlowFactor = breathingCycle,
                    isResting = true
                )
            }
        }

        // 2. Draw active floating particles
        particles.forEach { p ->
            val sway = sin(currentTimeSec * p.swayFreq + p.swayPhase) * (p.swayAmpRatio * width)
            val posX = (p.xRatio * width + sway).coerceIn(-50f, width + 50f)
            val posY = p.yRatio * height
            val currentAlpha = (p.alpha * p.lifeRatio * ambientAlpha).coerceIn(0f, 1f)

            if (currentAlpha > 0.01f) {
                drawThemeParticle(
                    particleType = currentTheme.particleType,
                    shapePath = shapePath,
                    detailPath = detailPath,
                    center = Offset(posX, posY),
                    size = p.sizePx,
                    rotation = p.rotation,
                    color = p.color.copy(alpha = currentAlpha),
                    glowColor = currentTheme.primaryLight,
                    isBlossomFlower = p.isBlossomFlower,
                    isGlowing = isGlowing,
                    breathingGlowFactor = breathingCycle,
                    isResting = false
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
    isBlossomFlower: Boolean,
    isGlowing: Boolean,
    breathingGlowFactor: Float,
    isResting: Boolean
) {
    rotate(degrees = rotation, pivot = center) {
        shapePath.reset()
        detailPath.reset()

        val halfW = size * 0.40f
        val halfH = size * 0.88f

        if (isBlossomFlower) {
            // Full Blossom Flower (Sakura 5-petal blossom or Sacred Lotus blossom)
            val petalCount = if (particleType == ParticleType.LOTUS_PETAL) 8 else 5
            val petalRadius = size * 0.42f
            val angleStep = 360f / petalCount

            // Outer subtle breathing aura
            if (isGlowing) {
                val haloAlpha = (0.14f + 0.16f * breathingGlowFactor).coerceIn(0.08f, 0.35f)
                drawCircle(
                    color = glowColor.copy(alpha = haloAlpha),
                    radius = size * 0.58f,
                    center = center
                )
            }

            // Draw Flower Petals
            for (p in 0 until petalCount) {
                val angleRad = Math.toRadians((p * angleStep).toDouble())
                val px = center.x + (cos(angleRad) * petalRadius * 0.55f).toFloat()
                val py = center.y + (sin(angleRad) * petalRadius * 0.55f).toFloat()

                drawCircle(
                    color = color,
                    radius = petalRadius * 0.48f,
                    center = Offset(px, py)
                )
            }

            // Flower Center / Pistil
            val coreColor = if (particleType == ParticleType.LOTUS_PETAL) Color(0xFFFBBF24) else Color(0xFFFFF1F2)
            drawCircle(
                color = coreColor.copy(alpha = color.alpha),
                radius = petalRadius * 0.28f,
                center = center
            )
            return@rotate
        }

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

                // Vein details
                detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                detailPath.lineTo(center.x, center.y + halfH * 0.88f)
                val stepY = halfH * 0.38f
                detailPath.moveTo(center.x, center.y - stepY * 0.6f)
                detailPath.quadraticTo(center.x + halfW * 0.5f, center.y - stepY * 0.85f, center.x + halfW * 0.65f, center.y - stepY * 0.7f)
                detailPath.moveTo(center.x, center.y - stepY * 0.6f)
                detailPath.quadraticTo(center.x - halfW * 0.5f, center.y - stepY * 0.85f, center.x - halfW * 0.65f, center.y - stepY * 0.7f)
                detailPath.moveTo(center.x, center.y + stepY * 0.1f)
                detailPath.quadraticTo(center.x + halfW * 0.55f, center.y - stepY * 0.15f, center.x + halfW * 0.7f, center.y)
                detailPath.moveTo(center.x, center.y + stepY * 0.1f)
                detailPath.quadraticTo(center.x - halfW * 0.55f, center.y - stepY * 0.15f, center.x - halfW * 0.7f, center.y)
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

                // Soft central petal crease
                detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                detailPath.quadraticTo(center.x + halfW * 0.20f, center.y, center.x, center.y + halfH * 0.85f)
            }

            ParticleType.SAKURA_PETAL -> {
                // 🌸 Iconic Notched Cherry Blossom Petal (Cleft split tip)
                val topY = center.y - halfH * 0.95f
                shapePath.moveTo(center.x, center.y + halfH)
                shapePath.cubicTo(
                    center.x - halfW * 1.25f, center.y + halfH * 0.35f,
                    center.x - halfW * 1.15f, center.y - halfH * 0.45f,
                    center.x - halfW * 0.60f, topY
                )
                // Notched tip indent
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

                // Delicate petal fold line
                detailPath.moveTo(center.x, topY + halfH * 0.3f)
                detailPath.lineTo(center.x, center.y + halfH * 0.8f)
            }
        }

        // Subtle, peaceful breathing aura around leaf/petal
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

        // Draw Petal / Leaf Details
        val detailColor = Color(0xFF041014).copy(alpha = (0.24f * color.alpha).coerceIn(0.06f, 0.35f))
        drawPath(
            path = detailPath,
            color = detailColor,
            style = Stroke(width = 1.0f)
        )
    }
}

// Low-discrepancy sequence generation to guarantee even screen width dispersion from leaf 1 to 90
private fun generateEvenlyDistributedBed(count: Int, particleType: ParticleType, palette: List<Color>): List<SettledParticle> {
    val random = Random(777)
    return List(count) { i ->
        // 3-tiered accumulation: Layer 0 (Floor base), Layer 1 (Mid mound), Layer 2 (Upper stack)
        val layer = when {
            i < count * 0.45f -> 0
            i < count * 0.80f -> 1
            else -> 2
        }

        val baseY = when (layer) {
            0 -> 0.955f + random.nextFloat() * 0.030f
            1 -> 0.915f + random.nextFloat() * 0.035f
            else -> 0.870f + random.nextFloat() * 0.040f
        }

        // Golden Ratio dispersion for perfectly uniform width distribution without clumping on either side
        val goldX = ((i * 0.6180339887f) % 1.0f) * 0.90f + 0.05f
        val xJitter = (random.nextFloat() - 0.5f) * 0.035f
        val xRatio = (goldX + xJitter).coerceIn(0.04f, 0.96f)

        val isBlossom = (particleType != ParticleType.BAMBOO_LEAF) && (i % 7 == 0)

        SettledParticle(
            xRatio = xRatio,
            yRatio = baseY,
            rotation = (random.nextFloat() - 0.5f) * 80f + if (i % 2 == 0) 20f else -20f,
            sizePx = random.nextFloat() * 8f + 18f,
            alpha = random.nextFloat() * 0.25f + 0.55f,
            color = palette[i % palette.size],
            isBlossomFlower = isBlossom,
            layer = layer
        )
    }
}

private fun generateAmbientParticles(count: Int, particleType: ParticleType, palette: List<Color>): List<ParticleState> {
    return List(count) { i ->
        val isBlossom = (particleType != ParticleType.BAMBOO_LEAF) && (i % 5 == 0)
        ParticleState(
            xRatio = Random.nextFloat(),
            yRatio = Random.nextFloat(),
            speedY = Random.nextFloat() * 0.040f + 0.020f,
            swayFreq = Random.nextFloat() * 1.4f + 1.1f,
            swayAmpRatio = Random.nextFloat() * 0.030f + 0.015f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 40f,
            sizePx = if (isBlossom) Random.nextFloat() * 8f + 20f else Random.nextFloat() * 10f + 16f,
            alpha = Random.nextFloat() * 0.30f + 0.45f,
            color = palette.random(),
            isBlossomFlower = isBlossom
        )
    }
}

private fun generateBurstParticles(count: Int, particleType: ParticleType, palette: List<Color>): List<ParticleState> {
    return List(count) { i ->
        val isBlossom = (particleType != ParticleType.BAMBOO_LEAF) && (i % 4 == 0)
        ParticleState(
            xRatio = Random.nextFloat() * 0.8f + 0.1f,
            yRatio = Random.nextFloat() * 0.35f,
            speedY = Random.nextFloat() * 0.09f + 0.05f,
            swayFreq = Random.nextFloat() * 2.2f + 1.6f,
            swayAmpRatio = Random.nextFloat() * 0.05f + 0.025f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 80f,
            sizePx = if (isBlossom) Random.nextFloat() * 10f + 20f else Random.nextFloat() * 12f + 16f,
            alpha = Random.nextFloat() * 0.35f + 0.55f,
            color = palette.random(),
            isBlossomFlower = isBlossom,
            isBurst = true
        )
    }
}
