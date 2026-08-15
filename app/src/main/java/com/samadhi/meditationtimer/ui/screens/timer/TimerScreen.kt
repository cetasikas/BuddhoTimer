package com.samadhi.meditationtimer.ui.screens.timer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.service.SoundHelper
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenAmberDark
import com.samadhi.meditationtimer.ui.theme.ZenAmberGlow
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenDarkBackground
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenOverrunBlue
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val isDndGranted by viewModel.isDndGranted.collectAsState()
    val isBatteryOptIgnored by viewModel.isBatteryOptIgnored.collectAsState()
    val currentQuote by viewModel.currentQuote.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
    val prepOptions = listOf(0, 5, 10, 15)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZenObsidian)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // App Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = ZenAmber,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "BUDDHO",
                style = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold,
                    color = ZenAmber
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pre-Session Daily Wisdom Reflection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { viewModel.cycleNextQuote() },
            colors = CardDefaults.cardColors(containerColor = ZenSurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(ZenAmber.copy(alpha = 0.35f), ZenBorder)
                )
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ZenAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CONTEMPLATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = ZenAmber
                        )
                    }
                    IconButton(
                        onClick = { viewModel.cycleNextQuote() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Next Reflection",
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${currentQuote.quote}\"",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 19.sp,
                    color = ZenTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "— ${currentQuote.author} (${currentQuote.sourceOrTradition})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZenAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Target Duration Display Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ZenSurfaceVariant, ZenSurface)
                    )
                )
                .border(1.dp, ZenBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 28.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (config.isOpenEnded) "OPEN PRACTICE (COUNT UP)" else "TARGET DURATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = ZenTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (config.isOpenEnded) {
                    Text(
                        text = "∞",
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Light,
                        color = ZenTeal
                    )
                    Text(
                        text = "Open stopwatch • Count up freely",
                        fontSize = 12.sp,
                        color = ZenTextMuted
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${config.targetDurationMinutes}",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Light,
                            color = ZenAmber
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "min",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = ZenTextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Text(
                        text = "Auto-continues into Samadhi Overrun when time ends",
                        fontSize = 11.sp,
                        color = ZenOverrunBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Preset Chips
        if (!config.isOpenEnded) {
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelLarge,
                color = ZenTextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetDurations.forEach { duration ->
                    val isSelected = config.targetDurationMinutes == duration
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateDurationMinutes(duration) },
                        label = { Text("${duration}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ZenSurface,
                            labelColor = ZenTextSecondary,
                            selectedContainerColor = ZenAmber,
                            selectedLabelColor = ZenObsidian
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) ZenAmber else ZenBorder,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Duration Slider
            Slider(
                value = config.targetDurationMinutes.toFloat(),
                onValueChange = { viewModel.updateDurationMinutes(it.toInt()) },
                valueRange = 1f..120f,
                steps = 118,
                colors = SliderDefaults.colors(
                    thumbColor = ZenAmber,
                    activeTrackColor = ZenAmber,
                    inactiveTrackColor = ZenSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sound & Bell Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ZenSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ZenBorder)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = ZenAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Meditation Chime",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = ZenTextPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.soundHelper.playChime(config.soundBellResName) },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenAmber),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(ZenAmber))
                    ) {
                        Text("Preview", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                viewModel.updateSoundBell(soundOption.id)
                                viewModel.soundHelper.playChime(soundOption.id)
                            },
                            label = { Text(soundOption.title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ZenSurfaceVariant,
                                labelColor = ZenTextSecondary,
                                selectedContainerColor = ZenAmber,
                                selectedLabelColor = ZenObsidian
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) ZenAmber else ZenBorder,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session Options Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ZenSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ZenBorder, ZenBorder))),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Open-Ended Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open-Ended Practice",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = "Count up as a stopwatch without a fixed end-time",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = ZenTextSecondary
                        )
                    }
                    Switch(
                        checked = config.isOpenEnded,
                        onCheckedChange = { viewModel.toggleOpenEnded(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZenAmber,
                            checkedTrackColor = ZenAmberDark,
                            uncheckedThumbColor = ZenTextMuted,
                            uncheckedTrackColor = ZenSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preparation Warm-up
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Warm-up Countdown",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = "Settling time before session begins",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = ZenTextSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        prepOptions.forEach { prep ->
                            val isSelected = config.prepTimeSeconds == prep
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ZenAmber else ZenSurfaceVariant)
                                    .clickable { viewModel.updatePrepTimeSeconds(prep) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (prep == 0) "Off" else "${prep}s",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ZenObsidian else ZenTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto DND Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Do Not Disturb",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                            color = ZenTextPrimary
                        )
                        Text(
                            text = "Silence phone notifications during meditation",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = ZenTextSecondary
                        )
                    }
                    Switch(
                        checked = config.isAutoDndEnabled,
                        onCheckedChange = { viewModel.toggleAutoDnd(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ZenAmber,
                            checkedTrackColor = ZenAmberDark,
                            uncheckedThumbColor = ZenTextMuted,
                            uncheckedTrackColor = ZenSurfaceVariant
                        )
                    )
                }
            }
        }

        // DND Access Warning Banner (if enabled but permission missing)
        if (config.isAutoDndEnabled && !isDndGranted) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = ZenAmberDark.copy(alpha = 0.2f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ZenAmber, ZenAmberDark))),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DoNotDisturbOn,
                        contentDescription = null,
                        tint = ZenAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DND Access Required",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 13.sp),
                            color = ZenAmber
                        )
                        Text(
                            text = "Tap here to allow DND permission in system settings.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = ZenTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Practice Big Button
        Button(
            onClick = {
                viewModel.startSession()
                onStartSession()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ZenAmber,
                contentColor = ZenObsidian
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = ZenObsidian,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Begin Practice",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
