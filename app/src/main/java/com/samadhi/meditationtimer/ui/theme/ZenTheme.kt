package com.samadhi.meditationtimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.samadhi.meditationtimer.R

enum class ParticleType {
    BAMBOO_LEAF,
    BODHI_LEAF,
    SAKURA_PETAL
}

enum class ZenThemeId(val id: String, val title: String, val subtitle: String) {
    GOLDEN_BODHI("golden_bodhi", "Sacred Bodhi", "Dynamic Material You theme with floating sacred bodhi leaves"),
    BAMBOO_TEAL("bamboo_teal", "Sacred Bodhi", "Dynamic Material You theme with floating sacred bodhi leaves"),
    SAKURA_TWILIGHT("sakura_twilight", "Sacred Bodhi", "Dynamic Material You theme with floating sacred bodhi leaves"),
    SYSTEM_DYNAMIC("system_dynamic", "Sacred Bodhi", "Dynamic Material You theme with floating sacred bodhi leaves")
}

data class ZenTheme(
    val id: ZenThemeId = ZenThemeId.GOLDEN_BODHI,
    val title: String = "Sacred Bodhi",
    val backgroundDrawableRes: Int = R.drawable.bg_zen_lotus,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val primaryGlow: Color,
    val secondary: Color = Color(0xFFD4AF37),
    val surface: Color,
    val surfaceVariant: Color,
    val background: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val overrunColor: Color,
    val particleType: ParticleType = ParticleType.BODHI_LEAF,
    val particlePalette: List<Color>
)

val GoldenBodhiTheme = ZenTheme(
    id = ZenThemeId.GOLDEN_BODHI,
    title = "Sacred Bodhi",
    backgroundDrawableRes = R.drawable.bg_zen_lotus,
    primary = Color(0xFFD4AF37),
    primaryLight = Color(0xFFD4AF37),
    primaryDark = Color(0xFF8D6E1C),
    primaryGlow = Color(0x35D4AF37),
    secondary = Color(0xFFD4AF37),
    surface = Color(0xFF201F1B),
    surfaceVariant = Color(0xFF2B2A25),
    background = Color(0xFF141311),
    border = Color(0xFF4C4639),
    textPrimary = Color(0xFFE6E2DC),
    textSecondary = Color(0xFFCEC5B4),
    textMuted = Color(0xFF978F80),
    overrunColor = Color(0xFF60A5FA),
    particleType = ParticleType.BODHI_LEAF,
    particlePalette = listOf(
        Color(0xFFFFE082),
        Color(0xFFFFD54F),
        Color(0xFFD4AF37),
        Color(0xFFFFF8E1),
        Color(0xFFFFECB3),
        Color(0xFFC8A261)
    )
)

val BambooTealTheme = GoldenBodhiTheme
val SakuraTwilightTheme = GoldenBodhiTheme
val SacredLotusTheme = GoldenBodhiTheme
val SystemDynamicTheme = GoldenBodhiTheme

val LocalZenTheme = compositionLocalOf { GoldenBodhiTheme }

@Composable
fun rememberZenTheme(): ZenTheme {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    return ZenTheme(
        id = ZenThemeId.GOLDEN_BODHI,
        title = "Sacred Bodhi",
        backgroundDrawableRes = R.drawable.bg_zen_lotus,
        primary = colorScheme.primary,
        primaryLight = colorScheme.primary,
        primaryDark = if (isDark) colorScheme.primaryContainer else colorScheme.onPrimaryContainer,
        primaryGlow = colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.15f),
        secondary = colorScheme.secondary,
        surface = colorScheme.surfaceContainer,
        surfaceVariant = colorScheme.surfaceContainerHigh,
        background = colorScheme.background,
        border = colorScheme.outlineVariant,
        textPrimary = colorScheme.onBackground,
        textSecondary = colorScheme.onSurfaceVariant,
        textMuted = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        overrunColor = colorScheme.tertiary,
        particleType = ParticleType.BODHI_LEAF,
        particlePalette = if (isDark) {
            listOf(
                colorScheme.primary,
                Color(0xFFFFE082),
                Color(0xFFFFD54F),
                Color(0xFFD4AF37),
                Color(0xFFE5A93B),
                colorScheme.secondary
            )
        } else {
            listOf(
                colorScheme.primary,
                Color(0xFFC68A1E),
                Color(0xFFB87814),
                Color(0xFF9E630A),
                Color(0xFF8A5506),
                colorScheme.secondary
            )
        }
    )
}

fun getZenThemeById(id: String?): ZenTheme = GoldenBodhiTheme
