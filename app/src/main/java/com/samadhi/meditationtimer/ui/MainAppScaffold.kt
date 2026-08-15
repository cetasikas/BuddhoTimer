package com.samadhi.meditationtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samadhi.meditationtimer.domain.model.TimerStatus
import com.samadhi.meditationtimer.service.MeditationTimerService
import com.samadhi.meditationtimer.ui.navigation.Screen
import com.samadhi.meditationtimer.ui.screens.breathing.BreathingScreen
import com.samadhi.meditationtimer.ui.screens.history.HistoryScreen
import com.samadhi.meditationtimer.ui.screens.history.HistoryViewModel
import com.samadhi.meditationtimer.ui.screens.session.ActiveSessionScreen
import com.samadhi.meditationtimer.ui.screens.settings.SettingsScreen
import com.samadhi.meditationtimer.ui.screens.settings.SettingsViewModel
import com.samadhi.meditationtimer.ui.screens.summary.SessionSummaryScreen
import com.samadhi.meditationtimer.ui.screens.timer.TimerScreen
import com.samadhi.meditationtimer.ui.screens.timer.TimerViewModel
import com.samadhi.meditationtimer.ui.screens.wisdom.WisdomScreen
import com.samadhi.meditationtimer.ui.theme.ZenAmber
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenSurface
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary

@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()
    val timerViewModel: TimerViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val timerStatus by timerViewModel.timerStatus.collectAsState()
    var completedSessionData by remember { mutableStateOf<TimerStatus.Completed?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Synchronize UI with background service state
    LaunchedEffect(timerStatus) {
        when (val status = timerStatus) {
            is TimerStatus.Preparing, is TimerStatus.Running, is TimerStatus.Paused -> {
                if (currentRoute != Screen.ActiveSession.route) {
                    navController.navigate(Screen.ActiveSession.route) {
                        launchSingleTop = true
                    }
                }
            }
            is TimerStatus.Completed -> {
                completedSessionData = status
                navController.navigate(Screen.Summary.route) {
                    popUpTo(Screen.Timer.route)
                }
            }
            is TimerStatus.Idle -> {
                // If on active session and idle, return to timer screen
                if (currentRoute == Screen.ActiveSession.route) {
                    navController.navigate(Screen.Timer.route) {
                        popUpTo(Screen.Timer.route) { inclusive = true }
                    }
                }
            }
        }
    }

    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = ZenSurface,
                    contentColor = ZenTextPrimary
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ZenAmber,
                                selectedTextColor = ZenAmber,
                                unselectedIconColor = ZenTextMuted,
                                unselectedTextColor = ZenTextMuted,
                                indicatorColor = ZenAmber.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timer.route,
            modifier = Modifier
                .fillMaxSize()
                .background(ZenObsidian)
                .padding(innerPadding)
        ) {
            composable(Screen.Timer.route) {
                TimerScreen(
                    viewModel = timerViewModel,
                    onStartSession = {
                        navController.navigate(Screen.ActiveSession.route)
                    },
                    onOpenBreathing = {
                        navController.navigate(Screen.Breathing.route)
                    }
                )
            }

            composable(Screen.ActiveSession.route) {
                ActiveSessionScreen(
                    viewModel = timerViewModel,
                    onSessionCompleted = {
                        navController.navigate(Screen.Summary.route)
                    },
                    onSessionCancelled = {
                        navController.navigate(Screen.Timer.route) {
                            popUpTo(Screen.Timer.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Breathing.route) {
                BreathingScreen(
                    onClose = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Summary.route) {
                SessionSummaryScreen(
                    completedStatus = completedSessionData,
                    onSaveSession = { session ->
                        historyViewModel.saveSession(session)
                        MeditationTimerService.resetStatus()
                        completedSessionData = null
                        navController.navigate(Screen.History.route) {
                            popUpTo(Screen.Timer.route)
                        }
                    },
                    onDiscard = {
                        MeditationTimerService.resetStatus()
                        completedSessionData = null
                        navController.navigate(Screen.Timer.route) {
                            popUpTo(Screen.Timer.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Wisdom.route) {
                WisdomScreen()
            }

            composable(Screen.History.route) {
                HistoryScreen(viewModel = historyViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onOpenBreathing = {
                        navController.navigate(Screen.Breathing.route)
                    }
                )
            }
        }
    }
}
