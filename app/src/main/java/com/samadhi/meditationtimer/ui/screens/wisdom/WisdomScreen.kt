package com.samadhi.meditationtimer.ui.screens.wisdom

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.data.wisdom.MeditationGuides
import com.samadhi.meditationtimer.data.wisdom.MeditationTip
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WisdomScreen(
    modifier: Modifier = Modifier
) {
    val allTips = remember { MeditationGuides.meditationTips }
    val phases = remember { MeditationGuides.meditationPhases }
    var selectedPhase by remember { mutableStateOf("All Steps") }
    var expandedTipId by remember { mutableStateOf<String?>("drop_past") }

    val filteredTips = remember(selectedPhase) {
        if (selectedPhase == "All Steps") {
            allTips
        } else {
            allTips.filter { it.phase == selectedPhase }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        // Subtle Zen Bamboo Sumi-e Background
        Image(
            painter = painterResource(id = R.drawable.bg_zen_bamboo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.35f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = ZenAmber,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Meditation Instructions",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = "Simple, practical steps for silent awareness",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZenTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phase Filter Tabs
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                phases.forEach { phaseName ->
                    val isSelected = selectedPhase == phaseName
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPhase = phaseName },
                        label = {
                            Text(
                                text = phaseName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ZenSurface.copy(alpha = 0.85f),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions Step List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTips, key = { it.id }) { tip ->
                    val isExpanded = expandedTipId == tip.id
                    TipCard(
                        tip = tip,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedTipId = if (isExpanded) null else tip.id
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun TipCard(
    tip: MeditationTip,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val phaseColor = when (tip.phaseNumber) {
        1 -> Color(0xFFD8BA84) // Warm Sandstone for Groundwork
        2 -> ZenTeal          // Calm Teal for Breath
        3 -> Color(0xFF90CAF9) // Celestial Blue for Stillness
        else -> ZenAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.88f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isExpanded) phaseColor.copy(alpha = 0.6f) else ZenBorder)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Step Number & Phase Badge + Expand Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(phaseColor.copy(alpha = 0.2f))
                            .border(1.dp, phaseColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${tip.stepNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip.phase.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = phaseColor
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = ZenTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step Title
            Text(
                text = tip.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ZenTextPrimary
            )

            // Subtitle
            Text(
                text = tip.subtitle,
                fontSize = 12.sp,
                color = ZenTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Core Wisdom Quote Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ZenSurfaceVariant.copy(alpha = 0.6f))
                    .padding(10.dp)
            ) {
                Text(
                    text = tip.coreWisdom,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = ZenTextPrimary
                )
            }

            // Expanded Practical Points
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "HOW TO PRACTICE:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = phaseColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    tip.detailedGuidance.forEach { point ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = phaseColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = point,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = ZenTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
