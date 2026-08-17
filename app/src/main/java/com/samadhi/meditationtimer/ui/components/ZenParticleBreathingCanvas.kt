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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.samadhi.meditationtimer.domain.model.BreathPhase
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ParticleType
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

private data class BambooStreamParticle(
    val stalkIndex: Int,
    val initialT: Float,
    val flowSpeed: Float,
    val size: Float,
    val lateralOffset: Float,
    val phase: Float,
    val color: Color
)

private data class BambooLeafParticle(
    val stalkIndex: Int,
    val nodeLevel: Int,
    val leafIndex: Int,
    val baseAngle: Float,
    val distance: Float,
    val size: Float,
    val flutterSpeed: Float,
    val phase: Float,
    val color: Color
)

private data class LotusMandalaParticle(
    val ringIndex: Int,
    val petalIndex: Int,
    val tOnPetal: Float,
    val size: Float,
    val dispersionAngle: Float,
    val dispersionDistance: Float,
    val dispersionSpeed: Float,
    val phase: Float,
    val color: Color
)

private data class SakuraVortexParticle(
    val index: Int,
    val armIndex: Int,
    val radiusRatio: Float,
    val size: Float,
    val orbitSpeed: Float,
    val spinSpeed: Float,
    val phase: Float,
    val isPetalBlade: Boolean,
    val color: Color
)

