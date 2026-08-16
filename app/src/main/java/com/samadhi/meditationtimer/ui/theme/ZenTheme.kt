package com.samadhi.meditationtimer.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.samadhi.meditationtimer.R

enum class ParticleType {
    BAMBOO_LEAF,
    LOTUS_PETAL,
    SAKURA_PETAL
}

enum class ZenThemeId(val id: String, val title: String, val subtitle: String) {
    BAMBOO_TEAL("bamboo_teal", "Moonlit Bamboo", "Original serene teal & bamboo leaves"),
    SACRED_LOTUS("sacred_lotus", "Sacred Lotus", "Tranquil water lily & floating lotus petals"),
    SAKURA_TWILIGHT("sakura_twilight", "Sakura Twilight", "Midnight cherry blossom & drifting petals")
}

data class ZenTheme(
    val id: ZenThemeId,
    val title: String,
    val backgroundDrawableRes: Int,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val primaryGlow: Color,
    val overrunColor: Color,
    val particleType: ParticleType,
    val particlePalette: List<Color>
)

val BambooTealTheme = ZenTheme(
    id = ZenThemeId.BAMBOO_TEAL,
    title = "Moonlit Bamboo",
    backgroundDrawableRes = R.drawable.bg_zen_bamboo,
    primary = Color(0xFF2EA8A4),
    primaryLight = Color(0xFF5EEAD4),
    primaryDark = Color(0xFF164E4D),
    primaryGlow = Color(0x352EA8A4),
    overrunColor = Color(0xFF60A5FA),
    particleType = ParticleType.BAMBOO_LEAF,
    particlePalette = listOf(
        Color(0xFF2EA8A4),
        Color(0xFF5EEAD4),
        Color(0xFF38C7C2),
        Color(0xFF8BA870),
        Color(0xFF94A3B8),
        Color(0xFFD4AF37)
    )
)

val SacredLotusTheme = ZenTheme(
    id = ZenThemeId.SACRED_LOTUS,
    title = "Sacred Lotus",
    backgroundDrawableRes = R.drawable.bg_zen_lotus,
    primary = Color(0xFFD977A6),
    primaryLight = Color(0xFFF4A8C4),
    primaryDark = Color(0xFF831843),
    primaryGlow = Color(0x35D977A6),
    overrunColor = Color(0xFFF59E0B),
    particleType = ParticleType.LOTUS_PETAL,
    particlePalette = listOf(
        Color(0xFFF4A8C4),
        Color(0xFFE879A8),
        Color(0xFFD977A6),
        Color(0xFFFDE8E8),
        Color(0xFFFBCFE8),
        Color(0xFFFBBF24)
    )
)

val SakuraTwilightTheme = ZenTheme(
    id = ZenThemeId.SAKURA_TWILIGHT,
    title = "Sakura Twilight",
    backgroundDrawableRes = R.drawable.bg_zen_sakura,
    primary = Color(0xFFE0829D),
    primaryLight = Color(0xFFFBCFE8),
    primaryDark = Color(0xFF881337),
    primaryGlow = Color(0x35E0829D),
    overrunColor = Color(0xFF818CF8),
    particleType = ParticleType.SAKURA_PETAL,
    particlePalette = listOf(
        Color(0xFFFBCFE8),
        Color(0xFFF472B6),
        Color(0xFFE0829D),
        Color(0xFFFCE7F3),
        Color(0xFFDDD6FE),
        Color(0xFFE9D5FF)
    )
)

val LocalZenTheme = compositionLocalOf { BambooTealTheme }

fun getZenThemeById(id: String?): ZenTheme {
    return when (id) {
        ZenThemeId.SACRED_LOTUS.id -> SacredLotusTheme
        ZenThemeId.SAKURA_TWILIGHT.id -> SakuraTwilightTheme
        else -> BambooTealTheme
    }
}
