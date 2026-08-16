package com.samadhi.meditationtimer.ui.screens.timer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.samadhi.meditationtimer.ui.components.ZenBranchSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenGoldGlow
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
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
    onOpenBreathing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val currentTheme = com.samadhi.meditationtimer.ui.theme.LocalZenTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var leafBurstTrigger by remember { mutableIntStateOf(0) }
    var isSliding by remember { mutableStateOf(false) }

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
        // 1. Dynamic Moonlit Plum Village Background
        Image(
            painter = painterResource(id = currentTheme.backgroundDrawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.20f
        )

        // Moonlit Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060911).copy(alpha = 0.85f),
                            Color(0xFF0A1120).copy(alpha = 0.68f),
                            Color(0xFF060911).copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // 2. Interactive Falling Particles Ambient Animation
        FallingLeavesEffect(
            leafCount = 16,
            triggerBurst = leafBurstTrigger,
            ambientAlpha = 0.32f
        )

        // 3. Compact Main Content (Fits on a single screen without scrolling overflow)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP HEADER ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Out-of-the-way Minimal Box Breathing Pill Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(currentTheme.primary.copy(alpha = 0.14f))
                            .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .clickable { onOpenBreathing() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Air,
                                contentDescription = null,
                                tint = currentTheme.primaryLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zen Box Breathing",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                color = currentTheme.primaryLight
                            )
                        }
                    }

                    // Action Icons: [?] and [⚙]
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onOpenWisdom,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ZenSurface.copy(alpha = 0.65f))
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ZenSurface.copy(alpha = 0.65f))
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
            }

            // --- LARGE MINIMAL TARGET DURATION DISPLAY ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${config.targetDurationMinutes}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-2).sp,
                    color = ZenTextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = if (config.targetDurationMinutes == 1) "MINUTE" else "MINUTES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp,
                    color = currentTheme.primaryLight,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // --- MINIMAL SLIDER WITH LEAF INTERACTION ANIMATION ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Minimal Organic Zen Branch Slider
                ZenBranchSlider(
                    value = config.targetDurationMinutes.toFloat(),
                    onValueChange = {
                        viewModel.updateDurationMinutes(it.toInt())
                        leafBurstTrigger++
                    },
                    valueRange = 1f..120f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // --- CLEAN GLOWING PRESET PILL BUTTONS ---
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetDurations.forEach { duration ->
                        val isSelected = config.targetDurationMinutes == duration

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) currentTheme.primary.copy(alpha = 0.22f)
                                    else ZenSurface.copy(alpha = 0.55f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) currentTheme.primaryLight else ZenBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.updateDurationMinutes(duration)
                                    leafBurstTrigger++
                                }
                                .padding(horizontal = 13.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${duration}m",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) currentTheme.primaryLight else ZenTextSecondary
                            )
                        }
                    }
                }
            }

            // --- COMPACT KEEP IT GOING MODE TOGGLE ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ZenSurface.copy(alpha = 0.60f))
                    .border(1.dp, ZenBorder.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = "Timer continues past 00:00 without interruption",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = ZenTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = config.isOverrunEnabled,
                        onCheckedChange = {
                            viewModel.toggleOverrun(it)
                            leafBurstTrigger++
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = currentTheme.primary,
                            checkedTrackColor = currentTheme.primary.copy(alpha = 0.35f),
                            uncheckedThumbColor = ZenTextMuted,
                            uncheckedTrackColor = ZenSurfaceVariant
                        )
                    )
                }
            }

            // --- COMPACT FLOATING "BEGIN PRACTICE" ACTION BUTTON ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = currentTheme.primary)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                currentTheme.primaryDark,
                                currentTheme.primary,
                                currentTheme.primaryLight
                            )
                        )
                    )
                    .border(1.dp, currentTheme.primaryLight.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
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
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Begin Practice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF041014)
                    )
                }
            }
        }
    }
}