@Composable
fun ZenParticleBreathingCanvas(
    currentPhase: BreathPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier,
    particleCount: Int = 160
) {
    val currentTheme = LocalZenTheme.current

    val bambooStreamParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(64) { i ->
            BambooStreamParticle(
                stalkIndex = i % 3,
                initialT = (i.toFloat() / 64f),
                flowSpeed = Random.nextFloat() * 0.12f + 0.18f,
                size = Random.nextFloat() * 3.5f + 2.0f,
                lateralOffset = (Random.nextFloat() - 0.5f) * 8f,
                phase = Random.nextFloat() * 6.28f,
                color = palette[i % palette.size]
            )
        }
    }

    val bambooLeafParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(76) { i ->
            val stalk = i % 3
            val node = (i / 3) % 4 + 1
            val side = if (i % 2 == 0) 1f else -1f
            BambooLeafParticle(
                stalkIndex = stalk,
                nodeLevel = node,
                leafIndex = i,
                baseAngle = (if (side > 0) -30f else -150f) + (Random.nextFloat() - 0.5f) * 35f,
                distance = Random.nextFloat() * 26f + 12f,
                size = Random.nextFloat() * 7f + 10f,
                flutterSpeed = Random.nextFloat() * 1.6f + 1.2f,
                phase = Random.nextFloat() * 6.28f,
                color = palette[i % palette.size]
            )
        }
    }

    val lotusMandalaParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(180) { i ->
            val ring = when {
                i < 40 -> 0
                i < 100 -> 1
                else -> 2
            }
            val petalsInRing = if (ring == 0) 6 else if (ring == 1) 8 else 12
            val petal = i % petalsInRing
            val t = ((i / petalsInRing) % 6).toFloat() / 5f
            LotusMandalaParticle(
                ringIndex = ring,
                petalIndex = petal,
                tOnPetal = t,
                size = if (ring == 0) Random.nextFloat() * 3.5f + 2.5f else Random.nextFloat() * 4.5f + 3.0f,
                dispersionAngle = Random.nextFloat() * 6.28318f,
                dispersionDistance = Random.nextFloat() * 0.85f + 0.20f,
                dispersionSpeed = Random.nextFloat() * 0.8f + 0.6f,
                phase = Random.nextFloat() * 6.28318f,
                color = palette[i % palette.size]
            )
        }
    }

    val sakuraVortexParticles = remember(currentTheme.id) {
        val palette = currentTheme.particlePalette
        List(160) { i ->
            val arm = i % 5
            val rRatio = sqrt(i.toFloat() / 160f)
            SakuraVortexParticle(
                index = i,
                armIndex = arm,
                radiusRatio = rRatio,
                size = if (i % 3 == 0) Random.nextFloat() * 6f + 12f else Random.nextFloat() * 3.5f + 2.5f,
                orbitSpeed = Random.nextFloat() * 0.4f + 0.6f,
                spinSpeed = (Random.nextFloat() - 0.5f) * 2.2f,
                phase = Random.nextFloat() * 6.28318f,
                isPetalBlade = (i % 3 == 0),
                color = palette[i % palette.size]
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particleBreathingClock")
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
    val (expansionFactor, phaseGlowColor) = when (currentPhase) {
        BreathPhase.INHALE -> {
            val expansion = 0.24f + (0.76f * easedProgress)
            Pair(expansion, currentTheme.primaryLight)
        }
        BreathPhase.HOLD_FULL -> {
            val suspensionPulse = sin(phaseProgress * PI.toFloat()) * 0.035f
            Pair(1.0f + suspensionPulse, currentTheme.primaryLight)
        }
        BreathPhase.EXHALE -> {
            val contraction = 1.0f - (0.76f * easedProgress)
            Pair(contraction, currentTheme.overrunColor)
        }
        BreathPhase.HOLD_EMPTY -> {
            val restingPulse = sin(phaseProgress * PI.toFloat()) * 0.015f
            Pair(0.24f + restingPulse, currentTheme.primaryDark)
        }
    }

    val path1 = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * 0.86f
        if (maxRadius <= 0f) return@Canvas

        when (currentTheme.id) {
            ZenThemeId.BAMBOO_TEAL -> {
                drawBioluminescentBamboo(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    streamParticles = bambooStreamParticles,
                    leafParticles = bambooLeafParticles,
                    clockSec = animClockSec,
                    corePulse = corePulse,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    phaseGlowColor = phaseGlowColor,
                    path = path1
                )
            }
            ZenThemeId.SACRED_LOTUS -> {
                drawLotusParticleMandala(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    isExhale = (currentPhase == BreathPhase.EXHALE || currentPhase == BreathPhase.HOLD_EMPTY),
                    particles = lotusMandalaParticles,
                    clockSec = animClockSec,
                    corePulse = corePulse,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    phaseGlowColor = phaseGlowColor,
                    path = path1
                )
            }
            ZenThemeId.SAKURA_TWILIGHT -> {
                drawSakuraFibonacciVortex(
                    center = center,
                    maxRadius = maxRadius,
                    expansion = expansionFactor,
                    particles = sakuraVortexParticles,
                    clockSec = animClockSec,
                    corePulse = corePulse,
                    primaryColor = currentTheme.primary,
                    primaryLight = currentTheme.primaryLight,
                    primaryDark = currentTheme.primaryDark,
                    phaseGlowColor = phaseGlowColor,
                    path = path1
                )
            }
        }
    }
}

private fun DrawScope.drawBioluminescentBamboo(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    streamParticles: List<BambooStreamParticle>,
    leafParticles: List<BambooLeafParticle>,
    clockSec: Float,
    corePulse: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    phaseGlowColor: Color,
    path: Path
) {
    val auraRadius = maxRadius * (0.35f + 0.65f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                phaseGlowColor.copy(alpha = (0.24f * expansion).coerceIn(0f, 0.40f)),
                primaryDark.copy(alpha = (0.08f * expansion).coerceIn(0f, 0.20f)),
                Color.Transparent
            ),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center
    )

    val rootY = center.y + maxRadius * 0.72f
    val baseStalkHeight = maxRadius * (0.42f + 0.92f * expansion)
    val stalks = listOf(
        Triple(0f, 1.0f, 0.08f),
        Triple(-maxRadius * 0.28f, 0.88f, -0.16f),
        Triple(maxRadius * 0.26f, 0.82f, 0.18f)
    )

    stalks.forEachIndexed { stalkIdx, (xOff, heightRatio, curveDir) ->
        val stalkH = baseStalkHeight * heightRatio
        val sway = sin(clockSec * 1.2f + stalkIdx * 1.5f) * (4.0f + 6.0f * expansion)
        val stalkTopX = center.x + xOff + (curveDir * stalkH * 0.35f) + sway
        val stalkBaseX = center.x + xOff * 0.65f

        path.reset()
        path.moveTo(stalkBaseX, rootY)
        path.quadraticTo(
            (stalkBaseX + stalkTopX) / 2f + (curveDir * 18f),
            rootY - stalkH / 2f,
            stalkTopX,
            rootY - stalkH
        )

        drawPath(
            path = path,
            color = primaryColor.copy(alpha = (0.22f + 0.18f * expansion).coerceIn(0.1f, 0.45f)),
            style = Stroke(width = (4.5f * (0.8f + 0.3f * expansion)))
        )

        for (node in 1..4) {
            val t = node.toFloat() / 4.5f
            val currX = stalkBaseX + (stalkTopX - stalkBaseX) * t + sin(t * PI.toFloat()) * (curveDir * 18f)
            val currY = rootY - (stalkH * t)

            drawCircle(
                color = primaryLight.copy(alpha = (0.75f + 0.20f * expansion).coerceIn(0f, 1f)),
                radius = 3.5f * (0.85f + 0.25f * expansion),
                center = Offset(currX, currY)
            )
        }
    }

    streamParticles.forEach { p ->
        val (xOff, heightRatio, curveDir) = stalks[p.stalkIndex]
        val stalkH = baseStalkHeight * heightRatio
        val sway = sin(clockSec * 1.2f + p.stalkIndex * 1.5f) * (4.0f + 6.0f * expansion)
        val stalkTopX = center.x + xOff + (curveDir * stalkH * 0.35f) + sway
        val stalkBaseX = center.x + xOff * 0.65f

        val t = ((p.initialT + clockSec * p.flowSpeed) % 1.0f)
        val currX = stalkBaseX + (stalkTopX - stalkBaseX) * t + sin(t * PI.toFloat()) * (curveDir * 18f) + p.lateralOffset
        val currY = rootY - (stalkH * t)

        val alpha = sin(t * PI.toFloat()) * (0.50f + 0.45f * expansion)
        drawCircle(
            color = p.color.copy(alpha = alpha.coerceIn(0f, 0.95f)),
            radius = p.size * (0.8f + 0.3f * expansion),
            center = Offset(currX, currY)
        )
    }

    leafParticles.forEach { leaf ->
        val (xOff, heightRatio, curveDir) = stalks[leaf.stalkIndex]
        val stalkH = baseStalkHeight * heightRatio
        val sway = sin(clockSec * 1.2f + leaf.stalkIndex * 1.5f) * (4.0f + 6.0f * expansion)
        val stalkTopX = center.x + xOff + (curveDir * stalkH * 0.35f) + sway
        val stalkBaseX = center.x + xOff * 0.65f

        val t = leaf.nodeLevel.toFloat() / 4.5f
        val nodeX = stalkBaseX + (stalkTopX - stalkBaseX) * t + sin(t * PI.toFloat()) * (curveDir * 18f)
        val nodeY = rootY - (stalkH * t)

        val leafFlutter = sin(clockSec * leaf.flutterSpeed + leaf.phase) * 6f
        val currentDist = leaf.distance * (0.65f + 0.60f * expansion)
        val rad = Math.toRadians((leaf.baseAngle + leafFlutter).toDouble())
        val leafX = nodeX + cos(rad).toFloat() * currentDist
        val leafY = nodeY + sin(rad).toFloat() * currentDist

        val leafSize = leaf.size * (0.80f + 0.35f * expansion)
        val leafRot = leaf.baseAngle + leafFlutter + 90f

        rotate(degrees = leafRot, pivot = Offset(leafX, leafY)) {
            path.reset()
            val halfW = leafSize * 0.28f
            val halfH = leafSize * 0.85f
            path.moveTo(leafX, leafY - halfH)
            path.cubicTo(
                leafX + halfW * 1.15f, leafY - halfH * 0.35f,
                leafX + halfW * 0.85f, leafY + halfH * 0.35f,
                leafX, leafY + halfH
            )
            path.cubicTo(
                leafX - halfW * 0.85f, leafY + halfH * 0.35f,
                leafX - halfW * 1.15f, leafY - halfH * 0.35f,
                leafX, leafY - halfH
            )
            path.close()

            val alpha = (0.55f + 0.40f * expansion).coerceIn(0.2f, 0.95f)
            drawPath(path = path, color = leaf.color.copy(alpha = alpha))
            drawPath(path = path, color = primaryLight.copy(alpha = (alpha * 0.7f)), style = Stroke(width = 1.0f))
        }
    }
}

