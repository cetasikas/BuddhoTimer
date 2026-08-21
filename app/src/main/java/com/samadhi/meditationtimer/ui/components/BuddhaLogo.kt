package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samadhi.meditationtimer.R

/**
 * Official BuddhoTimer App Emblem:
 * An authentic Sacred Lotus blossoming from an elegant Unalome spiral stem within a golden circle.
 */
@Composable
fun BuddhaOutlineLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color? = null,
    strokeWidth: Float = 2.0f,
    showEnsoCircle: Boolean = true
) {
    BuddhoEnsoLogo(
        modifier = modifier,
        size = size,
        tint = tint,
        strokeWidth = strokeWidth,
        showEnsoCircle = showEnsoCircle
    )
}

@Composable
fun BuddhoEnsoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color? = null,
    strokeWidth: Float = 2.0f,
    showEnsoCircle: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = if (showEnsoCircle) R.drawable.ic_buddho_emblem else R.drawable.ic_buddho_logo),
            contentDescription = "Buddho Timer Lotus Unalome Logo",
            colorFilter = tint?.let { ColorFilter.tint(it) },
            modifier = Modifier.size(size)
        )
    }
}
