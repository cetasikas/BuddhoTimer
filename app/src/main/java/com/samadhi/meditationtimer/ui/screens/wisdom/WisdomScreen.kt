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
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.data.wisdom.MeditationGuides
import com.samadhi.meditationtimer.data.wisdom.TopicCardData
import com.samadhi.meditationtimer.ui.components.FallingLeavesEffect
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenBorder
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenSurfaceVariant
import com.samadhi.meditationtimer.ui.theme.ZenTeal
import com.samadhi.meditationtimer.ui.theme.ZenTealLight
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenTextSecondary

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton

@Composable
fun WisdomScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val topics = remember { MeditationGuides.topicCards }
    var expandedTopicId by remember { mutableStateOf<String?>("start_here") }

    Box(modifier = modifier.fillMaxSize().background(ZenObsidian)) {
        // 1. Dark Moonlit Bamboo Backdrop
        Image(
            painter = painterResource(id = R.drawable.bg_zen_bamboo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.20f
        )

        // Moonlit Gradient Scrim
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

        // 2. Subtle Falling Leaves
        FallingLeavesEffect(
            leafCount = 14,
            ambientAlpha = 0.26f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row with Back Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
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
                        text = "MEDITATION GUIDES",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = ZenTextPrimary
                    )
                    Text(
                        text = "Tap any topic below to view simple instructions",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZenTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Expandable Topic Cards List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(topics, key = { it.id }) { topic ->
                    val isExpanded = expandedTopicId == topic.id
                    SimpleTopicCard(
                        topic = topic,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedTopicId = if (isExpanded) null else topic.id
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

@Composable
private fun SimpleTopicCard(
    topic: TopicCardData,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val topicColor = when (topic.topicNumber) {
        1 -> ZenAmber          // Start Here Gold
        2 -> ZenTeal           // Breath Teal
        3 -> Color(0xFF81D4FA) // Buddho Sky Blue
        4 -> Color(0xFFA5D6A7) // Walking Moss Green
        5 -> Color(0xFFFFCC80) // Calming Amber
        else -> Color(0xFFF48FB1) // Metta Rose
    }

    val iconVector: ImageVector = when (topic.iconName) {
        "start" -> Icons.Outlined.Spa
        "breath" -> Icons.Outlined.Air
        "buddho" -> Icons.Outlined.SelfImprovement
        "walking" -> Icons.AutoMirrored.Outlined.DirectionsWalk
        "restlessness" -> Icons.Outlined.WaterDrop
        else -> Icons.Outlined.FavoriteBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = ZenSurface.copy(alpha = 0.75f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isExpanded) topicColor.copy(alpha = 0.6f) else ZenBorder)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Topic Icon + Title + Expand Arrow
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
                            .background(topicColor.copy(alpha = 0.18f))
                            .border(1.dp, topicColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = topicColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = topic.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExpanded) ZenTextPrimary else ZenTextPrimary.copy(alpha = 0.95f)
                        )
                        Text(
                            text = topic.subtitle,
                            fontSize = 12.sp,
                            color = ZenTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = if (isExpanded) topicColor else ZenTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Expanded Practical Instructions
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Quote Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ZenSurfaceVariant.copy(alpha = 0.6f))
                            .border(1.dp, ZenBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = topic.quote,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 17.sp,
                            color = ZenTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "HOW TO PRACTICE:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = topicColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    topic.practicalSteps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(topicColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = topicColor
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = step,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = ZenTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
