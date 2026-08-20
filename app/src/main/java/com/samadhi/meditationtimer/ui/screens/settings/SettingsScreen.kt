package com.samadhi.meditationtimer.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.service.SoundHelper
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.screens.timer.TimerViewModel
import com.samadhi.meditationtimer.ui.theme.BambooTealTheme
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.SacredLotusTheme
import com.samadhi.meditationtimer.ui.theme.SakuraTwilightTheme
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary
import com.samadhi.meditationtimer.ui.theme.ZenThemeId

@Composable
private fun ExpandableSettingsCard(
    title: String,
    icon: ImageVector,
    summaryValue: String? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentTheme = LocalZenTheme.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.82f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isExpanded) currentTheme.primary.copy(alpha = 0.55f) else ZenBorder)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(currentTheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = currentTheme.primaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ZenTextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isExpanded && summaryValue != null) {
                        Text(
                            text = summaryValue,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentTheme.primaryLight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentTheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = ZenTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    timerViewModel: TimerViewModel,
    onNavigateBack: () -> Unit,
    onShowTutorial: () -> Unit,
    onSelectTheme: (ZenThemeId) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    val context = LocalContext.current
    val soundHelper = remember { SoundHelper(context) }
    val config by timerViewModel.config.collectAsState()
    val isDndGranted by timerViewModel.isDndGranted.collectAsState()
    val isBatteryOptIgnored by timerViewModel.isBatteryOptIgnored.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Expandable accordion states (Default compact/collapsed)
    var expandedTheme by remember { mutableStateOf(false) }
    var expandedChime by remember { mutableStateOf(false) }
    var expandedInterval by remember { mutableStateOf(false) }
    var expandedDisplay by remember { mutableStateOf(false) }
    var expandedReminder by remember { mutableStateOf(false) }
    var expandedPrep by remember { mutableStateOf(false) }
    var expandedPermissions by remember { mutableStateOf(false) }
    var expandedBackup by remember { mutableStateOf(false) }

    // Reminder state
    val reminderPrefs = remember { context.getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE) }
    var reminderEnabled by remember { mutableStateOf(reminderPrefs.getBoolean("daily_reminder_enabled", false)) }
    var reminderMode by remember { mutableStateOf(reminderPrefs.getString("reminder_mode", "daily") ?: "daily") }
    var reminderHour by remember { mutableIntStateOf(reminderPrefs.getInt("reminder_hour", 8)) }
    var reminderIntervalHours by remember { mutableIntStateOf(reminderPrefs.getInt("reminder_interval_hours", 4)) }

    val postNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                reminderEnabled = true
                reminderPrefs.edit().putBoolean("daily_reminder_enabled", true).apply()
                when (reminderMode) {
                    "random" -> com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleRandomDailyReminder(context)
                    "interval" -> com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleIntervalReminder(context, reminderIntervalHours)
                    else -> com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleDailyReminder(context, reminderHour, 0)
                }
            }
        }
    )

    // Document Launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.exportJsonBackup(it) } }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? -> uri?.let { viewModel.exportCsvBackup(it) } }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importJsonBackup(it) } }

    DisposableEffect(Unit) {
        timerViewModel.refreshPermissions()
        onDispose { }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        FallingLeavesEffect(leafCount = 14, ambientAlpha = 0.30f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Header with Back Button & Donate in Top Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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

                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = "Preferences & Stillness",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZenTextSecondary
                        )
                    }
                }

                // Support / Donate Button in Top Right of Settings
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ZenSurface.copy(alpha = 0.85f))
                        .border(1.dp, currentTheme.primary.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://ko-fi.com/hackysackapp")
                            ).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Donate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = currentTheme.primaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- COMPACT ACCORDION SECTIONS ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. ATMOSPHERE & THEME
                ExpandableSettingsCard(
                    title = "Atmosphere & Theme",
                    icon = Icons.Default.Palette,
                    summaryValue = currentTheme.title,
                    isExpanded = expandedTheme,
                    onToggle = { expandedTheme = !expandedTheme }
                ) {
                    val availableThemes = listOf(BambooTealTheme, SacredLotusTheme, SakuraTwilightTheme)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableThemes.forEach { theme ->
                            val isSelected = currentTheme.id == theme.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectTheme(theme.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) theme.primary.copy(alpha = 0.18f) else ZenSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = SolidColor(if (isSelected) theme.primaryLight else ZenBorder)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(theme.primaryLight)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = theme.title,
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) theme.primaryLight else ZenTextPrimary
                                            )
                                            Text(
                                                text = theme.id.subtitle,
                                                fontSize = 11.sp,
                                                color = ZenTextSecondary
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = theme.primaryLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. BELL SOUND & CHIMES
                val currentSoundTitle = SoundHelper.soundOptions.find { it.id == config.soundBellResName }?.title ?: "Singing Bowl"
                ExpandableSettingsCard(
                    title = "Bell Sound & Chimes",
                    icon = Icons.Default.MusicNote,
                    summaryValue = currentSoundTitle,
                    isExpanded = expandedChime,
                    onToggle = { expandedChime = !expandedChime }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Start & Finish Chime",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZenTextPrimary
                            )
                            OutlinedButton(
                                onClick = { soundHelper.playChime(config.soundBellResName) },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(15.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                            ) {
                                Text("Preview", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SoundHelper.soundOptions.forEach { soundOption ->
                                val isSelected = config.soundBellResName == soundOption.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        timerViewModel.updateSoundBell(soundOption.id)
                                        soundHelper.playChime(soundOption.id)
                                    },
                                    label = { Text(soundOption.title, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = ZenSurfaceVariant,
                                        labelColor = ZenTextSecondary,
                                        selectedContainerColor = currentTheme.primary,
                                        selectedLabelColor = ZenObsidian
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) currentTheme.primaryLight else ZenBorder,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. MINDFULNESS INTERVAL BELL
                val intervalSummary = if (config.mindfulnessIntervalMinutes == 0) "Off" else "${config.mindfulnessIntervalMinutes}m"
                ExpandableSettingsCard(
                    title = "Mindfulness Interval Bell",
                    icon = Icons.Default.Timer,
                    summaryValue = intervalSummary,
                    isExpanded = expandedInterval,
                    onToggle = { expandedInterval = !expandedInterval }
                ) {
                    val mindfulnessOptions = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30)
                    Column {
                        Text(
                            text = "Periodic chimes return awareness to the present moment.",
                            fontSize = 11.5.sp,
                            color = ZenTextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mindfulnessOptions.forEach { intervalMin ->
                                val isSelected = config.mindfulnessIntervalMinutes == intervalMin
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { timerViewModel.updateMindfulnessInterval(intervalMin) },
                                    label = {
                                        Text(
                                            text = if (intervalMin == 0) "Off" else "${intervalMin}m",
                                            fontSize = 11.5.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = ZenSurfaceVariant,
                                        labelColor = ZenTextSecondary,
                                        selectedContainerColor = currentTheme.primary,
                                        selectedLabelColor = ZenObsidian
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) currentTheme.primaryLight else ZenBorder,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. DISPLAY & SCREEN TIMEOUT
                val displaySummary = if (config.tapToBlackoutEnabled) "Blackout" else if (config.keepScreenAwake) "Awake" else "Standard"
                ExpandableSettingsCard(
                    title = "Display & Screen Timeout",
                    icon = Icons.Default.DarkMode,
                    summaryValue = displaySummary,
                    isExpanded = expandedDisplay,
                    onToggle = { expandedDisplay = !expandedDisplay }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Tap to Blackout Screen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ZenTextPrimary)
                                Text("Tap during sits for AMOLED black", fontSize = 11.sp, color = ZenTextSecondary)
                            }
                            Switch(
                                checked = config.tapToBlackoutEnabled,
                                onCheckedChange = { timerViewModel.toggleTapToBlackout(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = ZenObsidian, checkedTrackColor = currentTheme.primary)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Keep Screen Awake", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ZenTextPrimary)
                                Text("Prevent sleep during active sessions", fontSize = 11.sp, color = ZenTextSecondary)
                            }
                            Switch(
                                checked = config.keepScreenAwake,
                                onCheckedChange = { timerViewModel.toggleKeepScreenAwake(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = ZenObsidian, checkedTrackColor = currentTheme.primary)
                            )
                        }
                    }
                }

                // 5. MEDITATION REMINDERS (OUTSIDE APP)
                val reminderSummary = if (!reminderEnabled) "Off" else if (reminderMode == "random") "Random Waking" else if (reminderMode == "interval") "Every ${reminderIntervalHours}h" else "${if (reminderHour == 0) 12 else if (reminderHour <= 12) reminderHour else reminderHour - 12}${if (reminderHour < 12) "AM" else "PM"}"
                ExpandableSettingsCard(
                    title = "Meditation Reminders",
                    icon = Icons.Default.NotificationsActive,
                    summaryValue = reminderSummary,
                    isExpanded = expandedReminder,
                    onToggle = { expandedReminder = !expandedReminder }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Mindfulness Reminders", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ZenTextPrimary)
                                Text("Gentle notifications with Buddhist wisdom quotes", fontSize = 11.sp, color = ZenTextSecondary)
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (!hasPermission) {
                                            postNotificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                            return@Switch
                                        }
                                    }

                                    reminderEnabled = isChecked
                                    reminderPrefs.edit().putBoolean("daily_reminder_enabled", isChecked).apply()

                                    if (isChecked) {
                                        when (reminderMode) {
                                            "random" -> com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleRandomDailyReminder(context)
                                            "interval" -> com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleIntervalReminder(context, reminderIntervalHours)
                                            else -> com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleDailyReminder(context, reminderHour, 0)
                                        }
                                    } else {
                                        com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.cancelReminder(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ZenObsidian, checkedTrackColor = currentTheme.primary)
                            )
                        }

                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Mode selector — use FlowRow so chips wrap instead of squishing
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = reminderMode == "daily",
                                    onClick = {
                                        reminderMode = "daily"
                                        reminderPrefs.edit().putString("reminder_mode", "daily").apply()
                                        com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleDailyReminder(context, reminderHour, 0)
                                    },
                                    label = { Text("Daily Fixed", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = currentTheme.primary,
                                        selectedLabelColor = ZenObsidian
                                    )
                                )

                                FilterChip(
                                    selected = reminderMode == "random",
                                    onClick = {
                                        reminderMode = "random"
                                        reminderPrefs.edit().putString("reminder_mode", "random").apply()
                                        com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleRandomDailyReminder(context)
                                    },
                                    label = { Text("Random", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = currentTheme.primary,
                                        selectedLabelColor = ZenObsidian
                                    )
                                )

                                FilterChip(
                                    selected = reminderMode == "interval",
                                    onClick = {
                                        reminderMode = "interval"
                                        reminderPrefs.edit().putString("reminder_mode", "interval").apply()
                                        com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleIntervalReminder(context, reminderIntervalHours)
                                    },
                                    label = { Text("Interval", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = currentTheme.primary,
                                        selectedLabelColor = ZenObsidian
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (reminderMode == "random") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(currentTheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "A surprise mindfulness reminder is delivered once a day at a random time during active waking hours (8:00 AM – 9:00 PM).",
                                        fontSize = 11.5.sp,
                                        color = ZenTextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            } else if (reminderMode == "daily") {
                                val hourPresets = listOf(6, 7, 8, 9, 12, 18, 20, 21, 22)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    hourPresets.forEach { h ->
                                        val isSelected = reminderHour == h
                                        val timeLabel = when {
                                            h == 0 -> "12 AM"
                                            h < 12 -> "$h AM"
                                            h == 12 -> "12 PM"
                                            else -> "${h - 12} PM"
                                        }
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                reminderHour = h
                                                reminderPrefs.edit().putInt("reminder_hour", h).apply()
                                                com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleDailyReminder(context, h, 0)
                                            },
                                            label = { Text(timeLabel, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = currentTheme.primary,
                                                selectedLabelColor = ZenObsidian
                                            )
                                        )
                                    }
                                }
                            } else {
                                val intervalOptions = listOf(2, 3, 4, 6, 8, 12)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    intervalOptions.forEach { hrs ->
                                        val isSelected = reminderIntervalHours == hrs
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                reminderIntervalHours = hrs
                                                reminderPrefs.edit().putInt("reminder_interval_hours", hrs).apply()
                                                com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleIntervalReminder(context, hrs)
                                            },
                                            label = { Text("Every ${hrs}h", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = currentTheme.primary,
                                                selectedLabelColor = ZenObsidian
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    val quote = com.samadhi.meditationtimer.data.wisdom.MeditationGuides.wisdomQuotes.random()
                                    val helper = com.samadhi.meditationtimer.service.NotificationHelper(context)
                                    helper.showReminderNotification("Time for Stillness 🪷", "“${quote.quote}” — ${quote.author}")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(ZenBorder))
                            ) {
                                Text("Send Test Reminder Notification", fontSize = 11.5.sp, color = currentTheme.primaryLight)
                            }
                        }
                    }
                }

                // 6. WARM-UP PREPARATION
                val prepSummary = if (config.prepTimeSeconds == 0) "None" else "${config.prepTimeSeconds}s"
                val prepOptions = listOf(0, 5, 10, 15)
                ExpandableSettingsCard(
                    title = "Warm-up Preparation",
                    icon = Icons.Default.Timer,
                    summaryValue = prepSummary,
                    isExpanded = expandedPrep,
                    onToggle = { expandedPrep = !expandedPrep }
                ) {
                    Column {
                        Text("Posture adjustment countdown before timer begins.", fontSize = 11.5.sp, color = ZenTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            prepOptions.forEach { prepSec ->
                                val isSelected = config.prepTimeSeconds == prepSec
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { timerViewModel.updatePrepTimeSeconds(prepSec) },
                                    label = { Text(if (prepSec == 0) "None" else "${prepSec}s", fontSize = 11.5.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = currentTheme.primary,
                                        selectedLabelColor = ZenObsidian
                                    )
                                )
                            }
                        }
                    }
                }

                // 7. SYSTEM PERMISSIONS
                val permSummary = if (isDndGranted && isBatteryOptIgnored) "Optimized" else "Action Needed"
                ExpandableSettingsCard(
                    title = "System Permissions",
                    icon = Icons.Default.Security,
                    summaryValue = permSummary,
                    isExpanded = expandedPermissions,
                    onToggle = { expandedPermissions = !expandedPermissions }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Do Not Disturb Access", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ZenTextPrimary)
                                Text(if (isDndGranted) "Auto-silence enabled" else "Required to silence notifications", fontSize = 11.sp, color = ZenTextSecondary)
                            }
                            if (isDndGranted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = currentTheme.primaryLight, modifier = Modifier.size(20.dp))
                            } else {
                                OutlinedButton(
                                    onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) },
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(15.dp),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                                ) {
                                    Text("Grant", fontSize = 11.5.sp, color = currentTheme.primaryLight)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Background Execution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ZenTextPrimary)
                                Text(if (isBatteryOptIgnored) "Safe from Android sleep kill" else "Recommended for long sits", fontSize = 11.sp, color = ZenTextSecondary)
                            }
                            if (isBatteryOptIgnored) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = currentTheme.primaryLight, modifier = Modifier.size(20.dp))
                            } else {
                                OutlinedButton(
                                    onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) },
                                    modifier = Modifier.wrapContentSize(),
                                    shape = RoundedCornerShape(15.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                                ) {
                                    Text("Disable", fontSize = 11.5.sp, color = currentTheme.primaryLight)
                                }
                            }
                        }
                    }
                }

                // 8. DATA BACKUP & EXPORT
                ExpandableSettingsCard(
                    title = "Data Backup & Portability",
                    icon = Icons.Default.CloudUpload,
                    summaryValue = "Offline Backups",
                    isExpanded = expandedBackup,
                    onToggle = { expandedBackup = !expandedBackup }
                ) {
                    Column {
                        Text("Export or restore your meditation sessions and journal notes in JSON or CSV format.", fontSize = 11.5.sp, color = ZenTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { exportJsonLauncher.launch("buddho_backup_${System.currentTimeMillis()}.json") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = currentTheme.primaryLight, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON", fontSize = 11.sp, color = currentTheme.primaryLight)
                            }

                            OutlinedButton(
                                onClick = { exportCsvLauncher.launch("buddho_sessions_${System.currentTimeMillis()}.csv") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = currentTheme.primaryLight, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", fontSize = 11.sp, color = currentTheme.primaryLight)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FilledTonalButton(
                            onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = ZenSurfaceVariant, contentColor = ZenTextPrimary)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore from JSON Backup", fontSize = 11.5.sp)
                        }
                    }
                }

                // 9. TUTORIAL QUICK ACCESS
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onShowTutorial() },
                    colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.82f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = currentTheme.primaryLight, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Show Tutorial Tour", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ZenTextPrimary)
                            Text("3-step feature & atmosphere walkthrough", fontSize = 11.sp, color = ZenTextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
