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

        // 2. Beautifully proportioned main container with balanced spacing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. TOP HEADER (BUDDHO TIMER BRANDING) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                com.samadhi.meditationtimer.ui.components.BuddhaOutlineLogo(
                    size = 40.dp,
                    tint = currentTheme.primaryLight,
                    strokeWidth = 2.2f,
                    showEnsoCircle = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Buddho Timer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 16.sp
                        ),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = if (config.isBoxBreathingMode) "Living Breathwork" else "Mindful Stillness",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = currentTheme.primaryLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- 2. DYNAMIC SETUP CONTENT (MEDITATION VS BOX BREATHING) ---
            AnimatedContent(
                targetState = config.isBoxBreathingMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "PracticeModeSetup"
            ) { isBoxMode ->
                if (!isBoxMode) {
                    // === MEDITATION TIMER SETUP (CLEAN & SPACIOUS) ===
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Large Bold Duration Display
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${config.targetDurationMinutes}",
                                fontSize = 78.sp,
                                fontWeight = FontWeight.ExtraLight,
                                letterSpacing = (-2).sp,
                                color = ZenTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = if (config.targetDurationMinutes == 1) "MINUTE" else "MINUTES",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 4.sp,
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
                                .height(64.dp)
                        )

                        // 1 Clean Row of Essential Presets
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
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) currentTheme.primary.copy(alpha = 0.28f) else ZenSurface.copy(alpha = 0.70f))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primaryLight else ZenBorder, RoundedCornerShape(16.dp))
                                        .clickable {
                                            viewModel.updateDurationMinutes(duration)
                                            leafBurstTrigger++
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${duration}m",
                                        fontSize = 14.sp,
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
                                .background(ZenSurface.copy(alpha = 0.70f))
                                .border(1.dp, ZenBorder.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = ZenTextPrimary
                                    )
                                    Text(
                                        text = "Keep counting up after target time is reached",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
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
                    // === BOX BREATHING SETUP (CLEAN & DISTINCT) ===
                    val cycleDurationSec = selectedPattern.totalCycleSec
                    val estimatedCycles = (breathingDurationMin * 60) / cycleDurationSec.coerceAtLeast(1)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Large Duration & Cycles Display
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$breathingDurationMin",
                                fontSize = 78.sp,
                                fontWeight = FontWeight.ExtraLight,
                                letterSpacing = (-2).sp,
                                color = ZenTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "$estimatedCycles CYCLES • $breathingDurationMin MINUTES",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 3.sp,
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
                                .height(64.dp)
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
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) currentTheme.primary.copy(alpha = 0.28f) else ZenSurface.copy(alpha = 0.70f))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentTheme.primaryLight else ZenBorder, RoundedCornerShape(16.dp))
                                        .clickable {
                                            viewModel.updateBreathingDurationMinutes(dur)
                                            leafBurstTrigger++
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${dur}m",
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) currentTheme.primaryLight else ZenTextSecondary
                                    )
                                }
                            }
                        }

                        // Distinct High-Contrast Breathing Pattern Selector Card
                        var isPatternDropdownOpen by remember { mutableStateOf(false) }

                        val patternTag = when (selectedPattern.id) {
                            "box" -> "EQUAL 4-PHASE BOX"
                            "478" -> "DEEP SLEEP & RELAX"
                            "resonant" -> "HEART COHERENCE (5-5)"
                            else -> "GENTLE 3-PHASE BOX"
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
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(currentTheme.primary.copy(alpha = 0.18f))
                                                    .border(0.8.dp, currentTheme.primaryLight.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                            ) {
                                                Text(
                                                    text = patternTag,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    color = currentTheme.primaryLight
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = selectedPattern.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ZenTextPrimary
                                            )
                                        }

                                        Icon(
                                            imageVector = if (isPatternDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = currentTheme.primaryLight,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Clear Phase Breakdown Flow
                                    val phaseSummary = when (selectedPattern.id) {
                                        "box" -> "4s Inhale ➔ 4s Hold ➔ 4s Exhale ➔ 4s Hold"
                                        "478" -> "4s Inhale ➔ 7s Hold ➔ 8s Exhale"
                                        "resonant" -> "5s Inhale ➔ 5s Exhale (Continuous Flow)"
                                        else -> "3s Inhale ➔ 3s Hold ➔ 3s Exhale ➔ 3s Hold"
                                    }

                                    Text(
                                        text = phaseSummary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = currentTheme.primaryLight
                                    )
                                }
                            }

                            // Distinct, High-Contrast Dropdown Options
                            DropdownMenu(
                                expanded = isPatternDropdownOpen,
                                onDismissRequest = { isPatternDropdownOpen = false },
                                modifier = Modifier
                                    .background(ZenSurface.copy(alpha = 0.98f))
                                    .border(1.2.dp, currentTheme.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            ) {
                                val patternsWithInfo = listOf(
                                    Triple("box", "Box Breathing (4-4-4-4)", "4s In • 4s Hold • 4s Out • 4s Hold (Equal Box Focus)"),
                                    Triple("478", "4-7-8 Deep Relaxation", "4s In • 7s Hold • 8s Out (Deep Sedating Calm)"),
                                    Triple("resonant", "Coherence (5-5)", "5s Inhale • 5s Exhale (Smooth Heart Rate Coherence)"),
                                    Triple("box_3333", "Gentle Box (3-3-3-3)", "3s In • 3s Hold • 3s Out • 3s Hold (Beginner Friendly)")
                                )

                                patternsWithInfo.forEach { (id, name, timing) ->
                                    val isSelected = selectedPattern.id == id
                                    DropdownMenuItem(
                                        text = {
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text(
                                                    text = name,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) currentTheme.primaryLight else ZenTextPrimary
                                                )
                                                Text(
                                                    text = timing,
                                                    fontSize = 11.5.sp,
                                                    color = if (isSelected) currentTheme.primaryLight.copy(alpha = 0.8f) else ZenTextSecondary
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.updateBreathingPattern(id)
                                            isPatternDropdownOpen = false
                                            leafBurstTrigger++
                                        },
                                        leadingIcon = {
                                            if (isSelected) {
                                                Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = currentTheme.primaryLight, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. BOTTOM CONTROLS (MODE TOGGLE NEAR BOTTOM + PRIMARY ACTION BUTTON) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Practice Mode Toggle Near Bottom
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(ZenSurface.copy(alpha = 0.85f))
                        .border(1.dp, currentTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Meditation Option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!config.isBoxBreathingMode) currentTheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (!config.isBoxBreathingMode) currentTheme.primaryLight.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(20.dp))
                            .clickable {
                                if (config.isBoxBreathingMode) {
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
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (config.isBoxBreathingMode) currentTheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.dp, if (config.isBoxBreathingMode) currentTheme.primaryLight.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(20.dp))
                            .clickable {
                                if (!config.isBoxBreathingMode) {
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
                        .height(56.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = currentTheme.primary)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    currentTheme.primaryDark,
                                    currentTheme.primary,
                                    currentTheme.primaryLight
                                )
                            )
                        )
                        .border(1.dp, currentTheme.primaryLight.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (config.isBoxBreathingMode) "Begin Box Breathing" else "Begin Practice",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF041014)
                        )
                    }
                }
            }
        }
    }
}
