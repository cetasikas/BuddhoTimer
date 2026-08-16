package com.samadhi.meditationtimer.ui.screens.timer

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenAmberDark
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
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

    val presetDurations = listOf(
        Pair(5, Color(0xFFD8BA84)),   // Warm Sandstone
        Pair(10, Color(0xFFB8734C)),  // Terracotta Clay
        Pair(15, Color(0xFF2A7A78)),  // Selected Teal Glaze
        Pair(20, Color(0xFFD8BA84)),  // Sandstone
        Pair(30, Color(0xFFD8BA84)),  // Sandstone
        Pair(45, Color(0xFFB8734C)),  // Terracotta
        Pair(60, Color(0xFF6D804B))   // Matcha Olive
    )

    val ensoPoints = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        // 1. Subtle Japanese Bamboo Sumi-e Artwork Background
        Image(
            painter = painterResource(id = R.drawable.bg_zen_bamboo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.45f
        )

        // 2. Main Content Scrollable Flow
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Minimalist Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BUDDHO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE8EEF5)
                    )
                )

                // Top Right Action Buttons: [?] and [Settings]
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = onOpenWisdom,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF142036).copy(alpha = 0.8f))
                            .border(1.dp, Color(0xFF334B73), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Meditation Instructions",
                            tint = Color(0xFFC0D5EE),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF142036).copy(alpha = 0.8f))
                            .border(1.dp, Color(0xFF334B73), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Meditation Settings",
                            tint = Color(0xFFC0D5EE),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Handcrafted Hanji Washi Paper Deckled Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Washi Paper Texture Background
                Image(
                    painter = painterResource(id = R.drawable.bg_washi_paper),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Sumi-e Ink Typography on Paper
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "TARGET DURATION",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp,
                        color = Color(0xFF1C2C27)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${config.targetDurationMinutes}",
                            fontSize = 76.sp,
                            fontWeight = FontWeight.Light,
                            color = Color(0xFF152623)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "min",
                            fontSize = 24.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF1C2C27),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (config.isOverrunEnabled) "Continues into Samadhi Overrun when time ends" else "Chime sounds and finishes at target time",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C423C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Quick Presets Header
            Text(
                text = "Quick Presets",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE8EEF5),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Watercolor Paint Stroke Preset Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetDurations.forEach { (duration, chipColor) ->
                    val isSelected = config.targetDurationMinutes == duration
                    val effectiveColor = if (isSelected) Color(0xFF2A7A78) else chipColor

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(effectiveColor)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.updateDurationMinutes(duration) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${duration}m",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF1F1E1B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Zen Enso Track & Fine-tuning Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Enso Rings on Track
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ensoPoints.forEach { point ->
                        val isCurrentOrPassed = config.targetDurationMinutes >= point
                        val isExact = config.targetDurationMinutes == point

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isExact) Color(0xFF2A7A78).copy(alpha = 0.3f) else Color.Transparent
                                )
                                .border(
                                    width = if (isExact) 3.dp else 1.5.dp,
                                    color = when {
                                        isExact -> Color(0xFF2A7A78)
                                        isCurrentOrPassed -> Color(0xFF3B5680)
                                        else -> Color(0xFF22334D)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Slider overlay for smooth dragging
                Slider(
                    value = config.targetDurationMinutes.toFloat(),
                    onValueChange = { viewModel.updateDurationMinutes(it.toInt()) },
                    valueRange = 1f..120f,
                    steps = 118,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2A7A78),
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. "Keep It Going" Mode Toggle (Samadhi Overrun Option)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142036).copy(alpha = 0.85f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF2E4366))),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep It Going (Samadhi Overrun)",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFFE8EEF5)
                        )
                        Text(
                            text = "Timer runs past 00:00 without stopping so practice isn't cut short",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9FB6D4)
                        )
                    }

                    Switch(
                        checked = config.isOverrunEnabled,
                        onCheckedChange = { viewModel.toggleOverrun(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2A7A78),
                            checkedTrackColor = Color(0xFF1C4543),
                            uncheckedThumbColor = ZenTextMuted,
                            uncheckedTrackColor = ZenSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 7. Carved Teal Woodblock "▶ Begin Practice" Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .shadow(10.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        viewModel.startSession()
                        onStartSession()
                    },
                contentAlignment = Alignment.Center
            ) {
                // Teal Painted Wooden Texture Background
                Image(
                    painter = painterResource(id = R.drawable.bg_teal_wood),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark bevel overlay for tactile depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .border(1.5.dp, Color(0xFF5BA7A5).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                )

                // Button Text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFF3EED9),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Begin Practice",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFFF3EED9)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
