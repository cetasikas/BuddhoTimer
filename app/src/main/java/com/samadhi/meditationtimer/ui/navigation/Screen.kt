package com.samadhi.meditationtimer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Timer : Screen("timer", "Timer", Icons.Outlined.HourglassEmpty)
    object ActiveSession : Screen("active_session", "Session")
    object Summary : Screen("summary", "Reflection")
    object Breathing : Screen("breathing", "Breathing", Icons.Outlined.Air)
    object Wisdom : Screen("wisdom", "Teachings", Icons.AutoMirrored.Outlined.MenuBook)
    object Journal : Screen("journal", "Journal", Icons.Outlined.History)
    object JournalEditor : Screen("journal_editor", "Entry")
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)

    companion object {
        val bottomNavItems = listOf(Timer, Wisdom, Journal, Settings)
    }
}

