package com.samadhi.meditationtimer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenThemeId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private fun easeInOutCubic(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return if (t < 0.5f) {
        4f * t * t * t
    } else {
        1f - (-2f * t + 2f).let { it * it * it } / 2f
    }
}

private data class BotanicalElement(
    val id: Int,
    val angle: Float,
    val homeRadiusRatio: Float,
    val expandRadiusRatio: Float,
    val scale: Float,
    val rotationOffset: Float,
    val flutterSpeed: Float,
    val colorIndex: Int,
    val tier: Int // 0 = core, 1 = mid, 2 = outer
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current

    // Generate structured botanical elements designed for each specific plant species
    val botanicalElements = remember(currentTheme.id) {
        val count = when (currentTheme.id) {
            ZenThemeId.BAMBOO_TEAL -> 32 // 32 authentic bamboo sprigs with multi-leaf branches
            ZenThemeId.SACRED_LOTUS -> 28 // 28 layered lotus petals and blossom elements
            ZenThemeId.SAKURA_TWILIGHT -> 36 // 36 cherry blossom flowers & drifting petals
        }

        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat()

        List(count) { i ->
            val tier = when {
                i < count / 3 -> 0 // Inner
                i < (count * 2) / 3 -> 1 // Mid
                else -> 2 // Outer
            }

            val angle = i * goldenAngle + (Random.nextFloat() - 0.5f) * 0.12f
            val homeR = 0.08f + tier * 0.12f + Random.nextFloat() * 0.06f
            val expandR = 0.28f + tier * 0.26f + Random.nextFloat() * 0.14f

            val baseScale = when (tier) {
                0 -> Random.nextFloat() * 0.25f + 0.85f
                1 -> Random.nextFloat() * 0.35f + 1.10f
                else -> Random.nextFloat() * 0.40f + 1.35f
            }

            BotanicalElement(
                id = i,
                angle = angle,
                homeRadiusRatio = homeR,
                expandRadiusRatio = expandR,
                scale = baseScale,
                rotationOffset = (Random.nextFloat() - 0.5f) * 45f,
                flutterSpeed = Random.nextFloat() * 1.4f + 0.8f,
                colorIndex = i % currentTheme.particlePalette.size,
                tier = tier
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "botanicalBreathingClock")
    val animClockSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animClockSec"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val easedProgress = easeInOutCubic(phaseProgress)
    val (expansionFactor, phaseGlowColor, glowPulseIntensity) = when (currentPhase) {
        BreathPhase.INHALE -> {
            val expansion = 0.24f + (0.76f * easedProgress)
            val glow = 0.35f + (0.65f * easedProgress)
            Triple(expansion, currentTheme.primaryLight, glow)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = sin(phaseProgress * PI.toFloat()) * 0.030f
            val glowPulse = 1.0f + sin(phaseProgress * PI.toFloat()) * 0.22f
            Triple(1.0f + suspensionPulse, currentTheme.primaryLight, glowPulse)
        }
        BreathPhase.EXHALE -> {
            val contraction = 1.0f - (0.76f * easedProgress)
            val glow = 1.0f - (0.65f * easedProgress)
            Triple(contraction, currentTheme.overrunColor, glow)
        }
        BreathPhase.HOLD_EMPTY -> {
            val quietPulse = sin(phaseProgress * PI.toFloat()) * 0.020f
            val quietGlow = 0.35f + sin(phaseProgress * PI.toFloat()) * 0.12f
            Triple(0.24f + quietPulse, currentTheme.primary, quietGlow)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)
        val maxAvailableRadius = (width.coerceAtMost(height) * 0.42f)

        // 1. Serene Diffuse Atmosphere Aura
        val coreAuraRadius = maxAvailableRadius * (0.40f + 0.60f * expansionFactor) * corePulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseGlowColor.copy(alpha = (0.28f * glowPulseIntensity).coerceIn(0.08f, 0.45f)),
                    currentTheme.primary.copy(alpha = (0.12f * glowPulseIntensity).coerceIn(0.03f, 0.22f)),
                    Color.Transparent
                ),
                center = center,
                radius = coreAuraRadius * 1.35f
            ),
            radius = coreAuraRadius * 1.35f,
            center = center
        )

        // 2. Translucent Breath Expansion Guideline
        drawCircle(
            color = phaseGlowColor.copy(alpha = (0.15f * glowPulseIntensity).coerceIn(0.05f, 0.28f)),
            radius = maxAvailableRadius * expansionFactor,
            center = center,
            style = Stroke(width = 1.6f)
        )

        // 3. Render Authentic Species-Specific Botanical Elements
        val palette = currentTheme.particlePalette

        for (elem in botanicalElements) {
            val radius = maxAvailableRadius * (elem.homeRadiusRatio + (elem.expandRadiusRatio - elem.homeRadiusRatio) * expansionFactor)
            val orbitDrift = animClockSec * (0.015f * (1.0f - elem.homeRadiusRatio))
            val currentAngle = elem.angle + orbitDrift

            val px = center.x + cos(currentAngle) * radius
            val py = center.y + sin(currentAngle) * radius

            val baseElementAngle = Math.toDegrees(currentAngle.toDouble()).toFloat() + 90f
            val flutter = sin(animClockSec * elem.flutterSpeed + elem.id) * 6.0f
            val totalRotation = baseElementAngle + elem.rotationOffset + flutter

            val growthScale = (0.55f + 0.45f * expansionFactor) * elem.scale
            val elementColor = palette[elem.colorIndex]

            when (currentTheme.id) {
                ZenThemeId.BAMBOO_TEAL -> {
                    // 🎋 Draw Authentic Bamboo Branch Sprig (Stem + 3-4 pointed bamboo blades)
                    drawBambooBranchSprig(
                        center = Offset(px, py),
                        scale = growthScale,
                        rotation = totalRotation,
                        stemColor = currentTheme.primaryDark,
                        leafColor = elementColor,
                        highlightColor = phaseGlowColor,
                        glowIntensity = glowPulseIntensity * (0.85f + 0.15f * expansionFactor)
                    )
                }
                ZenThemeId.SACRED_LOTUS -> {
                    // 🪷 Draw Authentic Sacred Lotus Blossom Petal with golden veins
                    drawLotusBlossomPetal(
                        center = Offset(px, py),
                        scale = growthScale,
                        rotation = totalRotation,
                        petalColor = elementColor,
                        highlightColor = phaseGlowColor,
                        glowIntensity = glowPulseIntensity * (0.85f + 0.15f * expansionFactor),
                        isHero = elem.tier == 1
                    )
                }
                ZenThemeId.SAKURA_TWILIGHT -> {
                    // 🌸 Draw Authentic 5-Petal Cherry Blossom Flower or Notched Petal
                    if (elem.id % 3 == 0) {
                        drawSakuraBlossomFloret(
                            center = Offset(px, py),
                            scale = growthScale * 0.9f,
                            rotation = totalRotation,
                            petalColor = elementColor,
                            centerColor = Color(0xFFFFD1DC),
                            highlightColor = phaseGlowColor,
                            glowIntensity = glowPulseIntensity
                        )
                    } else {
                        drawSakuraPetalBlade(
                            center = Offset(px, py),
                            scale = growthScale * 1.1f,
                            rotation = totalRotation,
                            petalColor = elementColor,
                            highlightColor = phaseGlowColor,
                            glowIntensity = glowPulseIntensity
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 🎋 1. AUTHENTIC BAMBOO BRANCH SPRIG
// ==========================================
private fun DrawScope.drawBambooBranchSprig(
    center: Offset,
    scale: Float,
    rotation: Float,
    stemColor: Color,
    leafColor: Color,
    highlightColor: Color,
    glowIntensity: Float
) {
    rotate(degrees = rotation, pivot = center) {
        val s = scale * 18.0f // Size factor

        // 1. Bamboo Joint Stem
        val stemPath = Path().apply {
            moveTo(center.x, center.y + s * 1.2f)
            lineTo(center.x, center.y - s * 0.6f)
        }
        drawPath(
            path = stemPath,
            color = stemColor.copy(alpha = 0.85f),
            style = Stroke(width = s * 0.14f, cap = StrokeCap.Round)
        )
        // Bamboo Stem Node Joint
        drawCircle(
            color = highlightColor.copy(alpha = 0.90f),
            radius = s * 0.16f,
            center = Offset(center.x, center.y + s * 0.3f)
        )

        // 2. Bamboo Leaves (Central Blade + Left Blade + Right Blade)
        val leafBlades = listOf(
            Triple(0f, -s * 0.6f, 0f),       // Center top leaf
            Triple(-s * 0.35f, -s * 0.2f, -32f), // Left side leaf
            Triple(s * 0.35f, -s * 0.1f, 34f)    // Right side leaf
        )

        val leafPath = Path()
        val veinPath = Path()

        for ((lx, ly, lRot) in leafBlades) {
            rotate(degrees = lRot, pivot = Offset(center.x + lx, center.y + ly)) {
                val ox = center.x + lx
                val oy = center.y + ly
                val lLen = s * 1.6f
                val lWid = s * 0.38f

                leafPath.reset()
                veinPath.reset()

                // Long tapered lanceolate bamboo leaf
                leafPath.moveTo(ox, oy - lLen) // Sharp tip
                leafPath.cubicTo(
                    ox + lWid * 1.1f, oy - lLen * 0.55f,
                    ox + lWid * 0.8f, oy + lLen * 0.15f,
                    ox, oy // Stalk attach
                )
                leafPath.cubicTo(
                    ox - lWid * 0.8f, oy + lLen * 0.15f,
                    ox - lWid * 1.1f, oy - lLen * 0.55f,
                    ox, oy - lLen
                )
                leafPath.close()

                veinPath.moveTo(ox, oy - lLen * 0.92f)
                veinPath.lineTo(ox, oy)

                // Soft Outer Glow
                drawPath(
                    path = leafPath,
                    color = highlightColor.copy(alpha = (0.24f * glowIntensity).coerceIn(0.08f, 0.45f)),
                    style = Stroke(width = s * 0.25f, cap = StrokeCap.Round)
                )

                // Solid Leaf Body Fill
                drawPath(
                    path = leafPath,
                    color = leafColor.copy(alpha = 0.94f)
                )

                // Crisp Rim
                drawPath(
                    path = leafPath,
                    color = highlightColor.copy(alpha = 0.85f),
                    style = Stroke(width = 1.2f)
                )

                // Bamboo Center Rib
                drawPath(
                    path = veinPath,
                    color = highlightColor.copy(alpha = 0.75f),
                    style = Stroke(width = 1.0f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

// ==========================================
// 🪷 2. AUTHENTIC SACRED LOTUS BLOSSOM PETAL
// ==========================================
private fun DrawScope.drawLotusBlossomPetal(
    center: Offset,
    scale: Float,
    rotation: Float,
    petalColor: Color,
    highlightColor: Color,
    glowIntensity: Float,
    isHero: Boolean
) {
    rotate(degrees = rotation, pivot = center) {
        val s = scale * 22.0f
        val pLen = s * 1.6f
        val pWid = s * 0.65f

        val petalPath = Path().apply {
            moveTo(center.x, center.y - pLen) // Lotus pointed apex
            cubicTo(
                center.x + pWid * 1.25f, center.y - pLen * 0.55f,
                center.x + pWid * 1.10f, center.y + pLen * 0.20f,
                center.x, center.y + pLen * 0.45f // Base
            )
            cubicTo(
                center.x - pWid * 1.10f, center.y + pLen * 0.20f,
                center.x - pWid * 1.25f, center.y - pLen * 0.55f,
                center.x, center.y - pLen
            )
            close()
        }

        val ribsPath = Path().apply {
            // Main center vein
            moveTo(center.x, center.y - pLen * 0.90f)
            lineTo(center.x, center.y + pLen * 0.35f)

            // Radiating side ribs
            moveTo(center.x - pWid * 0.55f, center.y - pLen * 0.25f)
            quadraticTo(center.x, center.y + pLen * 0.10f, center.x + pWid * 0.55f, center.y - pLen * 0.25f)
        }

        // 1. Golden Aura Glow
        drawPath(
            path = petalPath,
            color = highlightColor.copy(alpha = (0.28f * glowIntensity).coerceIn(0.10f, 0.50f)),
            style = Stroke(width = s * 0.32f, cap = StrokeCap.Round)
        )

        // 2. Rich Petal Fill
        drawPath(
            path = petalPath,
            color = petalColor.copy(alpha = 0.95f)
        )

        // 3. Subtle Inner Petal Gradient Tint
        drawCircle(
            color = Color(0xFFFFF9C4).copy(alpha = 0.40f),
            radius = s * 0.45f,
            center = Offset(center.x, center.y - pLen * 0.35f)
        )

        // 4. Crisp Golden Rim
        drawPath(
            path = petalPath,
            color = highlightColor.copy(alpha = 0.90f),
            style = Stroke(width = 1.3f)
        )

        // 5. Veins
        drawPath(
            path = ribsPath,
            color = highlightColor.copy(alpha = 0.80f),
            style = Stroke(width = 1.1f, cap = StrokeCap.Round)
        )
    }
}

// ==========================================
// 🌸 3. AUTHENTIC SAKURA BLOSSOM FLORET & PETAL
// ==========================================
private fun DrawScope.drawSakuraBlossomFloret(
    center: Offset,
    scale: Float,
    rotation: Float,
    petalColor: Color,
    centerColor: Color,
    highlightColor: Color,
    glowIntensity: Float
) {
    rotate(degrees = rotation, pivot = center) {
        val s = scale * 15.0f
        val petalPath = Path()

        // 5-Petal Cherry Blossom Ring
        for (i in 0..4) {
            val petalAngle = i * 72f
            rotate(degrees = petalAngle, pivot = center) {
                val pLen = s * 1.1f
                val pWid = s * 0.45f
                val notch = pLen * 0.22f

                petalPath.reset()
                petalPath.moveTo(center.x, center.y)
                petalPath.cubicTo(
                    center.x - pWid * 1.1f, center.y - pLen * 0.45f,
                    center.x - pWid * 0.8f, center.y - pLen,
                    center.x - pWid * 0.45f, center.y - pLen
                )
                // Center notch cleft
                petalPath.quadraticTo(
                    center.x, center.y - pLen + notch,
                    center.x + pWid * 0.45f, center.y - pLen
                )
                petalPath.cubicTo(
                    center.x + pWid * 0.8f, center.y - pLen,
                    center.x + pWid * 1.1f, center.y - pLen * 0.45f,
                    center.x, center.y
                )
                petalPath.close()

                // Petal fill
                drawPath(
                    path = petalPath,
                    color = petalColor.copy(alpha = 0.92f)
                )

                // Petal rim
                drawPath(
                    path = petalPath,
                    color = highlightColor.copy(alpha = 0.80f),
                    style = Stroke(width = 1.0f)
                )
            }
        }

        // Central Pistil / Stamen Pistils
        drawCircle(
            color = centerColor.copy(alpha = 0.95f),
            radius = s * 0.28f,
            center = center
        )
        drawCircle(
            color = Color(0xFFFF4081).copy(alpha = 0.90f),
            radius = s * 0.14f,
            center = center
        )
    }
}

private fun DrawScope.drawSakuraPetalBlade(
    center: Offset,
    scale: Float,
    rotation: Float,
    petalColor: Color,
    highlightColor: Color,
    glowIntensity: Float
) {
    rotate(degrees = rotation, pivot = center) {
        val s = scale * 18.0f
        val pLen = s * 1.5f
        val pWid = s * 0.55f
        val notch = pLen * 0.20f

        val path = Path().apply {
            moveTo(center.x, center.y + pLen * 0.4f)
            cubicTo(
                center.x - pWid * 1.15f, center.y + pLen * 0.1f,
                center.x - pWid * 1.10f, center.y - pLen * 0.6f,
                center.x - pWid * 0.50f, center.y - pLen
            )
            // Heart notch
            quadraticTo(
                center.x, center.y - pLen + notch,
                center.x + pWid * 0.50f, center.y - pLen
            )
            cubicTo(
                center.x + pWid * 1.10f, center.y - pLen * 0.6f,
                center.x + pWid * 1.15f, center.y + pLen * 0.1f,
                center.x, center.y + pLen * 0.4f
            )
            close()
        }

        // Aura
        drawPath(
            path = path,
            color = highlightColor.copy(alpha = (0.24f * glowIntensity).coerceIn(0.08f, 0.45f)),
            style = Stroke(width = s * 0.24f, cap = StrokeCap.Round)
        )

        // Body
        drawPath(
            path = path,
            color = petalColor.copy(alpha = 0.94f)
        )

        // Outline
        drawPath(
            path = path,
            color = highlightColor.copy(alpha = 0.85f),
            style = Stroke(width = 1.1f)
        )
    }
}
