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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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
        // 1. Subtle Interactive Falling Particles Ambient Animation on pure AMOLED black
        FallingLeavesEffect(
            leafCount = 16,
            triggerBurst = leafBurstTrigger,
            ambientAlpha = 0.35f
        )

        // 2. Compact Main Content (Fits on a single screen without scrolling overflow)
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
                    horizontalArrangement = Arrangement.End
                ) {
                    // Support / Buy Me a Coffee Button
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ZenSurface.copy(alpha = 0.65f))
                            .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .clickable {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://ko-fi.com/hackysackapp")
                                ).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                                val strokeColor = currentTheme.primaryLight
                                val strokeW = 1.4f
                                // Cup body
                                val cup = Path().apply {
                                    moveTo(size.width * 0.18f, size.height * 0.34f)
                                    lineTo(size.width * 0.76f, size.height * 0.34f)
                                    lineTo(size.width * 0.70f, size.height * 0.82f)
                                    quadraticTo(size.width * 0.47f, size.height * 0.90f, size.width * 0.24f, size.height * 0.82f)
                                    close()
                                }
                                drawPath(cup, strokeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
                                // Handle
                                val handle = Path().apply {
                                    moveTo(size.width * 0.74f, size.height * 0.42f)
                                    quadraticTo(size.width * 0.98f, size.height * 0.54f, size.width * 0.70f, size.height * 0.68f)
                                }
                                drawPath(handle, strokeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
                                // Rising aroma steam
                                drawLine(strokeColor, Offset(size.width * 0.36f, size.height * 0.24f), Offset(size.width * 0.36f, size.height * 0.12f), strokeWidth = strokeW)
                                drawLine(strokeColor, Offset(size.width * 0.56f, size.height * 0.24f), Offset(size.width * 0.56f, size.height * 0.12f), strokeWidth = strokeW)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Donate",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                color = currentTheme.primaryLight
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

            // --- MINIMAL ORGANIC ZEN BRANCH SLIDER ---
            ZenBranchSlider(
                value = config.targetDurationMinutes.toFloat(),
                onValueChange = { newMin ->
                    viewModel.updateDurationMinutes(newMin.toInt())
                },
                valueRange = 1f..120f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            // --- QUICK DURATION PRESETS ---
            val presets = listOf(5, 10, 15, 20, 25, 30, 45, 60)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presets.forEach { duration ->
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
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${duration}m",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) currentTheme.primaryLight else ZenTextSecondary
                            )
                        }
                    }
                }
            }

            // --- TOGGLES SECTION ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. CONTINUE PAST TARGET TIME TOGGLE
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
                                text = "Continue Past Target Time",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = ZenTextPrimary
                            )
                            Text(
                                text = "Timer keeps counting up after reaching target time",
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

                // 2. BOX BREATHING MODE TOGGLE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZenSurface.copy(alpha = 0.60f))
                        .border(
                            1.dp,
                            if (config.isBoxBreathingMode) currentTheme.primary.copy(alpha = 0.5f) else ZenBorder.copy(alpha = 0.8f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Air,
                                    contentDescription = null,
                                    tint = if (config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Box Breathing Mode",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextPrimary
                                )
                            }
                            Text(
                                text = "Practice 4-4-4-4 visual breathwork with theme leaves",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = ZenTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = config.isBoxBreathingMode,
                            onCheckedChange = {
                                viewModel.toggleBoxBreathingMode(it)
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
            }

            // --- FLOATING ACTION BUTTON (DYNAMIC ACCORDING TO MODE) ---
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
                        if (config.isBoxBreathingMode) {
                            onOpenBreathing()
                        } else {
                            viewModel.startSession()
                            onStartSession()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (config.isBoxBreathingMode) Icons.Outlined.Air else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF041014),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (config.isBoxBreathingMode) "Begin Box Breathing" else "Begin Practice",
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
