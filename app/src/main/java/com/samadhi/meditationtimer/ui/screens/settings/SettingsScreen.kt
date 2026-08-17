package com.samadhi.meditationtimer.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    timerViewModel: TimerViewModel,
    onNavigateBack: () -> Unit = {},
    onShowTutorial: () -> Unit = {},
    onSelectTheme: (ZenThemeId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalZenTheme.current
    val isDndGranted by viewModel.isDndGranted.collectAsState()
    val isBatteryOptIgnored by viewModel.isBatteryOptIgnored.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val config by timerViewModel.config.collectAsState()

    val context = LocalContext.current
    val soundHelper = remember { SoundHelper(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val prepOptions = listOf(0, 5, 10, 15)

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

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // SAF Document Launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportJsonBackup(it) }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportCsvBackup(it) }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importJsonBackup(it) }
    }

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        // Subtle Falling Particles on pure AMOLED black
        FallingLeavesEffect(
            leafCount = 14,
            ambientAlpha = 0.30f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                        text = "Meditation Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = "Themes, chimes, permissions, and backups",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 1. ATMOSPHERE & THEMES
            Text(
                text = "ATMOSPHERE & THEME",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            val availableThemes = listOf(BambooTealTheme, SacredLotusTheme, SakuraTwilightTheme)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableThemes.forEach { theme ->
                    val isSelected = currentTheme.id == theme.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectTheme(theme.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) theme.primary.copy(alpha = 0.18f) else ZenSurface.copy(alpha = 0.75f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = SolidColor(if (isSelected) theme.primaryLight else ZenBorder)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(theme.primaryLight)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = theme.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) theme.primaryLight else ZenTextPrimary
                                    )
                                    Text(
                                        text = theme.id.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = ZenTextSecondary
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = theme.primaryLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Spacer(modifier = Modifier.height(22.dp))

            // 2. BELL SOUNDS
            Text(
                text = "BELL SOUND & CHIMES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = currentTheme.primaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start & End Chime",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = ZenTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { soundHelper.playChime(config.soundBellResName) },
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(17.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryLight),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                        ) {
                            Text("Preview", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundHelper.soundOptions.forEach { soundOption ->
                            val isSelected = config.soundBellResName == soundOption.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    timerViewModel.updateSoundBell(soundOption.id)
                                    soundHelper.playChime(soundOption.id)
                                },
                                label = { Text(soundOption.title, fontSize = 12.sp) },
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

            Spacer(modifier = Modifier.height(20.dp))

            // 3. MINDFULNESS INTERVAL BELL
            Text(
                text = "MINDFULNESS INTERVAL BELL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            val mindfulnessOptions = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Periodic Mindfulness Chime",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = "Chimes periodically during meditation to gently return your awareness to the present moment",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = ZenTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                                        fontSize = 12.sp
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

            Spacer(modifier = Modifier.height(20.dp))

            // 4. DISPLAY & PRACTICE PREFERENCES
            Text(
                text = "DISPLAY & PRACTICE PREFERENCES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tap to Blackout Screen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tap to Blackout Screen",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = ZenTextPrimary
                            )
                            Text(
                                text = "Tap anywhere during meditation to turn the screen pure AMOLED black, tap again to wake",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = ZenTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = config.tapToBlackoutEnabled,
                            onCheckedChange = { timerViewModel.toggleTapToBlackout(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ZenObsidian,
                                checkedTrackColor = currentTheme.primary,
                                uncheckedThumbColor = ZenTextMuted,
                                uncheckedTrackColor = ZenSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Keep Screen Awake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep Screen Awake",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = ZenTextPrimary
                            )
                            Text(
                                text = "Keep the display turned on during active meditation and box breathing",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = ZenTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = config.keepScreenAwake,
                            onCheckedChange = { timerViewModel.toggleKeepScreenAwake(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ZenObsidian,
                                checkedTrackColor = currentTheme.primary,
                                uncheckedThumbColor = ZenTextMuted,
                                uncheckedTrackColor = ZenSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. MEDITATION REMINDERS (OUTSIDE APP)
            Text(
                text = "MEDITATION REMINDERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                        if (reminderMode == "interval") {
                            com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleIntervalReminder(context, reminderIntervalHours)
                        } else {
                            com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleDailyReminder(context, reminderHour, 0)
                        }
                    }
                }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remind Me to Meditate",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = ZenTextPrimary
                            )
                            Text(
                                text = "Send gentle mindfulness notifications outside the app",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = ZenTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
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
                                    if (reminderMode == "interval") {
                                        com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleIntervalReminder(context, reminderIntervalHours)
                                    } else {
                                        com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleDailyReminder(context, reminderHour, 0)
                                    }
                                } else {
                                    com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.cancelReminder(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ZenObsidian,
                                checkedTrackColor = currentTheme.primary,
                                uncheckedThumbColor = ZenTextMuted,
                                uncheckedTrackColor = ZenSurfaceVariant
                            )
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Reminder Frequency Mode (Daily vs Interval)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = reminderMode == "daily",
                                onClick = {
                                    reminderMode = "daily"
                                    reminderPrefs.edit().putString("reminder_mode", "daily").apply()
                                    com.samadhi.meditationtimer.service.reminder.MeditationReminderScheduler.scheduleDailyReminder(context, reminderHour, 0)
                                },
                                label = { Text("Daily Schedule", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ZenSurfaceVariant,
                                    labelColor = ZenTextSecondary,
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
                                label = { Text("Periodic Interval", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ZenSurfaceVariant,
                                    labelColor = ZenTextSecondary,
                                    selectedContainerColor = currentTheme.primary,
                                    selectedLabelColor = ZenObsidian
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (reminderMode == "daily") {
                            Text(
                                text = "Reminder Time",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZenTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val hourPresets = listOf(6, 7, 8, 9, 12, 18, 20, 21, 22)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        label = { Text(timeLabel, fontSize = 11.5.sp) },
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
                        } else {
                            Text(
                                text = "Interval Cadence",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZenTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

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
                                        label = { Text("Every ${hrs}h", fontSize = 11.5.sp) },
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Test Notification Button
                        OutlinedButton(
                            onClick = {
                                val quote = com.samadhi.meditationtimer.data.wisdom.MeditationGuides.wisdomQuotes.random()
                                val helper = com.samadhi.meditationtimer.service.NotificationHelper(context)
                                helper.showReminderNotification(
                                    "Time for Stillness 🪷",
                                    "“${quote.quote}” — ${quote.author}"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(ZenBorder))
                        ) {
                            Text("Send Test Reminder Notification", fontSize = 12.sp, color = currentTheme.primaryLight)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. PREPARATION (WARM UP)
            Text(
                text = "PREPARATION (WARM UP)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Countdown before timer starts",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = "Allows you to get into a comfortable meditation posture",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = ZenTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        prepOptions.forEach { prepSec ->
                            val isSelected = config.prepTimeSeconds == prepSec
                            FilterChip(
                                selected = isSelected,
                                onClick = { timerViewModel.updatePrepTimeSeconds(prepSec) },
                                label = {
                                    Text(
                                        text = if (prepSec == 0) "None" else "${prepSec}s",
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
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

            Spacer(modifier = Modifier.height(20.dp))

            // 4. SYSTEM PERMISSIONS
            Text(
                text = "SYSTEM PERMISSIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Do Not Disturb & Battery Optimization
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // DND Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoNotDisturb,
                                contentDescription = null,
                                tint = if (isDndGranted) currentTheme.primaryLight else ZenTextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Do Not Disturb Access",
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                                    color = ZenTextPrimary
                                )
                                Text(
                                    text = if (isDndGranted) "Granted (Auto-silence enabled)" else "Permission required for auto-DND",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = if (isDndGranted) currentTheme.primaryLight else ZenTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isDndGranted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Granted",
                                tint = currentTheme.primaryLight,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(17.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                            ) {
                                Text("Grant", fontSize = 12.sp, color = currentTheme.primaryLight, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Battery Optimization Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = if (isBatteryOptIgnored) currentTheme.primaryLight else ZenTextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Background Execution",
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                                    color = ZenTextPrimary
                                )
                                Text(
                                    text = if (isBatteryOptIgnored) "Unrestricted (Safe from OS kill)" else "Recommended for long sessions",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = if (isBatteryOptIgnored) currentTheme.primaryLight else ZenTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isBatteryOptIgnored) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Ignored",
                                tint = currentTheme.primaryLight,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(17.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                            ) {
                                Text("Disable", fontSize = 12.sp, color = currentTheme.primaryLight, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. TUTORIAL & APP GUIDE
            Text(
                text = "TUTORIAL & HELP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onShowTutorial() },
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(currentTheme.primary.copy(alpha = 0.35f))),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = currentTheme.primaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Show Tutorial & Quick Guide",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                color = ZenTextPrimary
                            )
                            Text(
                                text = "Walkthrough of atmosphere, features, and permissions",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = ZenTextSecondary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onShowTutorial,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = currentTheme.primary,
                            contentColor = ZenObsidian
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. DATA BACKUP & EXPORT
            Text(
                text = "DATA BACKUP & PORTABILITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = currentTheme.primaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "100% Offline & Private",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = "Export your meditation logs and reflections locally or restore from backup files.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = ZenTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportJsonLauncher.launch("buddho_backup_${System.currentTimeMillis()}.json") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = currentTheme.primaryLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 11.sp, color = currentTheme.primaryLight)
                        }

                        OutlinedButton(
                            onClick = { exportCsvLauncher.launch("buddho_sessions_${System.currentTimeMillis()}.csv") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(currentTheme.primary))
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = currentTheme.primaryLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 11.sp, color = currentTheme.primaryLight)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ZenSurfaceVariant,
                            contentColor = ZenTextPrimary
                        )
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore from JSON Backup", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
