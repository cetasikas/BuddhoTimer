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
import kotlin.math.sin
import kotlin.random.Random

private data class LeafParticle(
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

private data class SettledLeaf(
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
    leafCount: Int = 14,
    triggerBurst: Int = 0,
    ambientAlpha: Float = 0.28f,
    isGlowing: Boolean = false,
    pileUpAtBottom: Boolean = false,
    sessionProgressRatio: Float = 0.0f
) {
    val leaves = remember {
        mutableStateListOf<LeafParticle>().apply {
            addAll(generateAmbientLeaves(leafCount))
        }
    }

    // Rich multi-tiered stacked bed of leaves along the bottom
    val settledBed = remember {
        generateStackedBed(65)
    }

    // Trigger burst leaves on action
    LaunchedEffect(triggerBurst) {
        if (triggerBurst > 0) {
            val burstLeaves = generateBurstLeaves(12)
            leaves.addAll(burstLeaves)
        }
    }

    var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    val iterator = leaves.listIterator()
                    while (iterator.hasNext()) {
                        val leaf = iterator.next()
                        leaf.yRatio += leaf.speedY * dt
                        leaf.rotation += leaf.rotationSpeed * dt

                        if (leaf.isBurst) {
                            leaf.lifeRatio -= dt * 0.28f
                            if (leaf.lifeRatio <= 0f || leaf.yRatio > 1.15f) {
                                iterator.remove()
                                continue
                            }
                        } else {
                            if (leaf.yRatio >= 1.05f) {
                                leaf.yRatio = -0.08f
                                leaf.xRatio = Random.nextFloat()
                            }
                        }
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    val leafPath = remember { Path() }
    val veinPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val currentTimeSec = (lastFrameTimeNanos / 1_000_000_000f)
        // Gentle, soothing 5.0-second breathing sine oscillation
        val breathingCycle = (sin(currentTimeSec * 1.25f) * 0.5f + 0.5f)

        // 1. Draw settled stacked leaves coinciding with session elapsed progress
        if (pileUpAtBottom && sessionProgressRatio > 0.01f) {
            val maxVisibleSettled = (sessionProgressRatio.coerceIn(0f, 1.25f) * settledBed.size).toInt().coerceAtMost(settledBed.size)
            for (i in 0 until maxVisibleSettled) {
                val settled = settledBed[i]
                val posX = settled.xRatio * width
                val posY = settled.yRatio * height
                val currentAlpha = (settled.alpha * ambientAlpha * 1.5f).coerceIn(0.12f, 0.75f)

                drawRealisticLeaf(
                    path = leafPath,
                    veinPath = veinPath,
                    center = Offset(posX, posY),
                    size = settled.sizePx,
                    rotation = settled.rotation,
                    color = settled.color.copy(alpha = currentAlpha),
                    isGlowing = isGlowing,
                    breathingGlowFactor = breathingCycle,
                    isResting = true
                )
            }
        }

        // 2. Draw active floating leaves
        leaves.forEach { leaf ->
            val sway = sin(currentTimeSec * leaf.swayFreq + leaf.swayPhase) * (leaf.swayAmpRatio * width)
            val posX = (leaf.xRatio * width + sway).coerceIn(-50f, width + 50f)
            val posY = leaf.yRatio * height
            val currentAlpha = (leaf.alpha * leaf.lifeRatio * ambientAlpha).coerceIn(0f, 1f)

            if (currentAlpha > 0.01f) {
                drawRealisticLeaf(
                    path = leafPath,
                    veinPath = veinPath,
                    center = Offset(posX, posY),
                    size = leaf.sizePx,
                    rotation = leaf.rotation,
                    color = leaf.color.copy(alpha = currentAlpha),
                    isGlowing = isGlowing,
                    breathingGlowFactor = breathingCycle,
                    isResting = false
                )
            }
        }
    }
}

private fun DrawScope.drawRealisticLeaf(
    path: Path,
    veinPath: Path,
    center: Offset,
    size: Float,
    rotation: Float,
    color: Color,
    isGlowing: Boolean,
    breathingGlowFactor: Float,
    isResting: Boolean = false
) {
    rotate(degrees = rotation, pivot = center) {
        path.reset()
        val halfW = size * 0.38f
        val halfH = size * 0.88f

        // Natural Bodhi / Bamboo organic contour
        path.moveTo(center.x, center.y - halfH)
        path.cubicTo(
            center.x + halfW * 1.05f, center.y - halfH * 0.32f,
            center.x + halfW * 0.85f, center.y + halfH * 0.38f,
            center.x, center.y + halfH
        )
        path.cubicTo(
            center.x - halfW * 0.85f, center.y + halfH * 0.38f,
            center.x - halfW * 1.05f, center.y - halfH * 0.32f,
            center.x, center.y - halfH
        )
        path.close()

        // Subtle, calming breathing glow aura during active meditation
        if (isGlowing) {
            val haloAlpha = (0.18f + 0.22f * breathingGlowFactor).coerceIn(0.12f, 0.45f)
            val rimAlpha = (0.28f + 0.25f * breathingGlowFactor).coerceIn(0.20f, 0.58f)

            // Soft Translucent Sage Outer Halo
            drawPath(
                path = path,
                color = Color(0xFFAECBB0).copy(alpha = haloAlpha),
                style = Stroke(width = if (isResting) 3.5f else 4.5f)
            )
            // Delicate Inner Sage Rim
            drawPath(
                path = path,
                color = Color(0xFF8BA888).copy(alpha = rimAlpha),
                style = Stroke(width = if (isResting) 1.5f else 2.0f)
            )
        }

        // Fill leaf body
        drawPath(path = path, color = color)

        // Delicate Leaf Texture: Central Spine & Side Ribs
        veinPath.reset()
        // Central spine
        veinPath.moveTo(center.x, center.y - halfH * 0.85f)
        veinPath.lineTo(center.x, center.y + halfH * 0.88f)

        // Subtle side ribs
        val stepY = halfH * 0.38f
        // Top veins
        veinPath.moveTo(center.x, center.y - stepY * 0.6f)
        veinPath.quadraticTo(center.x + halfW * 0.45f, center.y - stepY * 0.85f, center.x + halfW * 0.65f, center.y - stepY * 0.7f)
        veinPath.moveTo(center.x, center.y - stepY * 0.6f)
        veinPath.quadraticTo(center.x - halfW * 0.45f, center.y - stepY * 0.85f, center.x - halfW * 0.65f, center.y - stepY * 0.7f)

        // Middle veins
        veinPath.moveTo(center.x, center.y + stepY * 0.1f)
        veinPath.quadraticTo(center.x + halfW * 0.55f, center.y - stepY * 0.15f, center.x + halfW * 0.7f, center.y)
        veinPath.moveTo(center.x, center.y + stepY * 0.1f)
        veinPath.quadraticTo(center.x - halfW * 0.55f, center.y - stepY * 0.15f, center.x - halfW * 0.7f, center.y)

        // Draw leaf veins with gentle translucent stroke
        val veinColor = Color(0xFF041014).copy(alpha = (0.28f * color.alpha).coerceIn(0.10f, 0.40f))
        drawPath(
            path = veinPath,
            color = veinColor,
            style = Stroke(width = 1.0f)
        )
    }
}

// Harmonious Calming Sage & Earthy Moss Palette
private val leafPalette = listOf(
    Color(0xFF8BA888), // Calming Sage Green
    Color(0xFFAECBB0), // Light Sage Highlight
    Color(0xFF759772), // Forest Moss Green
    Color(0xFF9AA67A), // Warm Olive Sage
    Color(0xFF8E9FA5), // Moonlit Slate Blue
    Color(0xFFC8AD7F)  // Warm Sand / Antique Bamboo
)

private fun generateStackedBed(count: Int): List<SettledLeaf> {
    val random = Random(108)
    return List(count) { i ->
        // 3-tiered accumulation: Layer 0 (Floor base), Layer 1 (Middle mound), Layer 2 (Upper stack)
        val layer = when {
            i < count * 0.45f -> 0
            i < count * 0.80f -> 1
            else -> 2
        }

        val baseY = when (layer) {
            0 -> 0.95f + random.nextFloat() * 0.035f
            1 -> 0.91f + random.nextFloat() * 0.040f
            else -> 0.87f + random.nextFloat() * 0.045f
        }

        SettledLeaf(
            xRatio = ((i.toFloat() / count.toFloat()) + (random.nextFloat() - 0.5f) * 0.16f).coerceIn(0.02f, 0.98f),
            yRatio = baseY,
            rotation = (random.nextFloat() - 0.5f) * 75f + if (i % 2 == 0) 20f else -20f,
            sizePx = random.nextFloat() * 7f + 16f,
            alpha = random.nextFloat() * 0.30f + 0.45f,
            color = leafPalette[i % leafPalette.size],
            layer = layer
        )
    }.shuffled(Random(42))
}

private fun generateAmbientLeaves(count: Int): List<LeafParticle> {
    return List(count) {
        LeafParticle(
            xRatio = Random.nextFloat(),
            yRatio = Random.nextFloat(),
            speedY = Random.nextFloat() * 0.040f + 0.020f,
            swayFreq = Random.nextFloat() * 1.4f + 1.1f,
            swayAmpRatio = Random.nextFloat() * 0.030f + 0.015f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 40f,
            sizePx = Random.nextFloat() * 12f + 15f,
            alpha = Random.nextFloat() * 0.35f + 0.35f,
            color = leafPalette.random()
        )
    }
}

private fun generateBurstLeaves(count: Int): List<LeafParticle> {
    return List(count) {
        LeafParticle(
            xRatio = Random.nextFloat() * 0.8f + 0.1f,
            yRatio = Random.nextFloat() * 0.35f,
            speedY = Random.nextFloat() * 0.09f + 0.05f,
            swayFreq = Random.nextFloat() * 2.2f + 1.6f,
            swayAmpRatio = Random.nextFloat() * 0.05f + 0.025f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 80f,
            sizePx = Random.nextFloat() * 14f + 14f,
            alpha = Random.nextFloat() * 0.4f + 0.45f,
            color = leafPalette.random(),
            isBurst = true
        )
    }
}
