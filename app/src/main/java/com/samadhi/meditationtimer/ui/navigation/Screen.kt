package com.samadhi.meditationtimer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Timer : Screen("timer", "Timer", Icons.Default.HourglassEmpty)
    object ActiveSession : Screen("active_session", "Session")
    object Summary : Screen("summary", "Reflection")
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Timer, History, Settings)
    }
}
