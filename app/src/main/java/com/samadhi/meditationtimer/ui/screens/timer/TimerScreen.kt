package com.samadhi.meditationtimer.ui.screens.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.domain.model.BreathingPattern
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.components.ManualDurationInputDialog
import com.samadhi.meditationtimer.ui.components.ZenBranchSlider
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
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
    val breathingPatternId by viewModel.selectedBreathingPatternId.collectAsState()
    val breathingDurationMin by viewModel.breathingDurationMinutes.collectAsState()
    val hasSeenDurationHint by viewModel.hasSeenDurationHint.collectAsState()
    val currentTheme = com.samadhi.meditationtimer.ui.theme.LocalZenTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var leafBurstTrigger by remember { mutableIntStateOf(0) }
    var sliderDragVelocity by remember { mutableFloatStateOf(0f) }
    var showMeditationDurationDialog by remember { mutableStateOf(false) }
    var showBreathingDurationDialog by remember { mutableStateOf(false) }

    val selectedPattern = BreathingPattern.allPatterns.find { it.id == breathingPatternId } ?: BreathingPattern.BOX_BREATHING

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                com.samadhi.meditationtimer.service.NotificationHelper(context).clearTimerNotification()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 1. Subtle, Dynamic Whole-Screen Swirling Falling Leaves
        FallingLeavesEffect(
            enabled = config.showLeavesDuringMeditation,
            leafCount = 26,
            triggerBurst = leafBurstTrigger,
            dragVelocity = sliderDragVelocity,
            ambientAlpha = 0.38f
        )

        // 2. Fluid, dynamic viewport-aware container that automatically scales to OS text and DPI settings
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            val screenHeight = maxHeight
            val density = LocalDensity.current
            val fontScale = density.fontScale.coerceIn(0.85f, 2.0f)

            // Dynamic scale factor based on reference viewport height (~620dp inside scaffold)
            val heightScale = (screenHeight / 620.dp).coerceIn(0.68f, 1.15f)
            val dynamicScale = (heightScale / kotlin.math.sqrt(fontScale.coerceAtLeast(1.0f).toDouble()).toFloat()).coerceIn(0.65f, 1.15f)
            val adaptiveDurationFontSize = ((68f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.25f)).sp
            val showTip = screenHeight >= 460.dp && fontScale <= 1.3f && !hasSeenDurationHint
            val showExplanations = screenHeight >= 500.dp && fontScale <= 1.25f

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // --- 1. TOP HEADER (BUDDHO TIMER BRANDING) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (2 * dynamicScale).dp, bottom = (2 * dynamicScale).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    com.samadhi.meditationtimer.ui.components.BuddhaOutlineLogo(
                        size = (36.dp * dynamicScale).coerceIn(24.dp, 38.dp),
                        tint = null,
                        showEnsoCircle = true
                    )
                    Spacer(modifier = Modifier.width((10.dp * dynamicScale).coerceIn(6.dp, 12.dp)))
                    Column {
                        Text(
                            text = "Buddho Timer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp,
                                fontSize = ((15.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.15f)).sp
                            ),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = if (config.isBoxBreathingMode) "Mindful Breathwork • Box Breathing" else "Mindful Stillness • Samadhi",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = ((11f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.15f)).sp
                            ),
                            color = currentTheme.primaryLight
                        )
                    }
                }

                // --- 2. DYNAMIC SETUP CONTENT (WEIGHTED MIDDLE ZONE) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = config.isBoxBreathingMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(160))
                        },
                        label = "PracticeModeSetup",
                        modifier = Modifier.fillMaxWidth()
                    ) { isBoxMode ->
                        if (!isBoxMode) {
                            // === MEDITATION TIMER SETUP (100% FLUID & NON-SCROLLING) ===
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy((6.dp * dynamicScale).coerceIn(2.dp, 8.dp), Alignment.CenterVertically)
                            ) {
                                // Large Bold Duration Display with Click-to-Edit
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            viewModel.dismissDurationHint()
                                            showMeditationDurationDialog = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = (1 * dynamicScale).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${config.targetDurationMinutes}",
                                            fontSize = adaptiveDurationFontSize,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-1.5).sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = if (config.targetDurationMinutes == 1) "MINUTE" else "MINUTES",
                                            fontSize = ((12f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.2f)).sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 2.5.sp,
                                            color = currentTheme.primary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }

                                // One-time dismissible coachmark hint (gracefully hides if screen height is constrained)
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = showTip,
                                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(currentTheme.surfaceVariant.copy(alpha = 0.92f))
                                            .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                            .clickable { viewModel.dismissDurationHint() }
                                            .padding(horizontal = 12.dp, vertical = 3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Tip: Tap time to enter custom minutes",
                                                fontSize = ((11f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.15f)).sp,
                                                fontWeight = FontWeight.Medium,
                                                color = currentTheme.textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss tip",
                                                tint = currentTheme.textSecondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Organic Zen Branch Slider
                                ZenBranchSlider(
                                    value = config.targetDurationMinutes.toFloat().coerceIn(1f, 120f),
                                    onValueChange = { newMin ->
                                        viewModel.updateDurationMinutes(newMin.toInt())
                                    },
                                    valueRange = 1f..120f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((44.dp * dynamicScale).coerceIn(32.dp, 48.dp))
                                )

                                // Clean Row of Essential Presets
                                val meditationPresets = listOf(5, 15, 30, 45, 60)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy((6.dp * dynamicScale).coerceIn(4.dp, 8.dp))
                                ) {
                                    meditationPresets.forEach { duration ->
                                        val isSelected = config.targetDurationMinutes == duration
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape((12.dp * dynamicScale).coerceIn(8.dp, 14.dp)))
                                                .background(if (isSelected) currentTheme.primary.copy(alpha = 0.22f) else currentTheme.surfaceVariant)
                                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primary else currentTheme.border, RoundedCornerShape((12.dp * dynamicScale).coerceIn(8.dp, 14.dp)))
                                                .clickable {
                                                    viewModel.updateDurationMinutes(duration)
                                                }
                                                .padding(vertical = (6.dp * dynamicScale).coerceIn(2.5.dp, 7.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${duration}m",
                                                fontSize = ((12.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.15f)).sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) currentTheme.primary else currentTheme.textSecondary
                                            )
                                        }
                                    }
                                }

                                // Consolidated Zen Settings Card (DND + Overrun)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape((14.dp * dynamicScale).coerceIn(10.dp, 16.dp)))
                                        .background(ZenSurface.copy(alpha = 0.75f))
                                        .border(1.dp, ZenBorder.copy(alpha = 0.9f), RoundedCornerShape((14.dp * dynamicScale).coerceIn(10.dp, 16.dp)))
                                        .padding(horizontal = 12.dp, vertical = (3.dp * dynamicScale).coerceIn(1.5.dp, 5.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // 1. Do Not Disturb Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = (3.dp * dynamicScale).coerceIn(1.dp, 4.dp)),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = "Do Not Disturb",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = ((12.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.2f)).sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = ZenTextPrimary
                                                )
                                                if (showExplanations) {
                                                    Text(
                                                        text = "Mute incoming notifications & calls during sit",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = ((10f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.15f)).sp
                                                        ),
                                                        color = ZenTextSecondary
                                                    )
                                                }
                                            }
                                            Switch(
                                                checked = config.isAutoDndEnabled,
                                                onCheckedChange = {
                                                    viewModel.toggleAutoDnd(it)
                                                    leafBurstTrigger++
                                                },
                                                modifier = Modifier.scale((0.92f * dynamicScale).coerceIn(0.75f, 1.0f)),
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                    checkedTrackColor = currentTheme.primary,
                                                    checkedBorderColor = currentTheme.primary,
                                                    uncheckedThumbColor = currentTheme.textSecondary,
                                                    uncheckedTrackColor = currentTheme.surfaceVariant,
                                                    uncheckedBorderColor = currentTheme.border
                                                )
                                            )
                                        }

                                        // Hairline Zen Divider
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 1.dp)
                                                .height(0.8.dp)
                                                .background(ZenBorder.copy(alpha = 0.45f))
                                        )

                                        // 2. Continue Past Target Time Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = (3.dp * dynamicScale).coerceIn(1.dp, 4.dp)),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = "Continue Past Target Time",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontSize = ((12.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.2f)).sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = ZenTextPrimary
                                                )
                                                if (showExplanations) {
                                                    Text(
                                                        text = "Keep counting up after target time is reached",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = ((10f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.15f)).sp
                                                        ),
                                                        color = ZenTextSecondary
                                                    )
                                                }
                                            }
                                            Switch(
                                                checked = config.isOverrunEnabled,
                                                onCheckedChange = {
                                                    viewModel.toggleOverrun(it)
                                                    leafBurstTrigger++
                                                },
                                                modifier = Modifier.scale((0.92f * dynamicScale).coerceIn(0.75f, 1.0f)),
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                    checkedTrackColor = currentTheme.primary,
                                                    checkedBorderColor = currentTheme.primary,
                                                    uncheckedThumbColor = currentTheme.textSecondary,
                                                    uncheckedTrackColor = currentTheme.surfaceVariant,
                                                    uncheckedBorderColor = currentTheme.border
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // === BOX BREATHING SETUP (100% FLUID & NON-SCROLLING) ===
                            val cycleDurationSec = selectedPattern.totalCycleSec
                            val estimatedCycles = (breathingDurationMin * 60) / cycleDurationSec.coerceAtLeast(1)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy((6.dp * dynamicScale).coerceIn(2.dp, 8.dp), Alignment.CenterVertically)
                            ) {
                                // Large Bold Duration Display with Click-to-Edit
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            viewModel.dismissDurationHint()
                                            showBreathingDurationDialog = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = (1 * dynamicScale).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$breathingDurationMin",
                                            fontSize = adaptiveDurationFontSize,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-1.5).sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "$estimatedCycles CYCLES • $breathingDurationMin MINUTES",
                                            fontSize = ((11.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.2f)).sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.8.sp,
                                            color = currentTheme.primary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }

                                // One-time dismissible coachmark hint
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = showTip,
                                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(currentTheme.surfaceVariant.copy(alpha = 0.92f))
                                            .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                            .clickable { viewModel.dismissDurationHint() }
                                            .padding(horizontal = 12.dp, vertical = 3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Tip: Tap time to enter custom minutes",
                                                fontSize = ((11f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.15f)).sp,
                                                fontWeight = FontWeight.Medium,
                                                color = currentTheme.textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss tip",
                                                tint = currentTheme.textSecondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                ZenBranchSlider(
                                    value = breathingDurationMin.toFloat().coerceIn(1f, 30f),
                                    onValueChange = { newMin ->
                                        viewModel.updateBreathingDurationMinutes(newMin.toInt())
                                    },
                                    valueRange = 1f..30f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((44.dp * dynamicScale).coerceIn(32.dp, 48.dp))
                                )

                                val breathPresets = listOf(2, 5, 10, 15, 20)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy((6.dp * dynamicScale).coerceIn(4.dp, 8.dp))
                                ) {
                                    breathPresets.forEach { dur ->
                                        val isSelected = breathingDurationMin == dur
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape((12.dp * dynamicScale).coerceIn(8.dp, 14.dp)))
                                                .background(if (isSelected) currentTheme.primary.copy(alpha = 0.22f) else currentTheme.surfaceVariant)
                                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primary else currentTheme.border, RoundedCornerShape((12.dp * dynamicScale).coerceIn(8.dp, 14.dp)))
                                                .clickable {
                                                    viewModel.updateBreathingDurationMinutes(dur)
                                                }
                                                .padding(vertical = (6.dp * dynamicScale).coerceIn(2.5.dp, 7.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${dur}m",
                                                fontSize = ((12.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.15f)).sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) currentTheme.primary else currentTheme.textSecondary
                                            )
                                        }
                                    }
                                }

                                var isPatternDropdownOpen by remember { mutableStateOf(false) }

                                val patternCategory = when (selectedPattern.id) {
                                    "box" -> "FOCUS & STRESS RESET"
                                    "478" -> "DEEP SLEEP & RELAXATION"
                                    "resonant" -> "HEART COHERENCE (5-5)"
                                    else -> "BEGINNER FRIENDLY"
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape((14.dp * dynamicScale).coerceIn(10.dp, 16.dp)))
                                            .clickable { isPatternDropdownOpen = true },
                                        colors = CardDefaults.cardColors(
                                            containerColor = ZenSurface.copy(alpha = 0.85f)
                                        ),
                                        border = BorderStroke(1.2.dp, currentTheme.primary.copy(alpha = 0.55f)),
                                        shape = RoundedCornerShape((14.dp * dynamicScale).coerceIn(10.dp, 16.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = (6.dp * dynamicScale).coerceIn(3.dp, 8.dp)),
                                            verticalArrangement = Arrangement.spacedBy((3.dp * dynamicScale).coerceIn(1.dp, 4.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = selectedPattern.name,
                                                        fontSize = ((13.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.2f)).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ZenTextPrimary
                                                    )
                                                    Text(
                                                        text = patternCategory,
                                                        fontSize = ((9.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.1f)).sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        letterSpacing = 0.8.sp,
                                                        color = currentTheme.primaryLight
                                                    )
                                                }

                                                Icon(
                                                    imageVector = if (isPatternDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Select Pattern",
                                                    tint = currentTheme.primaryLight,
                                                    modifier = Modifier.size((18.dp * dynamicScale).coerceIn(15.dp, 20.dp))
                                                )
                                            }

                                            // Phase Timing Visual Chips
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy((4.dp * dynamicScale).coerceIn(2.dp, 4.dp)),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                when (selectedPattern.id) {
                                                    "box" -> {
                                                        PhaseChip("4s Inhale", currentTheme.primary, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("4s Hold", currentTheme.primary, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("4s Exhale", currentTheme.overrunColor, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("4s Hold", currentTheme.secondary, dynamicScale, fontScale)
                                                    }
                                                    "478" -> {
                                                        PhaseChip("4s Inhale", currentTheme.primary, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("7s Hold", currentTheme.primary, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("8s Exhale", currentTheme.overrunColor, dynamicScale, fontScale)
                                                    }
                                                    "resonant" -> {
                                                        PhaseChip("5s Inhale", currentTheme.primary, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("5s Exhale", currentTheme.overrunColor, dynamicScale, fontScale)
                                                    }
                                                    else -> {
                                                        PhaseChip("3s Inhale", currentTheme.primary, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("3s Hold", currentTheme.primary, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("3s Exhale", currentTheme.overrunColor, dynamicScale, fontScale)
                                                        PhaseSeparator()
                                                        PhaseChip("3s Hold", currentTheme.secondary, dynamicScale, fontScale)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isPatternDropdownOpen,
                                        onDismissRequest = { isPatternDropdownOpen = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.90f)
                                            .background(currentTheme.surfaceVariant)
                                            .border(1.2.dp, currentTheme.primary.copy(alpha = 0.50f), RoundedCornerShape(16.dp))
                                            .padding(vertical = 4.dp)
                                    ) {
                                        BreathingPattern.allPatterns.forEach { item ->
                                            val isSelected = selectedPattern.id == item.id
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = item.name,
                                                                fontSize = 14.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) currentTheme.primary else ZenTextPrimary
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = item.subtitle,
                                                                fontSize = 11.5.sp,
                                                                color = if (isSelected) currentTheme.primary else ZenTextSecondary
                                                            )
                                                        }

                                                        if (isSelected) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(24.dp)
                                                                    .clip(CircleShape)
                                                                    .background(currentTheme.primary.copy(alpha = 0.22f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.PlayArrow,
                                                                    contentDescription = "Active Pattern",
                                                                    tint = currentTheme.primary,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.updateBreathingPattern(item.id)
                                                    isPatternDropdownOpen = false
                                                    leafBurstTrigger++
                                                },
                                                modifier = Modifier
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) currentTheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                                                    .border(
                                                        if (isSelected) 1.2.dp else 0.dp,
                                                        if (isSelected) currentTheme.primary else Color.Transparent,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                            )
                                        }
                                    }
                                }

                                // Do Not Disturb Toggle for Breathwork
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape((14.dp * dynamicScale).coerceIn(10.dp, 16.dp)))
                                        .background(ZenSurface.copy(alpha = 0.75f))
                                        .border(1.dp, ZenBorder.copy(alpha = 0.9f), RoundedCornerShape((14.dp * dynamicScale).coerceIn(10.dp, 16.dp)))
                                        .padding(horizontal = 12.dp, vertical = (4.dp * dynamicScale).coerceIn(2.dp, 5.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = "Do Not Disturb",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = ((12.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.2f)).sp,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = ZenTextPrimary
                                            )
                                            if (showExplanations) {
                                                Text(
                                                    text = "Mute incoming notifications & calls during breathwork",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = ((10f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.15f)).sp
                                                    ),
                                                    color = ZenTextSecondary
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = config.isAutoDndEnabled,
                                            onCheckedChange = {
                                                viewModel.toggleAutoDnd(it)
                                                leafBurstTrigger++
                                            },
                                            modifier = Modifier.scale((0.92f * dynamicScale).coerceIn(0.75f, 1.0f)),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                                checkedTrackColor = currentTheme.primary,
                                                checkedBorderColor = currentTheme.primary,
                                                uncheckedThumbColor = currentTheme.textSecondary,
                                                uncheckedTrackColor = currentTheme.surfaceVariant,
                                                uncheckedBorderColor = currentTheme.border
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 3. BOTTOM CONTROLS (MODE TOGGLE + PRIMARY ACTION BUTTON) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (4 * dynamicScale).dp, bottom = (2 * dynamicScale).dp),
                    verticalArrangement = Arrangement.spacedBy((8.dp * dynamicScale).coerceIn(4.dp, 10.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Practice Mode Segmented Pill Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(currentTheme.surfaceVariant)
                            .border(1.dp, currentTheme.border, RoundedCornerShape(20.dp))
                            .padding((3.dp * dynamicScale).coerceIn(2.dp, 4.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Meditation Option
                        val isMeditation = !config.isBoxBreathingMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isMeditation) currentTheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                                .border(if (isMeditation) 1.5.dp else 0.dp, if (isMeditation) currentTheme.primary else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable {
                                if (!isMeditation) {
                                    viewModel.toggleBoxBreathingMode(false)
                                    sliderDragVelocity = 1.2f
                                    leafBurstTrigger++
                                }
                            }
                            .padding(
                                horizontal = (14.dp * dynamicScale).coerceIn(8.dp, 16.dp),
                                vertical = (5.dp * dynamicScale).coerceIn(3.dp, 7.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SelfImprovement,
                                    contentDescription = null,
                                    tint = if (isMeditation) currentTheme.primary else currentTheme.textSecondary,
                                    modifier = Modifier.size((15.dp * dynamicScale).coerceIn(12.dp, 16.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Meditation",
                                    fontSize = ((12.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.15f)).sp,
                                    fontWeight = if (isMeditation) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMeditation) currentTheme.primary else currentTheme.textSecondary
                                )
                            }
                        }

                        // Box Breathing Option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!isMeditation) currentTheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                                .border(if (!isMeditation) 1.5.dp else 0.dp, if (!isMeditation) currentTheme.primary else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable {
                                if (isMeditation) {
                                    viewModel.toggleBoxBreathingMode(true)
                                    sliderDragVelocity = 1.2f
                                    leafBurstTrigger++
                                }
                            }
                            .padding(
                                horizontal = (14.dp * dynamicScale).coerceIn(8.dp, 16.dp),
                                vertical = (5.dp * dynamicScale).coerceIn(3.dp, 7.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Air,
                                    contentDescription = null,
                                    tint = if (!isMeditation) currentTheme.primary else currentTheme.textSecondary,
                                    modifier = Modifier.size((15.dp * dynamicScale).coerceIn(12.dp, 16.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Box Breathing",
                                    fontSize = ((12.5f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.15f)).sp,
                                    fontWeight = if (!isMeditation) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isMeditation) currentTheme.primary else currentTheme.textSecondary
                                )
                            }
                        }
                    }

                    // Primary Action Button (Tactile Golden Gradient with subtle glow)
                    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val buttonGradient = Brush.horizontalGradient(
                        colors = if (isDark) {
                            listOf(
                                currentTheme.primary.copy(alpha = 0.88f),
                                currentTheme.primary,
                                Color(0xFFFFD54F)
                            )
                        } else {
                            listOf(
                                currentTheme.primary,
                                currentTheme.primary.copy(alpha = 0.90f),
                                currentTheme.primary
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((48.dp * dynamicScale).coerceIn(38.dp, 52.dp))
                            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = currentTheme.primary.copy(alpha = 0.40f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(buttonGradient)
                            .border(1.dp, currentTheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .clickable {
                                sliderDragVelocity = 1.5f
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
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size((20.dp * dynamicScale).coerceIn(16.dp, 22.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (config.isBoxBreathingMode) "Begin Box Breathing" else "Begin Practice",
                                fontSize = ((15f * dynamicScale) / fontScale * fontScale.coerceIn(0.9f, 1.15f)).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        if (showMeditationDurationDialog) {
            ManualDurationInputDialog(
                initialMinutes = config.targetDurationMinutes,
                minMinutes = 1,
                maxMinutes = 180,
                title = "Meditation Duration",
                subtitle = "Enter sit duration in minutes (1–180)",
                onDismiss = { showMeditationDurationDialog = false },
                onConfirm = { minutes ->
                    viewModel.updateDurationMinutes(minutes)
                }
            )
        }

        if (showBreathingDurationDialog) {
            ManualDurationInputDialog(
                initialMinutes = breathingDurationMin,
                minMinutes = 1,
                maxMinutes = 60,
                title = "Breathwork Duration",
                subtitle = "Enter breathwork duration in minutes (1–60)",
                onDismiss = { showBreathingDurationDialog = false },
                onConfirm = { minutes ->
                    viewModel.updateBreathingDurationMinutes(minutes)
                }
            )
        }
    }
}

@Composable
private fun PhaseChip(label: String, color: Color, dynamicScale: Float = 1f, fontScale: Float = 1f) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .border(0.8.dp, color.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(
                horizontal = (5.dp * dynamicScale).coerceIn(3.dp, 6.dp),
                vertical = (2.dp * dynamicScale).coerceIn(1.dp, 2.5.dp)
            )
    ) {
        Text(
            text = label,
            fontSize = ((10f * dynamicScale) / fontScale * fontScale.coerceIn(0.85f, 1.1f)).sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun PhaseSeparator() {
    Text(
        text = "➔",
        fontSize = 10.sp,
        color = ZenTextMuted
    )
}
