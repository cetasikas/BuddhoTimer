package com.samadhi.meditationtimer.ui.screens.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenAmberGlow
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenGoldDust
import com.samadhi.meditationtimer.ui.theme.ZenGoldGlow
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTealGlow
import com.samadhi.meditationtimer.ui.theme.ZenTealLight
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onStartSession: () -> Unit,
    onOpenWisdom: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var leafBurstTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val presetDurations = listOf(5, 10, 15, 20, 30, 45, 60)

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        // 1. Dark Moonlit Plum Village Bamboo Background
        Image(
            painter = painterResource(id = R.drawable.bg_zen_bamboo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.22f
        )

        // Moonlit Gradient Overlay for deep serenity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060911).copy(alpha = 0.82f),
                            Color(0xFF0A1120).copy(alpha = 0.65f),
                            Color(0xFF060911).copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // 2. Interactive Falling Leaves Ambient Animation
        FallingLeavesEffect(
            leafCount = 16,
            triggerBurst = leafBurstTrigger,
            ambientAlpha = 0.32f
        )

        // 3. Scrollable Main Content Flow
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Minimalist Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BUDDHO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenTextPrimary
                    )
                )

                // Top Right Minimal Outline Action Buttons: [?] and [Settings]
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = onOpenWisdom,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ZenSurface.copy(alpha = 0.6f))
                            .border(1.dp, ZenBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = "Meditation Instructions",
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ZenSurface.copy(alpha = 0.6f))
                            .border(1.dp, ZenBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Meditation Settings",
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(38.dp))

            // 4. Floating Target Duration Display (No cards or borders behind the text)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "TARGET DURATION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = ZenTeal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${config.targetDurationMinutes}",
                        fontSize = 84.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = (-2).sp,
                        color = ZenTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "min",
                        fontSize = 24.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Light,
                        color = ZenGoldGlow,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )
                }

                Text(
                    text = if (config.isOverrunEnabled) "Practice continues seamlessly into Samadhi Overrun" else "Mindful chime sounds at target completion",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = ZenTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            // 5. Minimalist Horizontal Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Slider(
                    value = config.targetDurationMinutes.toFloat(),
                    onValueChange = {
                        viewModel.updateDurationMinutes(it.toInt())
                    },
                    valueRange = 1f..120f,
                    steps = 118,
                    colors = SliderDefaults.colors(
                        thumbColor = ZenTeal,
                        activeTrackColor = ZenTeal,
                        inactiveTrackColor = ZenBorder.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Clean Row of Minimal Pill-shaped Preset Buttons with Soft Glowing Active States
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetDurations.forEach { duration ->
                    val isSelected = config.targetDurationMinutes == duration

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) ZenTeal.copy(alpha = 0.22f)
                                else ZenSurface.copy(alpha = 0.6f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ZenTeal else ZenBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                viewModel.updateDurationMinutes(duration)
                                leafBurstTrigger++
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${duration}m",
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ZenTealLight else ZenTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 7. Minimalist "Keep It Going" Mode Toggle (Samadhi Overrun Option)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZenSurface.copy(alpha = 0.65f))
                    .border(1.dp, ZenBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep It Going (Samadhi Overrun)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ZenTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Timer runs past 00:00 without stopping so practice isn't cut short",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = ZenTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = config.isOverrunEnabled,
                        onCheckedChange = {
                            viewModel.toggleOverrun(it)
                            leafBurstTrigger++
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZenTeal,
                            checkedTrackColor = ZenTeal.copy(alpha = 0.35f),
                            uncheckedThumbColor = ZenTextMuted,
                            uncheckedTrackColor = ZenSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 8. Serene Floating "▶ Begin Practice" Button with Soft Ambient Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = ZenTeal)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF227876),
                                Color(0xFF2EA8A4),
                                Color(0xFF38C7C2)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF5EEAD4).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable {
                        leafBurstTrigger++
                        viewModel.startSession()
                        onStartSession()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF041014),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Begin Practice",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF041014)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
