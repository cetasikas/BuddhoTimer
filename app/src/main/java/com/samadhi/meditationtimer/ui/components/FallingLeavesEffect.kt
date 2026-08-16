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
            addAll(generateAmbientParticles(leafCount, currentTheme.particlePalette))
        }
    }

    // Rich 4-tiered dense stacked bed (up to 100 particles)
    val settledBed = remember(currentTheme.id) {
        generateDenseStackedBed(100, currentTheme.particlePalette)
    }

    // Trigger burst particles on action
    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            val burstParticles = generateBurstParticles(14, currentTheme.particlePalette)
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
        // Calming 4.8s breathing sine wave
        val breathingCycle = (sin(currentTimeSec * 1.3f) * 0.5f + 0.5f)

        // 1. Draw settled stacked particles (Starts with an immediate base bed and builds up richly)
        if (pileUpAtBottom) {
            val baseCount = 18 // Base resting bed from the start
            val progressiveCount = (sessionProgressRatio.coerceIn(0f, 1.25f) * (settledBed.size - baseCount)).toInt()
            val totalVisible = (baseCount + progressiveCount).coerceIn(baseCount, settledBed.size)

            for (i in 0 until totalVisible) {
                val settled = settledBed[i]
                val posX = settled.xRatio * width
                val posY = settled.yRatio * height
                val currentAlpha = (settled.alpha * ambientAlpha * 1.5f).coerceIn(0.15f, 0.85f)

                drawThemeParticle(
                    particleType = currentTheme.particleType,
                    shapePath = shapePath,
                    detailPath = detailPath,
                    center = Offset(posX, posY),
                    size = settled.sizePx,
                    rotation = settled.rotation,
                    color = settled.color.copy(alpha = currentAlpha),
                    glowColor = currentTheme.primaryLight,
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
                // Natural Bodhi / Bamboo Leaf Shape
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

                // Veins
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
                // Graceful Cupped Sacred Lotus Petal
                shapePath.moveTo(center.x, center.y - halfH * 0.95f)
                shapePath.cubicTo(
                    center.x + halfW * 1.25f, center.y - halfH * 0.40f,
                    center.x + halfW * 1.15f, center.y + halfH * 0.45f,
                    center.x, center.y + halfH
                )
                shapePath.cubicTo(
                    center.x - halfW * 1.15f, center.y + halfH * 0.45f,
                    center.x - halfW * 1.25f, center.y - halfH * 0.40f,
                    center.x, center.y - halfH * 0.95f
                )
                shapePath.close()

                // Delicate central petal spine & soft shading
                detailPath.moveTo(center.x, center.y - halfH * 0.85f)
                detailPath.quadraticTo(center.x + halfW * 0.15f, center.y, center.x, center.y + halfH * 0.85f)
            }

            ParticleType.SAKURA_PETAL -> {
                // Notched Cherry Blossom Petal
                val topY = center.y - halfH * 0.95f
                shapePath.moveTo(center.x, center.y + halfH)
                shapePath.cubicTo(
                    center.x - halfW * 1.2f, center.y + halfH * 0.35f,
                    center.x - halfW * 1.1f, center.y - halfH * 0.45f,
                    center.x - halfW * 0.55f, topY
                )
                // Distinctive sakura petal center notch
                shapePath.quadraticTo(
                    center.x, topY + halfH * 0.22f,
                    center.x + halfW * 0.55f, topY
                )
                shapePath.cubicTo(
                    center.x + halfW * 1.1f, center.y - halfH * 0.45f,
                    center.x + halfW * 1.2f, center.y + halfH * 0.35f,
                    center.x, center.y + halfH
                )
                shapePath.close()

                // Soft central crease
                detailPath.moveTo(center.x, topY + halfH * 0.3f)
                detailPath.lineTo(center.x, center.y + halfH * 0.8f)
            }
        }

        // Subtle, calming breathing glow aura during active meditation
        if (isGlowing) {
            val haloAlpha = (0.18f + 0.22f * breathingGlowFactor).coerceIn(0.12f, 0.45f)
            val rimAlpha = (0.28f + 0.25f * breathingGlowFactor).coerceIn(0.20f, 0.58f)

            // Outer Breathing Halo
            drawPath(
                path = shapePath,
                color = glowColor.copy(alpha = haloAlpha),
                style = Stroke(width = if (isResting) 3.5f else 4.5f)
            )
            // Delicate Inner Rim
            drawPath(
                path = shapePath,
                color = color.copy(alpha = rimAlpha),
                style = Stroke(width = if (isResting) 1.5f else 2.0f)
            )
        }

        // Fill Particle Body
        drawPath(path = shapePath, color = color)

        // Draw Petal / Leaf Details
        val detailColor = Color(0xFF041014).copy(alpha = (0.26f * color.alpha).coerceIn(0.08f, 0.38f))
        drawPath(
            path = detailPath,
            color = detailColor,
            style = Stroke(width = 1.0f)
        )
    }
}

private fun generateDenseStackedBed(count: Int, palette: List<Color>): List<SettledParticle> {
    val random = Random(108)
    return List(count) { i ->
        // 4-tiered accumulation: Layer 0 (Floor base), Layer 1 (Mid mound), Layer 2 (Upper stack), Layer 3 (High drifts)
        val layer = when {
            i < count * 0.40f -> 0
            i < count * 0.70f -> 1
            i < count * 0.90f -> 2
            else -> 3
        }

        val baseY = when (layer) {
            0 -> 0.95f + random.nextFloat() * 0.035f
            1 -> 0.90f + random.nextFloat() * 0.045f
            2 -> 0.85f + random.nextFloat() * 0.050f
            else -> 0.79f + random.nextFloat() * 0.055f
        }

        SettledParticle(
            xRatio = ((i.toFloat() / count.toFloat()) + (random.nextFloat() - 0.5f) * 0.18f).coerceIn(0.02f, 0.98f),
            yRatio = baseY,
            rotation = (random.nextFloat() - 0.5f) * 90f + if (i % 2 == 0) 25f else -25f,
            sizePx = random.nextFloat() * 10f + 18f,
            alpha = random.nextFloat() * 0.30f + 0.50f,
            color = palette[i % palette.size],
            layer = layer
        )
    }.shuffled(Random(42))
}

private fun generateAmbientParticles(count: Int, palette: List<Color>): List<ParticleState> {
    return List(count) {
        ParticleState(
            xRatio = Random.nextFloat(),
            yRatio = Random.nextFloat(),
            speedY = Random.nextFloat() * 0.040f + 0.020f,
            swayFreq = Random.nextFloat() * 1.4f + 1.1f,
            swayAmpRatio = Random.nextFloat() * 0.030f + 0.015f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 40f,
            sizePx = Random.nextFloat() * 12f + 16f,
            alpha = Random.nextFloat() * 0.35f + 0.40f,
            color = palette.random()
        )
    }
}

private fun generateBurstParticles(count: Int, palette: List<Color>): List<ParticleState> {
    return List(count) {
        ParticleState(
            xRatio = Random.nextFloat() * 0.8f + 0.1f,
            yRatio = Random.nextFloat() * 0.35f,
            speedY = Random.nextFloat() * 0.09f + 0.05f,
            swayFreq = Random.nextFloat() * 2.2f + 1.6f,
            swayAmpRatio = Random.nextFloat() * 0.05f + 0.025f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 80f,
            sizePx = Random.nextFloat() * 14f + 16f,
            alpha = Random.nextFloat() * 0.4f + 0.50f,
            color = palette.random(),
            isBurst = true
        )
    }
}
