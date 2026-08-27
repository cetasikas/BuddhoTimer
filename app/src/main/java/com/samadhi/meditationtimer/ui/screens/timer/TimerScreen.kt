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
import com.samadhi.meditationtimer.domain.model.BreathingPattern
import com.samadhi.meditationtimer.ui.components.ZenBranchSlider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
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
    val currentTheme = com.samadhi.meditationtimer.ui.theme.LocalZenTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var leafBurstTrigger by remember { mutableIntStateOf(0) }
    var sliderDragVelocity by remember { mutableFloatStateOf(0f) }

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

        // 2. Beautifully proportioned main container fitted perfectly to viewport
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. TOP HEADER (BUDDHO TIMER BRANDING) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                com.samadhi.meditationtimer.ui.components.BuddhaOutlineLogo(
                    size = 38.dp,
                    tint = null,
                    showEnsoCircle = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Buddho Timer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            fontSize = 15.5.sp
                        ),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = if (config.isBoxBreathingMode) "Mindful Breathwork • Box Breathing" else "Mindful Stillness • Samadhi",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = currentTheme.primaryLight
                    )
                }
            }

            // --- 2. DYNAMIC SETUP CONTENT (MEDITATION VS BOX BREATHING) ---
            AnimatedContent(
                targetState = config.isBoxBreathingMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(160))
                },
                label = "PracticeModeSetup",
                modifier = Modifier.fillMaxWidth()
            ) { isBoxMode ->
                if (!isBoxMode) {
                    // === MEDITATION TIMER SETUP (CLEAN & PERFECTLY FITTED) ===
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Large Bold Duration Display
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${config.targetDurationMinutes}",
                                fontSize = 76.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1.5).sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = if (config.targetDurationMinutes == 1) "MINUTE" else "MINUTES",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                color = currentTheme.primary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Organic Zen Branch Slider
                        ZenBranchSlider(
                            value = config.targetDurationMinutes.toFloat(),
                            onValueChange = { newMin ->
                                viewModel.updateDurationMinutes(newMin.toInt())
                            },
                            valueRange = 1f..120f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        )

                        // Clean Row of Essential Presets
                        val meditationPresets = listOf(5, 15, 30, 45, 60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            meditationPresets.forEach { duration ->
                                val isSelected = config.targetDurationMinutes == duration
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) currentTheme.primary.copy(alpha = 0.22f) else currentTheme.surfaceVariant)
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primary else currentTheme.border, RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.updateDurationMinutes(duration)
                                            leafBurstTrigger++
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${duration}m",
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) currentTheme.primary else currentTheme.textSecondary
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ZenSurface.copy(alpha = 0.75f))
                                    .border(1.dp, ZenBorder.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = "Do Not Disturb",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = ZenTextPrimary
                                        )
                                        Text(
                                            text = "Mute incoming notifications & calls during sit",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = ZenTextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = config.isAutoDndEnabled,
                                        onCheckedChange = {
                                            viewModel.toggleAutoDnd(it)
                                            leafBurstTrigger++
                                        },
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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ZenSurface.copy(alpha = 0.75f))
                                    .border(1.dp, ZenBorder.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = "Continue Past Target Time",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = ZenTextPrimary
                                        )
                                        Text(
                                            text = "Keep counting up after target time is reached",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = ZenTextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = config.isOverrunEnabled,
                                        onCheckedChange = {
                                            viewModel.toggleOverrun(it)
                                            leafBurstTrigger++
                                        },
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
                    val cycleDurationSec = selectedPattern.totalCycleSec
                    val estimatedCycles = (breathingDurationMin * 60) / cycleDurationSec.coerceAtLeast(1)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$breathingDurationMin",
                                fontSize = 76.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1.5).sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "$estimatedCycles CYCLES • $breathingDurationMin MINUTES",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = currentTheme.primary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        ZenBranchSlider(
                            value = breathingDurationMin.toFloat(),
                            onValueChange = { newMin ->
                                viewModel.updateBreathingDurationMinutes(newMin.toInt())
                            },
                            valueRange = 1f..30f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        )

                        val breathPresets = listOf(2, 5, 10, 15, 20)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            breathPresets.forEach { dur ->
                                val isSelected = breathingDurationMin == dur
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) currentTheme.primary.copy(alpha = 0.22f) else currentTheme.surfaceVariant)
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primary else currentTheme.border, RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.updateBreathingDurationMinutes(dur)
                                            leafBurstTrigger++
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dur}m",
                                        fontSize = 13.5.sp,
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { isPatternDropdownOpen = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = ZenSurface.copy(alpha = 0.85f)
                                ),
                                border = BorderStroke(1.2.dp, currentTheme.primary.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedPattern.name,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ZenTextPrimary
                                            )
                                            Text(
                                                text = patternCategory,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.8.sp,
                                                color = currentTheme.primaryLight
                                            )
                                        }

                                        Icon(
                                            imageVector = if (isPatternDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Select Pattern",
                                            tint = currentTheme.primaryLight,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Phase Timing Visual Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        when (selectedPattern.id) {
                                            "box" -> {
                                                PhaseChip("4s Inhale", currentTheme.primary)
                                                PhaseSeparator()
                                                PhaseChip("4s Hold", currentTheme.primary)
                                                PhaseSeparator()
                                                PhaseChip("4s Exhale", currentTheme.overrunColor)
                                                PhaseSeparator()
                                                PhaseChip("4s Hold", currentTheme.secondary)
                                            }
                                            "478" -> {
                                                PhaseChip("4s Inhale", currentTheme.primary)
                                                PhaseSeparator()
                                                PhaseChip("7s Hold", currentTheme.primary)
                                                PhaseSeparator()
                                                PhaseChip("8s Exhale", currentTheme.overrunColor)
                                            }
                                            "resonant" -> {
                                                PhaseChip("5s Inhale", currentTheme.primary)
                                                PhaseSeparator()
                                                PhaseChip("5s Exhale", currentTheme.overrunColor)
                                            }
                                            else -> {
                                                PhaseChip("3s Inhale", currentTheme.primary)
                                                PhaseSeparator()
                                                PhaseChip("3s Hold", currentTheme.primary)
                                                PhaseSeparator()
                                                PhaseChip("3s Exhale", currentTheme.overrunColor)
                                                PhaseSeparator()
                                                PhaseChip("3s Hold", currentTheme.secondary)
                                            }
                                        }
                                    }
                                }
                            }

                            // Clean, Uncluttered Dropdown Menu
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

                        // Do Not Disturb Toggle for Breathwork (Off by default)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ZenSurface.copy(alpha = 0.75f))
                                .border(1.dp, ZenBorder.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Do Not Disturb",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = ZenTextPrimary
                                    )
                                    Text(
                                        text = "Mute incoming notifications & calls during breathwork",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = ZenTextSecondary
                                    )
                                }
                                Switch(
                                    checked = config.isAutoDndEnabled,
                                    onCheckedChange = {
                                        viewModel.toggleAutoDnd(it)
                                        leafBurstTrigger++
                                    },
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

            // --- 3. BOTTOM CONTROLS (MODE TOGGLE + PRIMARY ACTION BUTTON) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Practice Mode Segmented Pill Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(currentTheme.surfaceVariant)
                        .border(1.dp, currentTheme.border, RoundedCornerShape(20.dp))
                        .padding(4.dp),
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
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = if (isMeditation) currentTheme.primary else currentTheme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Meditation",
                                fontSize = 13.sp,
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
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Air,
                                contentDescription = null,
                                tint = if (!isMeditation) currentTheme.primary else currentTheme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Box Breathing",
                                fontSize = 13.sp,
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
                        .height(52.dp)
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
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (config.isBoxBreathingMode) "Begin Box Breathing" else "Begin Practice",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .border(0.8.dp, color.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
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
