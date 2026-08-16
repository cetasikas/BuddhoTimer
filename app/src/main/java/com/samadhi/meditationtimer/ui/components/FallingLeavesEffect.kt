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

@Composable
fun FallingLeavesEffect(
    modifier: Modifier = Modifier,
    leafCount: Int = 16,
    triggerBurst: Int = 0,
    ambientAlpha: Float = 0.32f,
    isGlowing: Boolean = false
) {
    val leaves = remember {
        mutableStateListOf<LeafParticle>().apply {
            addAll(generateAmbientLeaves(leafCount))
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
                            if (leaf.yRatio > 1.1f) {
                                leaf.yRatio = -0.1f
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

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val currentTimeSec = (lastFrameTimeNanos / 1_000_000_000f)

        leaves.forEach { leaf ->
            val sway = sin(currentTimeSec * leaf.swayFreq + leaf.swayPhase) * (leaf.swayAmpRatio * width)
            val posX = (leaf.xRatio * width + sway).coerceIn(-50f, width + 50f)
            val posY = leaf.yRatio * height
            val currentAlpha = (leaf.alpha * leaf.lifeRatio * ambientAlpha).coerceIn(0f, 1f)

            if (currentAlpha > 0.01f) {
                drawLeaf(
                    path = leafPath,
                    center = Offset(posX, posY),
                    size = leaf.sizePx,
                    rotation = leaf.rotation,
                    color = leaf.color.copy(alpha = currentAlpha),
                    isGlowing = isGlowing
                )
            }
        }
    }
}

private fun DrawScope.drawLeaf(
    path: Path,
    center: Offset,
    size: Float,
    rotation: Float,
    color: Color,
    isGlowing: Boolean
) {
    rotate(degrees = rotation, pivot = center) {
        path.reset()
        val halfW = size * 0.36f
        val halfH = size * 0.88f

        // Graceful Plum Village Bodhi / Bamboo Leaf Curve
        path.moveTo(center.x, center.y - halfH)
        path.cubicTo(
            center.x + halfW, center.y - halfH * 0.3f,
            center.x + halfW * 0.9f, center.y + halfH * 0.4f,
            center.x, center.y + halfH
        )
        path.cubicTo(
            center.x - halfW * 0.9f, center.y + halfH * 0.4f,
            center.x - halfW, center.y - halfH * 0.3f,
            center.x, center.y - halfH
        )
        path.close()

        // Luminous glowing rim when active
        if (isGlowing) {
            drawPath(
                path = path,
                color = Color(0xFFFFE082).copy(alpha = 0.45f),
                style = Stroke(width = 2.5f)
            )
            drawPath(
                path = path,
                color = Color(0xFFFFF9E6).copy(alpha = 0.30f),
                style = Stroke(width = 4.5f)
            )
        }

        drawPath(path = path, color = color)
    }
}

private val leafPalette = listOf(
    Color(0xFFE5A93B), // Warm Amber
    Color(0xFFFFD580), // Soft Golden Light
    Color(0xFF8BA870), // Zen Moss Green
    Color(0xFF2EA8A4), // Teal Glaze
    Color(0xFFD4AF37)  // Antique Gold
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
            sizePx = Random.nextFloat() * 14f + 14f,
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
            sizePx = Random.nextFloat() * 16f + 12f,
            alpha = Random.nextFloat() * 0.5f + 0.5f,
            color = leafPalette.random(),
            isBurst = true
        )
    }
}
