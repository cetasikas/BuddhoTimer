package com.samadhi.meditationtimer.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
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
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.service.SoundHelper
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.screens.timer.TimerViewModel
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTealLight
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    timerViewModel: TimerViewModel,
    onNavigateBack: () -> Unit = {},
    onShowTutorial: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
        // Moonlit Backdrop
        Image(
            painter = painterResource(id = R.drawable.bg_zen_bamboo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.20f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060911).copy(alpha = 0.85f),
                            Color(0xFF0A101D).copy(alpha = 0.70f),
                            Color(0xFF060911).copy(alpha = 0.92f)
                        )
                    )
                )
        )

        FallingLeavesEffect(
            leafCount = 14,
            ambientAlpha = 0.26f
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
                        tint = ZenTealLight,
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
                        text = "Chimes, warm-up, permissions, and backups",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 1. BELL SOUNDS
            Text(
                text = "BELL SOUND & CHIMES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = ZenTeal
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
                                tint = ZenTealLight,
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTealLight),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(ZenTeal))
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
                                    selectedContainerColor = ZenTeal,
                                    selectedLabelColor = ZenObsidian
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) ZenTealLight else ZenBorder,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. PREPARATION TIME (WARM UP)
            Text(
                text = "PREPARATION (WARM UP)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = ZenTeal
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
                                    selectedContainerColor = ZenTeal,
                                    selectedLabelColor = ZenObsidian
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) ZenTealLight else ZenBorder,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. SYSTEM PERMISSIONS
            Text(
                text = "SYSTEM PERMISSIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = ZenTeal
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
                                tint = if (isDndGranted) ZenTealLight else ZenTextMuted,
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
                                    color = if (isDndGranted) ZenTealLight else ZenTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isDndGranted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Granted",
                                tint = ZenTealLight,
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
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(ZenTeal))
                            ) {
                                Text("Grant", fontSize = 12.sp, color = ZenTealLight, fontWeight = FontWeight.Medium)
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
                                tint = if (isBatteryOptIgnored) ZenTealLight else ZenTextMuted,
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
                                    color = if (isBatteryOptIgnored) ZenTealLight else ZenTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isBatteryOptIgnored) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Ignored",
                                tint = ZenTealLight,
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
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(ZenTeal))
                            ) {
                                Text("Disable", fontSize = 12.sp, color = ZenTealLight, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. TUTORIAL & APP GUIDE
            Text(
                text = "TUTORIAL & HELP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = ZenTeal
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onShowTutorial() },
                colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenTeal.copy(alpha = 0.35f))),
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
                                .background(ZenTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ZenTealLight,
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
                                text = "Interactive walkthrough of features and permissions",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = ZenTextSecondary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onShowTutorial,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ZenTeal,
                            contentColor = ZenObsidian
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. DATA BACKUP & EXPORT
            Text(
                text = "DATA BACKUP & PORTABILITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = ZenTeal
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
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(ZenTeal))
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ZenTealLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 11.sp, color = ZenTealLight)
                        }

                        OutlinedButton(
                            onClick = { exportCsvLauncher.launch("buddho_sessions_${System.currentTimeMillis()}.csv") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(ZenTeal))
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = ZenTealLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 11.sp, color = ZenTealLight)
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
