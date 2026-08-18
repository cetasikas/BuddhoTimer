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
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        // 1. Subtle, Dynamic Whole-Screen Swirling Falling Leaves
        FallingLeavesEffect(
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
                    size = 36.dp,
                    tint = currentTheme.primaryLight,
                    strokeWidth = 2.0f,
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
                        text = if (config.isBoxBreathingMode) "Living Breathwork • Pranayama" else "Mindful Stillness • Samadhi",
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
                                fontSize = 72.sp,
                                fontWeight = FontWeight.ExtraLight,
                                letterSpacing = (-2).sp,
                                color = ZenTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = if (config.targetDurationMinutes == 1) "MINUTE" else "MINUTES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 3.sp,
                                color = currentTheme.primaryLight,
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
                                        .background(if (isSelected) currentTheme.primary.copy(alpha = 0.28f) else ZenSurface.copy(alpha = 0.70f))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primaryLight else ZenBorder, RoundedCornerShape(14.dp))
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
                                        color = if (isSelected) currentTheme.primaryLight else ZenTextSecondary
                                    )
                                }
                            }
                        }

                        // Continue Past Target Time Toggle
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
                                Column(modifier = Modifier.weight(1f)) {
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
                                        checkedThumbColor = currentTheme.primary,
                                        checkedTrackColor = currentTheme.primary.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // === BOX BREATHING SETUP (CLEAN & PERFECTLY FITTED) ===
                    val cycleDurationSec = selectedPattern.totalCycleSec
                    val estimatedCycles = (breathingDurationMin * 60) / cycleDurationSec.coerceAtLeast(1)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Large Duration & Cycles Display
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$breathingDurationMin",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.ExtraLight,
                                letterSpacing = (-2).sp,
                                color = ZenTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "$estimatedCycles CYCLES • $breathingDurationMin MINUTES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.5.sp,
                                color = currentTheme.primaryLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Duration Slider (1 to 30 min)
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

                        // 1 Clean Row of Breath Presets
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
                                        .background(if (isSelected) currentTheme.primary.copy(alpha = 0.28f) else ZenSurface.copy(alpha = 0.70f))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primaryLight else ZenBorder, RoundedCornerShape(14.dp))
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
                                        color = if (isSelected) currentTheme.primaryLight else ZenTextSecondary
                                    )
                                }
                            }
                        }

                        // Distinct High-Contrast Breathing Pattern Selector Card
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
                                                PhaseChip("4s Inhale", currentTheme.primaryLight)
                                                PhaseSeparator()
                                                PhaseChip("4s Hold", currentTheme.primaryLight)
                                                PhaseSeparator()
                                                PhaseChip("4s Exhale", currentTheme.overrunColor)
                                                PhaseSeparator()
                                                PhaseChip("4s Hold", ZenAmber)
                                            }
                                            "478" -> {
                                                PhaseChip("4s Inhale", currentTheme.primaryLight)
                                                PhaseSeparator()
                                                PhaseChip("7s Hold", currentTheme.primaryLight)
                                                PhaseSeparator()
                                                PhaseChip("8s Exhale", currentTheme.overrunColor)
                                            }
                                            "resonant" -> {
                                                PhaseChip("5s Inhale", currentTheme.primaryLight)
                                                PhaseSeparator()
                                                PhaseChip("5s Exhale", currentTheme.overrunColor)
                                            }
                                            else -> {
                                                PhaseChip("3s Inhale", currentTheme.primaryLight)
                                                PhaseSeparator()
                                                PhaseChip("3s Hold", currentTheme.primaryLight)
                                                PhaseSeparator()
                                                PhaseChip("3s Exhale", currentTheme.overrunColor)
                                                PhaseSeparator()
                                                PhaseChip("3s Hold", ZenAmber)
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
                                    .background(ZenSurface.copy(alpha = 0.98f))
                                    .border(1.2.dp, currentTheme.primary.copy(alpha = 0.50f), RoundedCornerShape(16.dp))
                                    .padding(vertical = 4.dp)
                            ) {
                                data class PatternMenuItem(
                                    val id: String,
                                    val title: String,
                                    val summary: String
                                )

                                val patternsList = listOf(
                                    PatternMenuItem(
                                        id = "box",
                                        title = "Box Breathing (4-4-4-4)",
                                        summary = "Focus & Stress Reset • 4s In • 4s Hold • 4s Out • 4s Hold"
                                    ),
                                    PatternMenuItem(
                                        id = "478",
                                        title = "4-7-8 Deep Relaxation",
                                        summary = "Deep Sleep & Relaxation • 4s In • 7s Hold • 8s Out"
                                    ),
                                    PatternMenuItem(
                                        id = "resonant",
                                        title = "Coherence Breathing (5-5)",
                                        summary = "Heart Coherence • 5s Inhale • 5s Exhale"
                                    ),
                                    PatternMenuItem(
                                        id = "gentle_box",
                                        title = "Gentle Box (3-3-3-3)",
                                        summary = "Beginner Friendly • 3s In • 3s Hold • 3s Out • 3s Hold"
                                    )
                                )

                                patternsList.forEach { item ->
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
                                                        text = item.title,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) currentTheme.primaryLight else ZenTextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = item.summary,
                                                        fontSize = 11.5.sp,
                                                        color = if (isSelected) currentTheme.primaryLight.copy(alpha = 0.85f) else ZenTextSecondary
                                                    )
                                                }

                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(currentTheme.primary.copy(alpha = 0.35f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Active Pattern",
                                                            tint = currentTheme.primaryLight,
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
                                            .background(if (isSelected) currentTheme.primary.copy(alpha = 0.20f) else Color.Transparent)
                                            .border(
                                                if (isSelected) 1.2.dp else 0.dp,
                                                if (isSelected) currentTheme.primaryLight.copy(alpha = 0.70f) else Color.Transparent,
                                                RoundedCornerShape(10.dp)
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
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Practice Mode Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ZenSurface.copy(alpha = 0.85f))
                        .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Meditation Option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!config.isBoxBreathingMode) currentTheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (!config.isBoxBreathingMode) currentTheme.primaryLight.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable {
                                if (config.isBoxBreathingMode) {
                                    viewModel.toggleBoxBreathingMode(false)
                                    sliderDragVelocity = 1.2f
                                    leafBurstTrigger++
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = if (!config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Meditation",
                                fontSize = 12.5.sp,
                                fontWeight = if (!config.isBoxBreathingMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextSecondary
                            )
                        }
                    }

                    // Box Breathing Option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (config.isBoxBreathingMode) currentTheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (config.isBoxBreathingMode) currentTheme.primaryLight.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable {
                                if (!config.isBoxBreathingMode) {
                                    viewModel.toggleBoxBreathingMode(true)
                                    sliderDragVelocity = 1.2f
                                    leafBurstTrigger++
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Air,
                                contentDescription = null,
                                tint = if (config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Box Breathing",
                                fontSize = 12.5.sp,
                                fontWeight = if (config.isBoxBreathingMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (config.isBoxBreathingMode) currentTheme.primaryLight else ZenTextSecondary
                            )
                        }
                    }
                }

                // Primary Action Button
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
                            tint = Color(0xFF041014),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (config.isBoxBreathingMode) "Begin Box Breathing" else "Begin Practice",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF041014)
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
