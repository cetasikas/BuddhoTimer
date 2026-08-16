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
    val color: Color
)

@Composable
fun FallingLeavesEffect(
    modifier: Modifier = Modifier,
    leafCount: Int = 16,
    triggerBurst: Int = 0,
    ambientAlpha: Float = 0.32f,
    isGlowing: Boolean = false,
    pileUpAtBottom: Boolean = false
) {
    val leaves = remember {
        mutableStateListOf<LeafParticle>().apply {
            addAll(generateAmbientLeaves(leafCount))
        }
    }

    val settledLeaves = remember { mutableStateListOf<SettledLeaf>() }

    // Clear settled leaves when meditation is not active
    LaunchedEffect(pileUpAtBottom) {
        if (!pileUpAtBottom) {
            settledLeaves.clear()
        }
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
                            if (leaf.yRatio >= 0.94f) {
                                if (pileUpAtBottom && settledLeaves.size < 36 && Random.nextFloat() < 0.65f) {
                                    settledLeaves.add(
                                        SettledLeaf(
                                            xRatio = (leaf.xRatio + (Random.nextFloat() - 0.5f) * 0.05f).coerceIn(0.03f, 0.97f),
                                            yRatio = Random.nextFloat() * 0.06f + 0.92f,
                                            rotation = leaf.rotation + (Random.nextFloat() - 0.5f) * 30f,
                                            sizePx = leaf.sizePx * 0.95f,
                                            alpha = (leaf.alpha * 0.75f).coerceIn(0.2f, 0.65f),
                                            color = leaf.color
                                        )
                                    )
                                }
                                // Reset to top
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
        // Gentle, soothing 5-second breathing glow oscillation
        val breathingCycle = (sin(currentTimeSec * 1.25f) * 0.5f + 0.5f)

        // Draw settled leaves resting at the bottom
        settledLeaves.forEach { settled ->
            val posX = settled.xRatio * width
            val posY = settled.yRatio * height
            val currentAlpha = (settled.alpha * ambientAlpha).coerceIn(0f, 1f)

            if (currentAlpha > 0.01f) {
                drawRealisticLeaf(
                    path = leafPath,
                    veinPath = veinPath,
                    center = Offset(posX, posY),
                    size = settled.sizePx,
                    rotation = settled.rotation,
                    color = settled.color.copy(alpha = currentAlpha),
                    isGlowing = isGlowing,
                    breathingGlowFactor = breathingCycle
                )
            }
        }

        // Draw active falling leaves
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
                    breathingGlowFactor = breathingCycle
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
    breathingGlowFactor: Float
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

        // Luminous breathing glow effect when meditation is active
        if (isGlowing) {
            val outerGlowAlpha = (0.20f + 0.32f * breathingGlowFactor).coerceIn(0.1f, 0.6f)
            val innerGlowAlpha = (0.35f + 0.35f * breathingGlowFactor).coerceIn(0.2f, 0.8f)

            // Outer Soft Halo
            drawPath(
                path = path,
                color = Color(0xFF5EEAD4).copy(alpha = outerGlowAlpha * color.alpha),
                style = Stroke(width = 5.0f)
            )
            // Inner Luminous Rim
            drawPath(
                path = path,
                color = Color(0xFFE6FFFA).copy(alpha = innerGlowAlpha * color.alpha),
                style = Stroke(width = 2.0f)
            )
        }

        // Fill leaf body
        drawPath(path = path, color = color)

        // Delicate Leaf Texture: Central Rib & Side Veins
        veinPath.reset()
        // Central vein
        veinPath.moveTo(center.x, center.y - halfH * 0.85f)
        veinPath.lineTo(center.x, center.y + halfH * 0.88f)

        // Subtle side veins
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

        // Draw leaf veins with soft translucent stroke
        val veinColor = Color(0xFF041014).copy(alpha = (0.28f * color.alpha).coerceIn(0.08f, 0.4f))
        drawPath(
            path = veinPath,
            color = veinColor,
            style = Stroke(width = 1.0f)
        )
    }
}

private val leafPalette = listOf(
    Color(0xFF2EA8A4), // Teal Glaze
    Color(0xFF38C7C2), // Luminous Teal
    Color(0xFF5EEAD4), // Bright Zen Teal
    Color(0xFF8BA870), // Zen Moss Green
    Color(0xFF94A3B8), // Soft Moonlit Slate
    Color(0xFFD4AF37)  // Antique Gold Accent
)

private fun generateAmbientLeaves(count: Int): List<LeafParticle> {
    return List(count) {
        LeafParticle(
            xRatio = Random.nextFloat(),
            yRatio = Random.nextFloat(),
            speedY = Random.nextFloat() * 0.045f + 0.022f,
            swayFreq = Random.nextFloat() * 1.5f + 1.2f,
            swayAmpRatio = Random.nextFloat() * 0.035f + 0.015f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 45f,
            sizePx = Random.nextFloat() * 14f + 15f,
            alpha = Random.nextFloat() * 0.4f + 0.35f,
            color = leafPalette.random()
        )
    }
}

private fun generateBurstLeaves(count: Int): List<LeafParticle> {
    return List(count) {
        LeafParticle(
            xRatio = Random.nextFloat() * 0.8f + 0.1f,
            yRatio = Random.nextFloat() * 0.35f,
            speedY = Random.nextFloat() * 0.10f + 0.06f,
            swayFreq = Random.nextFloat() * 2.5f + 1.8f,
            swayAmpRatio = Random.nextFloat() * 0.06f + 0.03f,
            swayPhase = Random.nextFloat() * 6.28f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 90f,
            sizePx = Random.nextFloat() * 16f + 14f,
            alpha = Random.nextFloat() * 0.5f + 0.5f,
            color = leafPalette.random(),
            isBurst = true
        )
    }
}
