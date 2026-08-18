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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType
import com.samadhi.meditationtimer.ui.theme.ZenThemeId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
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

private data class BreathPlantEntity(
    val index: Int,
    val angle: Float,
    val homeRadiusRatio: Float,
    val expandRadiusRatio: Float,
    val size: Float,
    val baseGlow: Float,
    val opacity: Float,
    val speed: Float,
    val wobble: Float,
    val flutterSpeed: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier,
    leafCount: Int = 220
) {
    val currentTheme = LocalZenTheme.current

    // Generate 220 breathing botanical entities distributed in an organic phyllotaxis spiral
    val plantEntities = remember(leafCount, currentTheme.id) {
        val palette = currentTheme.particlePalette
        val goldenAngle = (PI * (3.0 - sqrt(5.0))).toFloat() // ~137.5 deg phyllotaxis

        List(leafCount) { i ->
            val angle = i * goldenAngle + (Random.nextFloat() - 0.5f) * 0.18f
            val col = palette[i % palette.size]

            val homeR = (Random.nextFloat().pow(0.5f)) * 0.14f
            val expandR = (0.22f + (Random.nextFloat().pow(0.55f)) * 0.78f)

            // Varied sizes with hero blooming elements
            val size = when {
                i % 10 == 0 -> Random.nextFloat() * 6.0f + 25.0f // Hero plant elements
                i % 3 == 0 -> Random.nextFloat() * 5.0f + 19.0f
                else -> Random.nextFloat() * 4.0f + 15.0f
            }

            val glow = Random.nextFloat() * 3.0f + 6.0f
            val op = (Random.nextFloat() * 0.20f + 0.80f).coerceIn(0.75f, 1.0f)

            BreathPlantEntity(
                index = i,
                angle = angle,
                homeRadiusRatio = homeR,
                expandRadiusRatio = expandR,
                size = size,
                baseGlow = glow,
                opacity = op,
                speed = Random.nextFloat() * 0.08f + 0.05f,
                wobble = Random.nextFloat() * 2.2f + 1.2f,
                flutterSpeed = Random.nextFloat() * 1.5f + 1.2f,
                phase = Random.nextFloat() * 6.28318f,
                color = col
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "plantBreathingClock")
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
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val easedProgress = easeInOutCubic(phaseProgress)
    val (expansionFactor, phaseGlowColor, glowPulseIntensity) = when (currentPhase) {
        BreathPhase.INHALE -> {
            val expansion = 0.22f + (0.78f * easedProgress)
            val glow = 0.35f + (0.65f * easedProgress)
            Triple(expansion, currentTheme.primaryLight, glow)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = sin(phaseProgress * PI.toFloat()) * 0.030f
            val glowPulse = 1.0f + sin(phaseProgress * PI.toFloat()) * 0.22f
            Triple(1.0f + suspensionPulse, currentTheme.primaryLight, glowPulse)
        }
        BreathPhase.EXHALE -> {
            val contraction = 1.0f - (0.78f * easedProgress)
            val glow = 1.0f - (0.65f * easedProgress)
            Triple(contraction, currentTheme.overrunColor, glow)
        }
        BreathPhase.HOLD_EMPTY -> {
            val quietPulse = sin(phaseProgress * PI.toFloat()) * 0.020f
            val quietGlow = 0.35f + sin(phaseProgress * PI.toFloat()) * 0.12f
            Triple(0.22f + quietPulse, currentTheme.primary, quietGlow)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)
        val maxAvailableRadius = (width.coerceAtMost(height) * 0.44f)

        // 1. Serene Diffuse Atmosphere Aura (Pure Organic Ambient Glow)
        val coreAuraRadius = maxAvailableRadius * (0.35f + 0.65f * expansionFactor) * corePulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    phaseGlowColor.copy(alpha = (0.28f * glowPulseIntensity).coerceIn(0.08f, 0.42f)),
                    currentTheme.primary.copy(alpha = (0.14f * glowPulseIntensity).coerceIn(0.03f, 0.22f)),
                    Color.Transparent
                ),
                center = center,
                radius = coreAuraRadius * 1.35f
            ),
            radius = coreAuraRadius * 1.35f,
            center = center
        )

        // 2. Translucent Breath Boundary Ring (Expands/contracts with breath cycle)
        val ringRadius = maxAvailableRadius * expansionFactor
        drawCircle(
            color = phaseGlowColor.copy(alpha = (0.16f * glowPulseIntensity).coerceIn(0.05f, 0.30f)),
            radius = ringRadius,
            center = center,
            style = Stroke(width = 1.8f)
        )

        // 3. Render Theme Botanical Plants (Bamboo Leaves, Lotus Petals, Sakura Blossoms)
        val path = Path()
        val spinePath = Path()

        for (plant in plantEntities) {
            val entityProgress = (expansionFactor + sin(animClockSec * plant.speed + plant.phase) * 0.025f).coerceIn(0.16f, 1.05f)
            val currentRadius = maxAvailableRadius * (plant.homeRadiusRatio + (plant.expandRadiusRatio - plant.homeRadiusRatio) * entityProgress)

            val orbitDrift = animClockSec * (0.018f * (1.0f - plant.homeRadiusRatio))
            val currentAngle = plant.angle + orbitDrift + (sin(animClockSec * plant.flutterSpeed + plant.phase) * 0.045f)

            val px = center.x + cos(currentAngle) * currentRadius
            val py = center.y + sin(currentAngle) * currentRadius

            // Dynamic rotation pointing organically outward with gentle breathing tilt
            val outwardAngleDeg = Math.toDegrees(currentAngle.toDouble()).toFloat() + 90f
            val naturalFlutter = sin(animClockSec * plant.flutterSpeed + plant.phase) * plant.wobble * 4.5f
            val plantRotation = outwardAngleDeg + naturalFlutter

            val growthScale = (0.45f + 0.55f * expansionFactor) * (1.0f + 0.06f * sin(animClockSec * 2.0f + plant.phase))
            val entitySize = plant.size * growthScale

            val entityAlpha = (plant.opacity * (0.75f + 0.25f * expansionFactor)).coerceIn(0.55f, 1.0f)
            val glowIntensity = (plant.baseGlow / 7.0f) * glowPulseIntensity

            drawThemeBotanicalElement(
                themeId = currentTheme.id,
                particleType = currentTheme.particleType,
                center = Offset(px, py),
                size = entitySize,
                rotation = plantRotation,
                color = plant.color,
                highlightColor = phaseGlowColor,
                expansionFactor = expansionFactor,
                glowIntensity = glowIntensity,
                dynamicAlpha = entityAlpha,
                path = path,
                spinePath = spinePath
            )
        }
    }
}

