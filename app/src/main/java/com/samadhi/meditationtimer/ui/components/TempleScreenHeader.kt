package com.samadhi.meditationtimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@Composable
fun TempleScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val currentTheme = LocalZenTheme.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ZenSurface.copy(alpha = 0.75f))
                            .border(1.dp, ZenBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = currentTheme.primaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    BuddhaOutlineLogo(size = 38.dp, tint = currentTheme.primaryLight, showEnsoCircle = true)
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        ),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenTextSecondary
                    )
                }
            }

            trailing?.invoke()
        }

        Spacer(modifier = Modifier.height(12.dp))
        BrushDivider(color = currentTheme.primary.copy(alpha = 0.34f))
    }
}

@Composable
fun BrushDivider(modifier: Modifier = Modifier, color: Color = LocalZenTheme.current.primary.copy(alpha = 0.32f)) {
    Canvas(modifier = modifier.fillMaxWidth().height(10.dp)) {
        val y = size.height * 0.52f
        val path = Path().apply {
            moveTo(size.width * 0.06f, y)
            cubicTo(size.width * 0.24f, y - 5f, size.width * 0.42f, y + 4f, size.width * 0.60f, y)
            cubicTo(size.width * 0.76f, y - 4f, size.width * 0.88f, y + 2f, size.width * 0.95f, y)
        }
        drawPath(path, color, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
        drawCircle(color.copy(alpha = 0.65f), radius = 2.4f, center = Offset(size.width * 0.045f, y))
    }
}
