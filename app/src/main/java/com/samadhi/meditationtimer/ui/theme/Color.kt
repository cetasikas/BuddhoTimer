package com.samadhi.meditationtimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Dynamic Theme Color Accessors (automatically bound to active Material 3 ColorScheme)
val ZenDarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val ZenSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainer

val ZenSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainerHigh

val ZenBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant

val ZenTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val ZenTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val ZenTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

val ZenObsidian: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onPrimary

// Accent and State Colors
val ZenTeal = Color(0xFF2EA8A4)
val ZenTealLight = Color(0xFF5EEAD4)
val ZenTealDark = Color(0xFF164E4D)
val ZenTealGlow = Color(0x352EA8A4)

val ZenLotus = Color(0xFFD4AF37)
val ZenLotusLight = Color(0xFFFFE082)
val ZenLotusDark = Color(0xFF8D6E1C)
val ZenLotusGlow = Color(0x35D4AF37)

val ZenSakura = Color(0xFFE0829D)
val ZenSakuraLight = Color(0xFFFBCFE8)
val ZenSakuraDark = Color(0xFF881337)
val ZenSakuraGlow = Color(0x35E0829D)

val ZenSage = Color(0xFF8BA888)
val ZenSageLight = Color(0xFFAECBB0)
val ZenSageDark = Color(0xFF4D6B51)

val ZenAmber = Color(0xFFE5A93B)
val ZenAmberLight = Color(0xFFFFCC66)
val ZenAmberDark = Color(0xFFB37B1B)
val ZenAmberGlow = Color(0x40E5A93B)

val ZenGoldGlow = Color(0xFFFFD580)
val ZenGoldCore = Color(0xFFFFF6D6)
val ZenGoldDust = Color(0xFFF0B849)

val ZenOverrunBlue = Color(0xFF60A5FA)
val ZenOverrunGlow = Color(0x3560A5FA)

val ZenSuccess = Color(0xFF34D399)
val ZenDanger = Color(0xFFF87171)