private fun DrawScope.drawThemeBotanicalElement(
    themeId: ZenThemeId,
    particleType: ParticleType,
    center: Offset,
    size: Float,
    rotation: Float,
    color: Color,
    highlightColor: Color,
    expansionFactor: Float,
    glowIntensity: Float,
    dynamicAlpha: Float,
    path: Path,
    spinePath: Path
) {
    rotate(degrees = rotation, pivot = center) {
        val halfW = size * 0.38f
        val halfH = size * 0.75f
        path.reset()
        spinePath.reset()

        when (themeId) {
            ZenThemeId.BAMBOO_TEAL -> {
                // 🎋 Moonlit Bamboo: Slender, pointed lanceolate bamboo blade with node stalk & parallel venation
                val bWidth = size * 0.24f
                path.moveTo(center.x, center.y - halfH * 1.05f) // Sharp bamboo apex tip
                path.cubicTo(
                    center.x + bWidth * 1.25f, center.y - halfH * 0.40f,
                    center.x + bWidth * 0.90f, center.y + halfH * 0.45f,
                    center.x, center.y + halfH * 0.95f // Stalk base
                )
                path.cubicTo(
                    center.x - bWidth * 0.90f, center.y + halfH * 0.45f,
                    center.x - bWidth * 1.25f, center.y - halfH * 0.40f,
                    center.x, center.y - halfH * 1.05f
                )
                path.close()

                // Bamboo main spine vein + side notch
                spinePath.moveTo(center.x, center.y - halfH * 0.95f)
                spinePath.lineTo(center.x, center.y + halfH * 0.85f)

                // Bamboo culm joint sheath notch
                spinePath.moveTo(center.x - bWidth * 0.45f, center.y + halfH * 0.55f)
                spinePath.lineTo(center.x + bWidth * 0.45f, center.y + halfH * 0.55f)
            }
            ZenThemeId.SACRED_LOTUS -> {
                // 🪷 Golden Lotus: Broad, gracefully cupped sacred lotus water lily petal
                val lWidth = size * 0.46f
                path.moveTo(center.x, center.y - halfH * 1.02f) // Pointed petal tip
                path.cubicTo(
                    center.x + lWidth * 1.25f, center.y - halfH * 0.30f,
                    center.x + lWidth * 1.10f, center.y + halfH * 0.50f,
                    center.x, center.y + halfH * 0.90f // Cupped base
                )
                path.cubicTo(
                    center.x - lWidth * 1.10f, center.y + halfH * 0.50f,
                    center.x - lWidth * 1.25f, center.y - halfH * 0.30f,
                    center.x, center.y - halfH * 1.02f
                )
                path.close()

                // Lotus central spine + gentle radiating rib curves
                spinePath.moveTo(center.x, center.y - halfH * 0.85f)
                spinePath.lineTo(center.x, center.y + halfH * 0.75f)

                spinePath.moveTo(center.x - lWidth * 0.35f, center.y - halfH * 0.20f)
                spinePath.quadraticTo(center.x, center.y + halfH * 0.30f, center.x + lWidth * 0.35f, center.y - halfH * 0.20f)
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                // 🌸 Sakura Twilight: Iconic notched cherry blossom petal with heart-cleft apex
                val sWidth = size * 0.42f
                val topY = center.y - halfH * 0.95f
                val notchDepth = halfH * 0.22f

                path.moveTo(center.x, center.y + halfH * 0.90f) // Petal base
                path.cubicTo(
                    center.x - sWidth * 1.15f, center.y + halfH * 0.40f,
                    center.x - sWidth * 1.05f, center.y - halfH * 0.40f,
                    center.x - sWidth * 0.52f, topY // Left lobe
                )
                // Center heart notch cleft
                path.quadraticTo(
                    center.x, topY + notchDepth,
                    center.x + sWidth * 0.52f, topY // Right lobe
                )
                path.cubicTo(
                    center.x + sWidth * 1.05f, center.y - halfH * 0.40f,
                    center.x + sWidth * 1.15f, center.y + halfH * 0.40f,
                    center.x, center.y + halfH * 0.90f
                )
                path.close()

                // Sakura delicate stamen vein
                spinePath.moveTo(center.x, topY + notchDepth * 1.3f)
                spinePath.lineTo(center.x, center.y + halfH * 0.75f)
            }
        }

        // Pass 1: Luminous Outer Aura (Soft organic framing)
        val outerGlowAlpha = (0.22f * glowIntensity * dynamicAlpha).coerceIn(0f, 0.40f)
        val outerGlowStroke = 8.0f * (0.75f + 0.40f * glowIntensity * expansionFactor)
        drawPath(
            path = path,
            color = Color.White.copy(alpha = outerGlowAlpha),
            style = Stroke(width = outerGlowStroke, cap = StrokeCap.Round)
        )

        // Pass 2: Vibrant Theme Colored Radiant Bloom
        val bloomAlpha = (0.52f * glowIntensity * dynamicAlpha).coerceIn(0.18f, 0.75f)
        val bloomStroke = 3.8f * (0.8f + 0.35f * glowIntensity)
        drawPath(
            path = path,
            color = highlightColor.copy(alpha = bloomAlpha),
            style = Stroke(width = bloomStroke, cap = StrokeCap.Round)
        )

        // Pass 3: Solid Rich Botanical Body Fill (100% Theme Pigment)
        drawPath(
            path = path,
            color = color.copy(alpha = (0.92f + 0.08f * expansionFactor).coerceIn(0.85f, 1.0f))
        )

        // Pass 4: Crisp Theme Highlight Outer Edge Outline
        val rimAlpha = (0.90f * dynamicAlpha).coerceIn(0.6f, 1.0f)
        drawPath(
            path = path,
            color = highlightColor.copy(alpha = rimAlpha),
            style = Stroke(width = 1.3f)
        )

        // Pass 5: Luminous Center Spine Vein
        drawPath(
            path = spinePath,
            color = highlightColor.copy(alpha = (rimAlpha * 0.90f)),
            style = Stroke(width = 1.1f, cap = StrokeCap.Round)
        )
    }
}
