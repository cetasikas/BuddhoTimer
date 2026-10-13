package com.samadhi.meditationtimer.ui.screens.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.domain.model.BreathingPattern
import com.samadhi.meditationtimer.domain.model.TimerConfiguration
import com.samadhi.meditationtimer.service.AmbientSoundEngine
import com.samadhi.meditationtimer.service.SoundHelper
import com.samadhi.meditationtimer.ui.components.BuddhaOutlineLogo
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.components.ManualDurationInputDialog
import com.samadhi.meditationtimer.ui.components.ZenBranchSlider

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
    val isDndGranted by viewModel.isDndGranted.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isBoxMode = config.isBoxBreathingMode
    var leafBurstTrigger by remember { mutableIntStateOf(0) }
    var showMeditationDurationDialog by remember { mutableStateOf(false) }
    var showBreathingDurationDialog by remember { mutableStateOf(false) }
    var showAmbientDialog by remember { mutableStateOf(false) }
    val setupScroll = rememberScrollState()

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

    Box(modifier = modifier.fillMaxSize().background(colorScheme.background)) {
        // Ambient, low-key drifting leaves (kept sparse so they never fight the controls)
        FallingLeavesEffect(
            enabled = config.showLeavesDuringMeditation,
            leafCount = 14,
            triggerBurst = leafBurstTrigger,
            ambientAlpha = 0.28f,
            pileUpAtBottom = true,
            initialSettledLeavesCount = 5
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            val screenHeight = maxHeight
            val density = LocalDensity.current
            val fontScale = density.fontScale.coerceIn(0.85f, 2.0f)
            val dynamicScale = (screenHeight / 620.dp).coerceIn(0.85f, 1.05f)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 1. COMPACT HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (2 * dynamicScale).dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    BuddhaOutlineLogo(
                        size = 32.dp,
                        tint = null,
                        showEnsoCircle = true
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Text(
                            text = "Buddho Timer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                fontSize = spFor(16f, 1.25f)
                            ),
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = if (isBoxMode) "Mindful Breathwork \u00B7 Box Breathing" else "Mindful Stillness \u00B7 Samadhi",
                            fontSize = spFor(11.5f, 1.22f),
                            color = colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (config.showTimerAmbientShortcut) {
                        IconButton(
                            onClick = { showAmbientDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = "Ambient sound",
                                tint = if (config.ambientSoundsEnabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // --- 2. MODE SELECTOR (near the top) ---
                PracticeModeSelector(
                    isBoxMode = isBoxMode,
                    onSelectMode = { box ->
                        viewModel.toggleBoxBreathingMode(box)
                        leafBurstTrigger++
                    },
                    modifier = Modifier.padding(top = (6 * dynamicScale).dp, bottom = (4 * dynamicScale).dp)
                )

                // --- 4. SCROLLABLE SETUP CONTENT (centered when there is room) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isBoxMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                        },
                        label = "PracticeModeSetup",
                        modifier = Modifier.fillMaxSize()
                    ) { box ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(setupScroll),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                (11.dp * dynamicScale).coerceIn(8.dp, 14.dp),
                                Alignment.CenterVertically
                            )
                        ) {
                            if (!box) {
                                // ===== MEDITATION SETUP =====
                                DurationDisplay(
                                    valueText = "${config.targetDurationMinutes}",
                                    caption = if (config.targetDurationMinutes == 1) "MINUTE" else "MINUTES",
                                    showHint = !hasSeenDurationHint,
                                    onEdit = {
                                        viewModel.dismissDurationHint()
                                        showMeditationDurationDialog = true
                                    }
                                )

                                ZenBranchSlider(
                                    value = config.targetDurationMinutes.toFloat().coerceIn(1f, 120f),
                                    onValueChange = { newMin -> viewModel.updateDurationMinutes(newMin.toInt()) },
                                    valueRange = 1f..120f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((40.dp * dynamicScale).coerceIn(34.dp, 44.dp))
                                )

                                PresetRow(
                                    presets = listOf(5, 15, 30, 45, 60),
                                    selected = config.targetDurationMinutes,
                                    onSelect = viewModel::updateDurationMinutes
                                )

                                SettingSwitchRow(
                                    label = "Do Not Disturb",
                                    subtitle = when {
                                        config.isAutoDndEnabled && !isDndGranted -> "Grant notification access"
                                        else -> "Mute notifications during your sit"
                                    },
                                    subtitleIsWarning = config.isAutoDndEnabled && !isDndGranted,
                                    checked = config.isAutoDndEnabled,
                                    onCheckedChange = {
                                        viewModel.toggleAutoDnd(it)
                                        leafBurstTrigger++
                                    }
                                )
                                SettingSwitchRow(
                                    label = "Continue Past Target",
                                    subtitle = "Keep the timer going past the set time",
                                    checked = config.isOverrunEnabled,
                                    onCheckedChange = {
                                        viewModel.toggleOverrun(it)
                                        leafBurstTrigger++
                                    }
                                )
                            } else {
                                // ===== BOX BREATHING SETUP =====
                                val cycleDurationSec = selectedPattern.totalCycleSec
                                val estimatedCycles = (breathingDurationMin * 60) / cycleDurationSec.coerceAtLeast(1)

                                DurationDisplay(
                                    valueText = "$breathingDurationMin",
                                    caption = "$estimatedCycles CYCLES \u00B7 $breathingDurationMin MINUTES",
                                    showHint = !hasSeenDurationHint,
                                    onEdit = {
                                        viewModel.dismissDurationHint()
                                        showBreathingDurationDialog = true
                                    }
                                )

                                ZenBranchSlider(
                                    value = breathingDurationMin.toFloat().coerceIn(1f, 30f),
                                    onValueChange = { newMin -> viewModel.updateBreathingDurationMinutes(newMin.toInt()) },
                                    valueRange = 1f..30f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((40.dp * dynamicScale).coerceIn(34.dp, 44.dp))
                                )

                                PresetRow(
                                    presets = listOf(2, 5, 10, 15, 20),
                                    selected = breathingDurationMin,
                                    onSelect = viewModel::updateBreathingDurationMinutes
                                )

                                BreathingPatternSelector(
                                    selectedPattern = selectedPattern,
                                    onPatternSelected = {
                                        viewModel.updateBreathingPattern(it)
                                        leafBurstTrigger++
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                SettingSwitchRow(
                                    label = "Do Not Disturb",
                                    subtitle = when {
                                        config.isAutoDndEnabled && !isDndGranted -> "Grant notification access"
                                        else -> "Mute notifications during breathwork"
                                    },
                                    subtitleIsWarning = config.isAutoDndEnabled && !isDndGranted,
                                    checked = config.isAutoDndEnabled,
                                    onCheckedChange = {
                                        viewModel.toggleAutoDnd(it)
                                        leafBurstTrigger++
                                    }
                                )
                            }
                        }
                    }
                }

                // --- 5. PRIMARY START BUTTON (anchored above the bottom navigation) ---
                PrimaryStartButton(
                    isBoxMode = isBoxMode,
                    dynamicScale = dynamicScale,
                    onPress = {
                        leafBurstTrigger++
                        if (isBoxMode) {
                            onOpenBreathing()
                        } else {
                            viewModel.startSession()
                            onStartSession()
                        }
                    },
                    modifier = Modifier.padding(top = (4 * dynamicScale).dp, bottom = 2.dp)
                )
            }
        }

        if (showMeditationDurationDialog) {
            ManualDurationInputDialog(
                initialMinutes = config.targetDurationMinutes,
                minMinutes = 1,
                maxMinutes = 180,
                title = "Meditation Duration",
                subtitle = "Enter sit duration in minutes (1\u2013180)",
                onDismiss = { showMeditationDurationDialog = false },
                onConfirm = { minutes -> viewModel.updateDurationMinutes(minutes) }
            )
        }

        if (showBreathingDurationDialog) {
            ManualDurationInputDialog(
                initialMinutes = breathingDurationMin,
                minMinutes = 1,
                maxMinutes = 60,
                title = "Breathwork Duration",
                subtitle = "Enter breathwork duration in minutes (1\u201360)",
                onDismiss = { showBreathingDurationDialog = false },
                onConfirm = { minutes -> viewModel.updateBreathingDurationMinutes(minutes) }
            )
        }

        if (showAmbientDialog) {
            AmbientSoundPickerDialog(
                config = config,
                isBoxMode = isBoxMode,
                onToggleEnabled = { viewModel.toggleAmbientSounds(it) },
                onSelectSound = { title, res -> viewModel.updateAmbientSound(title, res) },
                onSelectGuidedMeditation = { title, res -> viewModel.updateGuidedMeditation(title, res) },
                onDismiss = { showAmbientDialog = false }
            )
        }
    }
}

/** In-place ambient audio control surfaced from the timer setup music icon. */
@Composable
private fun AmbientSoundPickerDialog(
    config: TimerConfiguration,
    isBoxMode: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectSound: (String, String) -> Unit,
    onSelectGuidedMeditation: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val previewEngine = remember { AmbientSoundEngine(context) }
    var previewingId by remember { mutableStateOf<String?>(null) }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose { previewEngine.stop() }
    }
    fun stopPreview() {
        previewEngine.stop()
        previewingId = null
    }

    AlertDialog(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        containerColor = colorScheme.surfaceContainerHigh,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Soundscape",
                    fontSize = spFor(17f, 1.2f),
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBoxMode) "Ambient during breathwork" else "Play during practice",
                            fontSize = spFor(13.5f, 1.2f),
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                !config.ambientSoundsEnabled -> "Off"
                                config.meditationAudioMode == "guided" && !isBoxMode -> "Guided meditation starts with the session"
                                else -> "Ambient sound plays softly in the background"
                            },
                            fontSize = spFor(11f, 1.2f),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.ambientSoundsEnabled,
                        onCheckedChange = {
                            onToggleEnabled(it)
                            if (!it) stopPreview()
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colorScheme.primary,
                            checkedThumbColor = colorScheme.onPrimary,
                            uncheckedThumbColor = colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = colorScheme.surfaceContainerHighest
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AudioSectionHeader(
                    title = "AMBIENT SOUNDS",
                    subtitle = config.ambientSoundName,
                    expanded = expandedSection == "ambient",
                    onClick = { expandedSection = if (expandedSection == "ambient") null else "ambient" }
                )

                if (expandedSection == "ambient") {
                    Spacer(modifier = Modifier.height(4.dp))
                    SoundHelper.ambientSoundOptions.forEach { option ->
                        val isSelected = config.meditationAudioMode == "ambient" && config.ambientSoundResName == option.id
                        val isPreviewing = previewingId == option.id
                        val canPreview = option.id != AmbientSoundEngine.TYPE_SILENCE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) colorScheme.primary.copy(alpha = 0.14f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    stopPreview()
                                    onSelectSound(option.title, option.id)
                                }
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    fontSize = spFor(13.5f, 1.2f),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                )
                                Text(
                                    text = option.description,
                                    fontSize = spFor(11f, 1.2f),
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (canPreview) {
                                IconButton(
                                    onClick = {
                                        if (isPreviewing) {
                                            stopPreview()
                                        } else {
                                            previewEngine.stop()
                                            previewEngine.start(option.id)
                                            previewingId = option.id
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewing) "Stop preview" else "Preview",
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isBoxMode) {
                    Spacer(modifier = Modifier.height(8.dp))

                    AudioSectionHeader(
                        title = "GUIDED MEDITATIONS",
                        subtitle = config.guidedMeditationName,
                        expanded = expandedSection == "guided",
                        onClick = { expandedSection = if (expandedSection == "guided") null else "guided" }
                    )

                    if (expandedSection == "guided") {
                        Spacer(modifier = Modifier.height(4.dp))
                        SoundHelper.guidedMeditationOptions.forEach { option ->
                            val isSelected = config.meditationAudioMode == "guided" && config.guidedMeditationResName == option.id
                            val isPreviewing = previewingId == option.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) colorScheme.primary.copy(alpha = 0.14f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        stopPreview()
                                        onSelectGuidedMeditation(option.title, option.id)
                                    }
                                    .defaultMinSize(minHeight = 48.dp)
                                    .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        fontSize = spFor(13.5f, 1.2f),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                    )
                                    Text(
                                        text = option.description,
                                        fontSize = spFor(11f, 1.2f),
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (isPreviewing) {
                                            stopPreview()
                                        } else {
                                            previewEngine.stop()
                                            previewEngine.start(option.id, loop = false)
                                            previewingId = option.id
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewing) "Stop preview" else "Preview",
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Guided meditations are hidden during box breathing so the breath cadence stays clear.",
                        fontSize = spFor(11f, 1.2f),
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                stopPreview()
                onDismiss()
            }) {
                Text(
                    text = "Done",
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun AudioSectionHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = spFor(10.5f, 1.2f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = colorScheme.primary
            )
            Text(
                text = subtitle,
                fontSize = spFor(11f, 1.2f),
                color = colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Quiet, theme-aware font helper that respects system font scale without blowing out layouts. */
@Composable
private fun spFor(baseSp: Float, maxFontMultiplier: Float = 1.25f, viewportCompression: Float = 1f): TextUnit {
    val density = LocalDensity.current
    val fontScale = density.fontScale.coerceIn(0.85f, 2.0f)
    val clampedMultiplier = fontScale.coerceIn(0.85f, maxFontMultiplier)
    return (baseSp * viewportCompression * (clampedMultiplier / fontScale)).sp
}

/** Medication / Box Breathing segmented selector, quiet secondary-container style. */
@Composable
private fun PracticeModeSelector(
    isBoxMode: Boolean,
    onSelectMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surfaceContainerHigh)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedOption(
            label = "Meditation",
            icon = Icons.Default.SelfImprovement,
            selected = !isBoxMode,
            onClick = { if (isBoxMode) onSelectMode(false) },
            modifier = Modifier.weight(1f)
        )
        SegmentedOption(
            label = "Box Breathing",
            icon = Icons.Outlined.Air,
            selected = isBoxMode,
            onClick = { if (!isBoxMode) onSelectMode(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val fg = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = spFor(13.5f, 1.18f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Prominent duration number with a subtle pencil edit affordance and a single quiet hint line. */
@Composable
private fun DurationDisplay(
    valueText: String,
    caption: String,
    showHint: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale.coerceIn(0.85f, 2.0f)
    val durationFont = spFor(56f, 1.18f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = valueText,
                fontSize = durationFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit duration",
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = caption,
            fontSize = spFor(11.5f, 1.2f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.2.sp,
            color = colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        if (showHint && fontScale <= 1.3f) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tap to set a custom time",
                fontSize = spFor(10.5f, 1.2f),
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Evenly spaced duration presets; unselected presets stay visually quiet. */
@Composable
private fun PresetRow(
    presets: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        presets.forEach { value ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) colorScheme.primary.copy(alpha = 0.16f)
                        else colorScheme.surfaceContainerHigh
                    )
                    .then(
                        if (isSelected) Modifier.border(1.dp, colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .clickable { onSelect(value) }
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${value}m",
                    fontSize = spFor(13.5f, 1.22f),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/** Compact setting row: quiet label left, unambiguous theme-aware Switch right. */
@Composable
private fun SettingSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitleIsWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = spFor(13.5f, 1.2f),
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = spFor(11f, 1.2f),
                color = if (subtitleIsWarning) colorScheme.error else colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colorScheme.primary,
                checkedThumbColor = colorScheme.onPrimary,
                checkedBorderColor = colorScheme.primary,
                uncheckedThumbColor = colorScheme.onSurfaceVariant,
                uncheckedTrackColor = colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = colorScheme.outline
            )
        )
    }
}

/** Technique card: clear title/category plus wrapping phase chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BreathingPatternSelector(
    selectedPattern: BreathingPattern,
    onPatternSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var isDropdownOpen by remember { mutableStateOf(false) }

    val patternCategory = when (selectedPattern.id) {
        "box" -> "Focus & Stress Reset"
        "478" -> "Deep Sleep & Relaxation"
        "resonant" -> "Heart Coherence (5-5)"
        "gentle_box" -> "Beginner Friendly"
        else -> "Anti-Autopilot Settling"
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isDropdownOpen = true },
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedPattern.name,
                            fontSize = spFor(14.5f, 1.25f),
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = patternCategory,
                            fontSize = spFor(10.5f, 1.2f),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp,
                            color = colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (isDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Select Pattern",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Phase chips wrap cleanly on narrow screens / large fonts
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (selectedPattern.id) {
                        "box" -> {
                            PhaseChip("4s Inhale", colorScheme.primary)
                            PhaseChip("4s Hold", colorScheme.primary)
                            PhaseChip("4s Exhale", colorScheme.tertiary)
                            PhaseChip("4s Hold", colorScheme.secondary)
                        }
                        "478" -> {
                            PhaseChip("4s Inhale", colorScheme.primary)
                            PhaseChip("7s Hold", colorScheme.primary)
                            PhaseChip("8s Exhale", colorScheme.tertiary)
                        }
                        "resonant" -> {
                            PhaseChip("5s Inhale", colorScheme.primary)
                            PhaseChip("5s Exhale", colorScheme.tertiary)
                        }
                        else -> {
                            PhaseChip(selectedPattern.inhaleSec.toString() + "s Inhale", colorScheme.primary)
                            if (selectedPattern.holdFullSec > 0) {
                                PhaseChip(selectedPattern.holdFullSec.toString() + "s Hold", colorScheme.primary)
                            }
                            PhaseChip(selectedPattern.exhaleSec.toString() + "s Exhale", colorScheme.tertiary)
                            if (selectedPattern.holdEmptySec > 0) {
                                PhaseChip(selectedPattern.holdEmptySec.toString() + "s Hold", colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = isDropdownOpen,
            onDismissRequest = { isDropdownOpen = false },
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .background(colorScheme.surfaceContainerHigh)
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(vertical = 4.dp)
        ) {
            BreathingPattern.allPatterns.forEach { item ->
                val itemSelected = selectedPattern.id == item.id
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
                                    fontWeight = if (itemSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (itemSelected) colorScheme.primary else colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.5.sp,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (itemSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(colorScheme.primary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Active Pattern",
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        isDropdownOpen = false
                        onPatternSelected(item.id)
                    },
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (itemSelected) colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                )
            }
        }
    }
}

/** Slim phase chip (quiet, but clearly readable). */
@Composable
private fun PhaseChip(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = spFor(11f, 1.2f),
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Primary action button, gradient derived from the active Material You accent. */
@Composable
private fun PrimaryStartButton(
    isBoxMode: Boolean,
    dynamicScale: Float,
    onPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val buttonGradient = Brush.verticalGradient(
        colors = listOf(
            colorScheme.primary,
            colorScheme.primary.copy(alpha = 0.82f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = (52.dp * dynamicScale).coerceIn(48.dp, 56.dp))
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = colorScheme.primary.copy(alpha = 0.30f))
            .clip(RoundedCornerShape(18.dp))
            .background(buttonGradient)
            .clickable(onClick = onPress)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isBoxMode) Icons.Outlined.Air else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isBoxMode) "Begin Box Breathing" else "Begin Practice",
                fontSize = spFor(16f, 1.2f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = colorScheme.onPrimary
            )
        }
    }
}