private fun DrawScope.drawLotusParticleMandala(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    isExhale: Boolean,
    particles: List<LotusMandalaParticle>,
    clockSec: Float,
    corePulse: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    phaseGlowColor: Color,
    path: Path
) {
    val auraRadius = maxRadius * (0.35f + 0.70f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                phaseGlowColor.copy(alpha = (0.26f * expansion).coerceIn(0f, 0.45f)),
                primaryDark.copy(alpha = (0.10f * expansion).coerceIn(0f, 0.22f)),
                Color.Transparent
            ),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center
    )

    particles.forEach { p ->
        val petalsInRing = if (p.ringIndex == 0) 6 else if (p.ringIndex == 1) 8 else 12
        val basePetalAngleDeg = (p.petalIndex.toFloat() / petalsInRing.toFloat()) * 360f + (p.ringIndex * 22.5f) + (clockSec * 2.0f)
        val ringScale = if (p.ringIndex == 0) 0.35f else if (p.ringIndex == 1) 0.65f else 0.95f

        val petalAngleRad = Math.toRadians(basePetalAngleDeg.toDouble())
        val petalBaseRadius = maxRadius * ringScale * (0.20f + 0.70f * expansion)
        val petalSweep = sin(p.tOnPetal * PI.toFloat()) * (maxRadius * 0.16f * ringScale * expansion)
        
        val structuredX = center.x + cos(petalAngleRad).toFloat() * (petalBaseRadius * p.tOnPetal) + cos(petalAngleRad + PI / 2).toFloat() * petalSweep
        val structuredY = center.y + sin(petalAngleRad).toFloat() * (petalBaseRadius * p.tOnPetal) + sin(petalAngleRad + PI / 2).toFloat() * petalSweep

        val dispersionFactor = (1.45f - expansion * 0.70f)
        val dispAngle = p.dispersionAngle + (clockSec * 0.18f * p.dispersionSpeed)
        val dispR = maxRadius * p.dispersionDistance * dispersionFactor + sin(clockSec * p.dispersionSpeed + p.phase) * 8f
        val dispersedX = center.x + cos(dispAngle).toFloat() * dispR
        val dispersedY = center.y + sin(dispAngle).toFloat() * dispR

        val disperseT = if (isExhale) (1.0f - (expansion - 0.24f) / 0.76f).coerceIn(0f, 1f) else 0f
        val finalX = structuredX + (dispersedX - structuredX) * disperseT
        val finalY = structuredY + (dispersedY - structuredY) * disperseT

        val alpha = if (isExhale) {
            (0.35f + (1f - expansion) * 0.60f).coerceIn(0.2f, 0.95f)
        } else {
            (0.40f + expansion * 0.55f).coerceIn(0.2f, 0.95f)
        }

        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = p.size * (0.85f + 0.35f * expansion),
            center = Offset(finalX, finalY)
        )
    }

    val coreR = maxRadius * 0.14f * corePulse * (0.75f + 0.35f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryLight.copy(alpha = 0.85f),
                primaryColor.copy(alpha = 0.45f),
                Color.Transparent
            ),
            center = center,
            radius = coreR
        ),
        radius = coreR,
        center = center
    )
}

