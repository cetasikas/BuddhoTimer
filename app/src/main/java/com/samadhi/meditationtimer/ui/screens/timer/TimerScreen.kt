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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenAmberDark
import com.samadhi.meditationtimer.ui.theme.ZenBorder
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
    onOpenBreathing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val isDndGranted by viewModel.isDndGranted.collectAsState()
    val isBatteryOptIgnored by viewModel.isBatteryOptIgnored.collectAsState()
    val currentQuote by viewModel.currentQuote.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showWisdomDialog by remember { mutableStateOf(false) }

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

        // Top Minimalist Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Wisdom Dialog trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showWisdomDialog = true }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = ZenAmber,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BUDDHO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenAmber
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Daily Contemplation",
                    tint = ZenAmber.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Quick Jump to Box Breathing Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ZenSurfaceVariant)
                    .border(1.dp, ZenTeal.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable { onOpenBreathing() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = ZenTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Breathing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZenTeal
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Target Duration Display Card (Pristine, focused, front & center)
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
                .padding(vertical = 34.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TARGET DURATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = ZenTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${config.targetDurationMinutes}",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        color = ZenAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "min",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = ZenTextSecondary,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
                Text(
                    text = "Runs past 00:00 into Samadhi Overrun without stopping",
                    fontSize = 11.sp,
                    color = ZenOverrunBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Preset Chips
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

        Spacer(modifier = Modifier.height(12.dp))

        // Duration Slider (1 min to 120 mins)
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

        Spacer(modifier = Modifier.height(18.dp))

        // Session Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ZenSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ZenBorder, ZenBorder))),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            text = "Gentle settling time before chime",
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

                Spacer(modifier = Modifier.height(14.dp))

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
                            text = "Silence phone notifications during sit",
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

                Spacer(modifier = Modifier.height(14.dp))

                // Box Breathing Relief Entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenBreathing() }
                        .background(ZenSurfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = ZenTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Box Breathing Exercise",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ZenTextPrimary
                            )
                            Text(
                                text = "4-4-4-4 for acute stress & anxiety relief",
                                fontSize = 11.sp,
                                color = ZenTextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Start Breathing",
                        tint = ZenTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // DND Access Warning Banner (if permission missing)
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
                    Column {
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

    // Daily Wisdom Contemplation Dialog
    if (showWisdomDialog) {
        AlertDialog(
            onDismissRequest = { showWisdomDialog = false },
            containerColor = ZenSurface,
            titleContentColor = ZenTextPrimary,
            textContentColor = ZenTextSecondary,
            title = {
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Contemplation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZenAmber
                        )
                    }
                    IconButton(
                        onClick = { viewModel.cycleNextQuote() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Next Quote",
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "\"${currentQuote.quote}\"",
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 22.sp,
                        color = ZenTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "— ${currentQuote.author}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenAmber
                    )
                    Text(
                        text = currentQuote.sourceOrTradition,
                        fontSize = 11.sp,
                        color = ZenTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showWisdomDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZenAmber,
                        contentColor = ZenObsidian
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sit with Mind", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
