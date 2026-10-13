package com.samadhi.meditationtimer.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.model.TimerStatus
import com.samadhi.meditationtimer.service.MeditationTimerService
import com.samadhi.meditationtimer.ui.components.TutorialOverlay
import com.samadhi.meditationtimer.ui.navigation.Screen
import com.samadhi.meditationtimer.ui.screens.breathing.BreathingScreen
import com.samadhi.meditationtimer.ui.screens.journal.EditorRequest
import com.samadhi.meditationtimer.ui.screens.journal.JournalScreen
import com.samadhi.meditationtimer.ui.screens.journal.JournalViewModel
import com.samadhi.meditationtimer.ui.screens.journal.SessionEditorScreen
import com.samadhi.meditationtimer.ui.screens.session.ActiveSessionScreen
import com.samadhi.meditationtimer.ui.screens.settings.SettingsScreen
import com.samadhi.meditationtimer.ui.screens.settings.SettingsViewModel
import com.samadhi.meditationtimer.ui.screens.timer.TimerScreen
import com.samadhi.meditationtimer.ui.screens.timer.TimerViewModel
import com.samadhi.meditationtimer.ui.screens.wisdom.WisdomScreen
import com.samadhi.meditationtimer.ui.theme.LocalZenTheme
import com.samadhi.meditationtimer.ui.theme.SamadhiTimerTheme
import com.samadhi.meditationtimer.ui.theme.rememberZenTheme
import kotlinx.coroutines.launch

