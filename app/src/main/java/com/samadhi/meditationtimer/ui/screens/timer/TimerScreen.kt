package com.samadhi.meditationtimer.ui.screens.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenOverrunBlue
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onStartSession: () -> Unit,
    onOpenWisdom: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ZenObsidian)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Top Minimalist Bar: Brand on Left, [?] & [Settings] on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Right Action Icons: Help/Wisdom Instructions (?) and Settings (⚙️)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Meditation Instructions (?)
                IconButton(
                    onClick = onOpenWisdom,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ZenSurface)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Meditation Instructions",
                        tint = ZenAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Settings (⚙️)
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ZenSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Meditation Settings",
                        tint = ZenTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Target Duration Display Card (Clean, elegant, front & center)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ZenSurfaceVariant, ZenSurface)
                    )
                )
                .border(1.dp, ZenBorder, RoundedCornerShape(28.dp))
                .padding(vertical = 44.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TARGET DURATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.5.sp,
                    color = ZenTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${config.targetDurationMinutes}",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Light,
                        color = ZenAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "min",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = ZenTextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                Text(
                    text = "Continues into Samadhi Overrun when time ends",
                    fontSize = 11.sp,
                    color = ZenOverrunBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Preset Chips
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.labelLarge,
            color = ZenTextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(10.dp))

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
                    label = { Text("${duration}m", fontSize = 13.sp) },
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

        Spacer(modifier = Modifier.height(18.dp))

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

        Spacer(modifier = Modifier.height(44.dp))

        // Big Begin Practice Button
        Button(
            onClick = {
                viewModel.startSession()
                onStartSession()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
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
                modifier = Modifier.size(26.dp)
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