private fun DrawScope.drawSakuraFibonacciVortex(
    center: Offset,
    maxRadius: Float,
    expansion: Float,
    particles: List<SakuraVortexParticle>,
    clockSec: Float,
    corePulse: Float,
    primaryColor: Color,
    primaryLight: Color,
    primaryDark: Color,
    phaseGlowColor: Color,
    path: Path
) {
    val auraRadius = maxRadius * (0.35f + 0.70f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                phaseGlowColor.copy(alpha = (0.24f * expansion).coerceIn(0f, 0.40f)),
                primaryDark.copy(alpha = (0.09f * expansion).coerceIn(0f, 0.20f)),
                Color.Transparent
            ),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center
    )

    particles.forEach { p ->
        val spiralArmAngle = (p.armIndex * 72f) + (p.radiusRatio * 180f)
        val currentAngle = Math.toRadians((spiralArmAngle + (clockSec * 16f * p.orbitSpeed)).toDouble())
        val dynamicR = maxRadius * p.radiusRatio * (0.28f + 0.72f * expansion) + sin(clockSec * 1.5f + p.phase) * 6f
        val pX = center.x + cos(currentAngle).toFloat() * dynamicR
        val pY = center.y + sin(currentAngle).toFloat() * dynamicR

        val alpha = (0.40f + 0.55f * expansion).coerceIn(0.2f, 0.95f)

        if (p.isPetalBlade) {
            val rot = Math.toDegrees(currentAngle).toFloat() + 90f + (clockSec * 25f * p.spinSpeed)
            rotate(degrees = rot, pivot = Offset(pX, pY)) {
                path.reset()
                val size = p.size * (0.85f + 0.30f * expansion)
                val halfW = size * 0.42f
                val halfH = size * 0.88f
                val topY = pY - halfH * 0.95f

                path.moveTo(pX, pY + halfH)
                path.cubicTo(
                    pX - halfW * 1.20f, pY + halfH * 0.35f,
                    pX - halfW * 1.10f, pY - halfH * 0.45f,
                    pX - halfW * 0.55f, topY
                )
                path.quadraticTo(
                    pX, topY + halfH * 0.20f,
                    pX + halfW * 0.55f, topY
                )
                path.cubicTo(
                    pX + halfW * 1.10f, pY - halfH * 0.45f,
                    pX + halfW * 1.20f, pY + halfH * 0.35f,
                    pX, pY + halfH
                )
                path.close()

                drawPath(path = path, color = p.color.copy(alpha = alpha))
                drawPath(path = path, color = primaryLight.copy(alpha = alpha * 0.7f), style = Stroke(width = 1.0f))
            }
        } else {
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * (0.85f + 0.30f * expansion),
                center = Offset(pX, pY)
            )
        }
    }

    val coreR = maxRadius * 0.12f * corePulse * (0.80f + 0.30f * expansion)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryLight.copy(alpha = 0.85f),
                primaryColor.copy(alpha = 0.40f),
                Color.Transparent
            ),
            center = center,
            radius = coreR
        ),
        radius = coreR,
        center = center
    )
}