@Composable
fun MainAppScaffold() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("buddho_prefs", Context.MODE_PRIVATE) }
    var showTutorialOverlay by remember {
        mutableStateOf(!prefs.getBoolean("has_seen_tutorial_v2", false))
    }
    val appScope = rememberCoroutineScope()
    val sessionRepository = remember {
        (context.applicationContext as MeditationApp).sessionRepository
    }

    SamadhiTimerTheme {
        val currentTheme = rememberZenTheme()
        val navController = rememberNavController()
        val timerViewModel: TimerViewModel = viewModel()
        val journalViewModel: JournalViewModel = viewModel()
        val settingsViewModel: SettingsViewModel = viewModel()

        val timerStatus by timerViewModel.timerStatus.collectAsState()
        val timerConfig by timerViewModel.config.collectAsState()
        var completedSessionData by remember { mutableStateOf<TimerStatus.Completed?>(null) }
        var editorRequest by remember { mutableStateOf<EditorRequest?>(null) }
        var loggedCompletionMs by remember { mutableStateOf(0L) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Synchronize UI with background service state
        LaunchedEffect(timerStatus, timerConfig.showSessionOverview) {
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
                    if (status.completedTimeEpochMs != loggedCompletionMs) {
                        loggedCompletionMs = status.completedTimeEpochMs
                        appScope.launch {
                            sessionRepository.recordCompletion(
                                completedAtMs = status.completedTimeEpochMs,
                                durationSec = status.actualDurationSec,
                                isBreathing = false
                            )
                        }
                        if (!timerConfig.showSessionOverview) {
                            // No reflection screen: log the sit immediately so every session reaches the journal.
                            journalViewModel.saveSession(
                                MeditationSession(
                                    startTimeEpochMs = status.startTimeEpochMs,
                                    completedTimeEpochMs = status.completedTimeEpochMs,
                                    targetDurationSec = status.targetDurationSec,
                                    actualDurationSec = status.actualDurationSec,
                                    overrunSec = status.overrunSec,
                                    rating = 0,
                                    notesMarkdown = "",
                                    completedSuccessfully = true
                                )
                            )
                        }
                    }
                    if (timerConfig.showSessionOverview) {
                        navController.navigate(Screen.Summary.route) {
                            popUpTo(Screen.Timer.route)
                        }
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
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 3.dp
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
                                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
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
                            .background(MaterialTheme.colorScheme.background)
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
                                    val status = timerViewModel.timerStatus.value
                                    if (status is TimerStatus.Completed) {
                                        completedSessionData = status
                                    }
                                    if (timerConfig.showSessionOverview) {
                                        navController.navigate(Screen.Summary.route) {
                                            popUpTo(Screen.Timer.route)
                                        }
                                    } else {
                                        MeditationTimerService.resetStatus()
                                        navController.navigate(Screen.Timer.route) {
                                            popUpTo(Screen.Timer.route) { inclusive = true }
                                        }
                                    }
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
                                },
                                onSessionCompleted = { completedStatus ->
                                    completedSessionData = completedStatus
                                    appScope.launch {
                                        sessionRepository.recordCompletion(
                                            completedAtMs = completedStatus.completedTimeEpochMs,
                                            durationSec = completedStatus.actualDurationSec,
                                            isBreathing = true
                                        )
                                    }
                                    if (timerConfig.showSessionOverview) {
                                        navController.navigate(Screen.Summary.route) {
                                            popUpTo(Screen.Timer.route)
                                        }
                                    } else {
                                        journalViewModel.saveSession(
                                            MeditationSession(
                                                startTimeEpochMs = completedStatus.startTimeEpochMs,
                                                completedTimeEpochMs = completedStatus.completedTimeEpochMs,
                                                targetDurationSec = completedStatus.targetDurationSec,
                                                actualDurationSec = completedStatus.actualDurationSec,
                                                overrunSec = completedStatus.overrunSec,
                                                rating = 0,
                                                notesMarkdown = "",
                                                completedSuccessfully = true
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        composable(Screen.Summary.route) {
                            val status = completedSessionData
                            if (status == null) {
                                LaunchedEffect(Unit) {
                                    navController.navigate(Screen.Timer.route) {
                                        popUpTo(Screen.Timer.route) { inclusive = true }
                                    }
                                }
                            } else {
                                SessionEditorScreen(
                                    request = EditorRequest.PostSession(status),
                                    onSave = { session ->
                                        journalViewModel.saveSession(session)
                                        com.samadhi.meditationtimer.service.DndManager(context).restoreDnd()
                                        MeditationTimerService.resetStatus()
                                        completedSessionData = null
                                        navController.navigate(Screen.Journal.route) {
                                            popUpTo(Screen.Timer.route)
                                        }
                                    },
                                    onDiscard = {
                                        // The sit still happened: keep it in the journal without a reflection.
                                        journalViewModel.saveSession(
                                            MeditationSession(
                                                startTimeEpochMs = status.startTimeEpochMs,
                                                completedTimeEpochMs = status.completedTimeEpochMs,
                                                targetDurationSec = status.targetDurationSec,
                                                actualDurationSec = status.actualDurationSec,
                                                overrunSec = status.overrunSec,
                                                rating = 0,
                                                notesMarkdown = "",
                                                completedSuccessfully = true
                                            )
                                        )
                                        com.samadhi.meditationtimer.service.DndManager(context).restoreDnd()
                                        MeditationTimerService.resetStatus()
                                        completedSessionData = null
                                        navController.navigate(Screen.Timer.route) {
                                            popUpTo(Screen.Timer.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
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

                        composable(Screen.Journal.route) {
                            JournalScreen(
                                viewModel = journalViewModel,
                                onEditSession = { session ->
                                    editorRequest = EditorRequest.Edit(session)
                                    navController.navigate(Screen.JournalEditor.route)
                                },
                                onLogManual = {
                                    editorRequest = EditorRequest.ManualNew
                                    navController.navigate(Screen.JournalEditor.route)
                                },
                                onNavigateBack = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate(Screen.Timer.route)
                                    }
                                }
                            )
                        }

                        composable(Screen.JournalEditor.route) {
                            val request = editorRequest
                            if (request == null) {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            } else {
                                SessionEditorScreen(
                                    request = request,
                                    onSave = { session ->
                                        journalViewModel.saveSession(session)
                                        editorRequest = null
                                        navController.popBackStack()
                                    },
                                    onDiscard = {
                                        editorRequest = null
                                        navController.popBackStack()
                                    }
                                )
                            }
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
                        prefs.edit().putBoolean("has_seen_tutorial_v2", true).apply()
                    }
                )
            }
        }
    }
}
