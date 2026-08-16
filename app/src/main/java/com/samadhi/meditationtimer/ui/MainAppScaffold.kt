package com.samadhi.meditationtimer.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samadhi.meditationtimer.domain.model.TimerStatus
import com.samadhi.meditationtimer.service.MeditationTimerService
import com.samadhi.meditationtimer.ui.components.TutorialOverlay
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
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.ZenObsidian
import com.samadhi.meditationtimer.ui.theme.ZenTextMuted
import com.samadhi.meditationtimer.ui.theme.ZenTextPrimary
import com.samadhi.meditationtimer.ui.theme.ZenThemeId
import com.samadhi.meditationtimer.ui.theme.getZenThemeById

@Composable
fun MainAppScaffold() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val timerViewModel: TimerViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val timerStatus by timerViewModel.timerStatus.collectAsState()
    var completedSessionData by remember { mutableStateOf<TimerStatus.Completed?>(null) }

    val prefs = remember { context.getSharedPreferences("buddho_prefs", Context.MODE_PRIVATE) }
    var selectedThemeId by remember {
        mutableStateOf(prefs.getString("selected_theme_id", ZenThemeId.BAMBOO_TEAL.id) ?: ZenThemeId.BAMBOO_TEAL.id)
    }
    var showTutorialOverlay by remember {
        mutableStateOf(!prefs.getBoolean("has_seen_tutorial_v1", false))
    }

    val currentTheme = getZenThemeById(selectedThemeId)

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

    CompositionLocalProvider(LocalZenTheme provides currentTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = ZenObsidian,
                            contentColor = ZenTextPrimary,
                            tonalElevation = 0.dp
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
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Timer.route) {
                                                    saveState = false
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ZenTextPrimary,
                                        selectedTextColor = ZenTextPrimary,
                                        unselectedIconColor = ZenTextMuted,
                                        unselectedTextColor = ZenTextMuted,
                                        indicatorColor = currentTheme.primary.copy(alpha = 0.28f)
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
                            onOpenWisdom = {
                                navController.navigate(Screen.Wisdom.route)
                            },
                            onOpenSettings = {
                                navController.navigate(Screen.Settings.route)
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
                        WisdomScreen(
                            onNavigateBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(Screen.Timer.route)
                                }
                            }
                        )
                    }

                    composable(Screen.History.route) {
                        HistoryScreen(viewModel = historyViewModel)
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            timerViewModel = timerViewModel,
                            onNavigateBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(Screen.Timer.route)
                                }
                            },
                            onShowTutorial = {
                                showTutorialOverlay = true
                            },
                            onSelectTheme = { newThemeId ->
                                selectedThemeId = newThemeId.id
                                prefs.edit().putString("selected_theme_id", newThemeId.id).apply()
                            }
                        )
                    }
                }
            }

            // Interactive Tutorial Overlay (Initial Launch & On-Demand)
            TutorialOverlay(
                isVisible = showTutorialOverlay,
                onDismiss = {
                    showTutorialOverlay = false
                    prefs.edit().putBoolean("has_seen_tutorial_v1", true).apply()
                },
                onSelectTheme = { newThemeId ->
                    selectedThemeId = newThemeId.id
                    prefs.edit().putString("selected_theme_id", newThemeId.id).apply()
                }
            )
        }
    }
}
