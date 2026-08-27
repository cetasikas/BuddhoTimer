package com.samadhi.meditationtimer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Fallback Material 3 Dark Color Scheme (Deep Obsidian & Golden Bodhi)
private val FallbackDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5A93B),
    onPrimary = Color(0xFF1E1500),
    primaryContainer = Color(0xFF5C4000),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = Color(0xFFD4AF37),
    onSecondary = Color(0xFF1E1500),
    secondaryContainer = Color(0xFF3B3014),
    onSecondaryContainer = Color(0xFFFFE082),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF003062),
    tertiaryContainer = Color(0xFF00468B),
    onTertiaryContainer = Color(0xFFD6E3FF),
    background = Color(0xFF0A0D12),
    onBackground = Color(0xFFF5F1E8),
    surface = Color(0xFF10141C),
    onSurface = Color(0xFFF5F1E8),
    surfaceVariant = Color(0xFF18202D),
    onSurfaceVariant = Color(0xFFCEC6B8),
    surfaceContainerLowest = Color(0xFF06080B),
    surfaceContainer = Color(0xFF131822),
    surfaceContainerHigh = Color(0xFF1A212E),
    surfaceContainerHighest = Color(0xFF222B3B),
    outline = Color(0xFF8A94A6),
    outlineVariant = Color(0xFF283446)
)

// Fallback Material 3 Light Color Scheme (Clean Warm Parchment & Rich Amber)
private val FallbackLightColorScheme = lightColorScheme(
    primary = Color(0xFF785900),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF261900),
    secondary = Color(0xFF6E5D2E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF9E0A6),
    onSecondaryContainer = Color(0xFF251A00),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E3FF),
    onTertiaryContainer = Color(0xFF001B3F),
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF1D1B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B18),
    surfaceVariant = Color(0xFFE8E0D2),
    onSurfaceVariant = Color(0xFF4C4639),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF3ECE1),
    surfaceContainerHigh = Color(0xFFEBE3D6),
    surfaceContainerHighest = Color(0xFFE2DACC),
    outline = Color(0xFF7E7667),
    outlineVariant = Color(0xFFDDD3C2)
)

@Composable
fun SamadhiTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val rawScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDarkColorScheme
        else -> FallbackLightColorScheme
    }

    // Preserve dynamic Material You accents (primary, secondary, tertiary)
    // while anchoring surfaces to deep Obsidian (Dark) and crisp Parchment (Light) for clean visual separation
    val colorScheme = if (darkTheme) {
        rawScheme.copy(
            background = Color(0xFF0A0D12),
            onBackground = Color(0xFFF5F1E8),
            surface = Color(0xFF10141C),
            onSurface = Color(0xFFF5F1E8),
            surfaceVariant = Color(0xFF18202D),
            onSurfaceVariant = Color(0xFFCEC6B8),
            surfaceContainerLowest = Color(0xFF06080B),
            surfaceContainer = Color(0xFF131822),
            surfaceContainerHigh = Color(0xFF1A212E),
            surfaceContainerHighest = Color(0xFF222B3B),
            outlineVariant = Color(0xFF283446)
        )
    } else {
        rawScheme.copy(
            background = Color(0xFFFAF8F5),
            onBackground = Color(0xFF1D1B18),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1D1B18),
            surfaceVariant = Color(0xFFE8E0D2),
            onSurfaceVariant = Color(0xFF4C4639),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFF3ECE1),
            surfaceContainerHigh = Color(0xFFEBE3D6),
            surfaceContainerHighest = Color(0xFFE2DACC),
            outlineVariant = Color(0xFFDDD3C2)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
