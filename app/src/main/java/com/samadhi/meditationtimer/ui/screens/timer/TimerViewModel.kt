package com.samadhi.meditationtimer.ui.screens.timer

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samadhi.meditationtimer.data.wisdom.MeditationGuides
import com.samadhi.meditationtimer.data.wisdom.WisdomQuote
import com.samadhi.meditationtimer.domain.model.TimerConfiguration
import com.samadhi.meditationtimer.domain.model.TimerStatus
import com.samadhi.meditationtimer.service.DndManager
import com.samadhi.meditationtimer.service.MeditationTimerService
import com.samadhi.meditationtimer.service.SoundHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val dndManager = DndManager(context)
    val soundHelper = SoundHelper(context)

    private val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)

    val timerStatus: StateFlow<TimerStatus> = MeditationTimerService.timerStatus

    private val _config = MutableStateFlow(loadPersistedConfig())
    val config: StateFlow<TimerConfiguration> = _config.asStateFlow()

    private val _isDndGranted = MutableStateFlow(dndManager.isDndAccessGranted)
    val isDndGranted: StateFlow<Boolean> = _isDndGranted.asStateFlow()

    private val _isBatteryOptIgnored = MutableStateFlow(checkBatteryOptimization())
    val isBatteryOptIgnored: StateFlow<Boolean> = _isBatteryOptIgnored.asStateFlow()

    // Pre-session wisdom quote
    private val _currentQuote = MutableStateFlow(getRandomQuote())
    val currentQuote: StateFlow<WisdomQuote> = _currentQuote.asStateFlow()

    // Box Breathing setup preferences
    private val _selectedBreathingPatternId = MutableStateFlow(prefs.getString("breathing_pattern_id", "box") ?: "box")
    val selectedBreathingPatternId: StateFlow<String> = _selectedBreathingPatternId.asStateFlow()

    private val _breathingDurationMinutes = MutableStateFlow(prefs.getInt("breathing_duration_min", 5))
    val breathingDurationMinutes: StateFlow<Int> = _breathingDurationMinutes.asStateFlow()

    fun updateBreathingPattern(patternId: String) {
        _selectedBreathingPatternId.value = patternId
        prefs.edit().putString("breathing_pattern_id", patternId).apply()
    }

    fun updateBreathingDurationMinutes(minutes: Int) {
        _breathingDurationMinutes.value = minutes
        prefs.edit().putInt("breathing_duration_min", minutes).apply()
    }

    private fun loadPersistedConfig(): TimerConfiguration {
        return TimerConfiguration(
            targetDurationMinutes = prefs.getInt("target_duration_min", 15),
            prepTimeSeconds = prefs.getInt("prep_time_sec", 5),
            soundBellResName = prefs.getString("sound_bell_res", SoundHelper.SOUND_TIBETAN_BOWL) ?: SoundHelper.SOUND_TIBETAN_BOWL,
            isAutoDndEnabled = prefs.getBoolean("auto_dnd", false),
            isOpenEnded = prefs.getBoolean("open_ended", false),
            isOverrunEnabled = prefs.getBoolean("overrun_enabled", false),
            showLeavesDuringMeditation = prefs.getBoolean("show_leaves", true),
            meditationLeafCount = prefs.getInt("meditation_leaf_count", 16),
            mindfulnessIntervalMinutes = prefs.getInt("mindfulness_interval_min", 0),
            tapToBlackoutEnabled = prefs.getBoolean("tap_to_blackout", true),
            keepScreenAwake = prefs.getBoolean("keep_screen_awake", true),
            isBoxBreathingMode = prefs.getBoolean("box_breathing_mode", false),
            isCountdownHapticsEnabled = prefs.getBoolean("countdown_haptics", true),
            isCompletionBellEnabled = prefs.getBoolean("completion_bell_enabled", true),
            isStartBellEnabled = prefs.getBoolean("start_bell_enabled", false),
            showSessionOverview = prefs.getBoolean("show_session_overview", true)
        )
    }

    private fun saveConfig(newConfig: TimerConfiguration) {
        _config.value = newConfig
        prefs.edit().apply {
            putInt("target_duration_min", newConfig.targetDurationMinutes)
            putInt("prep_time_sec", newConfig.prepTimeSeconds)
            putString("sound_bell_res", newConfig.soundBellResName)
            putBoolean("auto_dnd", newConfig.isAutoDndEnabled)
            putBoolean("open_ended", newConfig.isOpenEnded)
            putBoolean("overrun_enabled", newConfig.isOverrunEnabled)
            putBoolean("show_leaves", newConfig.showLeavesDuringMeditation)
            putInt("meditation_leaf_count", newConfig.meditationLeafCount)
            putInt("mindfulness_interval_min", newConfig.mindfulnessIntervalMinutes)
            putBoolean("tap_to_blackout", newConfig.tapToBlackoutEnabled)
            putBoolean("keep_screen_awake", newConfig.keepScreenAwake)
            putBoolean("box_breathing_mode", newConfig.isBoxBreathingMode)
            putBoolean("countdown_haptics", newConfig.isCountdownHapticsEnabled)
            putBoolean("completion_bell_enabled", newConfig.isCompletionBellEnabled)
            putBoolean("start_bell_enabled", newConfig.isStartBellEnabled)
            putBoolean("show_session_overview", newConfig.showSessionOverview)
            apply()
        }
    }

    fun toggleShowSessionOverview(enabled: Boolean) {
        saveConfig(_config.value.copy(showSessionOverview = enabled))
    }

    fun updateDurationMinutes(minutes: Int) {
        saveConfig(_config.value.copy(targetDurationMinutes = minutes, isOpenEnded = false))
    }

    fun updatePrepTimeSeconds(seconds: Int) {
        saveConfig(_config.value.copy(prepTimeSeconds = seconds))
    }

    fun toggleCountdownHaptics(enabled: Boolean) {
        saveConfig(_config.value.copy(isCountdownHapticsEnabled = enabled))
    }

    fun toggleCompletionBell(enabled: Boolean) {
        saveConfig(_config.value.copy(isCompletionBellEnabled = enabled))
    }

    fun toggleStartBell(enabled: Boolean) {
        saveConfig(_config.value.copy(isStartBellEnabled = enabled))
    }

    fun toggleOpenEnded(isOpenEnded: Boolean) {
        saveConfig(_config.value.copy(isOpenEnded = isOpenEnded))
    }

    fun toggleOverrun(isOverrunEnabled: Boolean) {
        saveConfig(_config.value.copy(isOverrunEnabled = isOverrunEnabled))
    }

    fun toggleBoxBreathingMode(isBoxBreathing: Boolean) {
        saveConfig(_config.value.copy(isBoxBreathingMode = isBoxBreathing))
    }

    fun toggleAutoDnd(enabled: Boolean) {
        saveConfig(_config.value.copy(isAutoDndEnabled = enabled))
    }

    fun toggleShowLeavesDuringMeditation(enabled: Boolean) {
        saveConfig(_config.value.copy(showLeavesDuringMeditation = enabled))
    }

    fun updateMeditationLeafCount(count: Int) {
        saveConfig(_config.value.copy(meditationLeafCount = count))
    }

    fun updateSoundBell(soundRes: String) {
        saveConfig(_config.value.copy(soundBellResName = soundRes))
    }

    fun updateMindfulnessInterval(minutes: Int) {
        saveConfig(_config.value.copy(mindfulnessIntervalMinutes = minutes))
    }

    fun toggleTapToBlackout(enabled: Boolean) {
        saveConfig(_config.value.copy(tapToBlackoutEnabled = enabled))
    }

    fun toggleKeepScreenAwake(enabled: Boolean) {
        saveConfig(_config.value.copy(keepScreenAwake = enabled))
    }

    fun refreshPermissions() {
        _isDndGranted.value = dndManager.isDndAccessGranted
        _isBatteryOptIgnored.value = checkBatteryOptimization()
    }

    fun refreshQuote() {
        val quotes = MeditationGuides.wisdomQuotes
        val nextIndex = (quotes.indexOf(_currentQuote.value) + 1) % quotes.size
        _currentQuote.value = quotes[nextIndex]
    }

    private fun getRandomQuote(): WisdomQuote {
        val quotes = MeditationGuides.wisdomQuotes
        return quotes[Random.nextInt(quotes.size)]
    }

    fun startSession() {
        val currentConfig = _config.value
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_START
            putExtra(MeditationTimerService.EXTRA_TARGET_SEC, currentConfig.targetDurationSeconds)
            putExtra(MeditationTimerService.EXTRA_PREP_SEC, currentConfig.prepTimeSeconds)
            putExtra(MeditationTimerService.EXTRA_AUTO_DND, currentConfig.isAutoDndEnabled && _isDndGranted.value)
            putExtra(MeditationTimerService.EXTRA_OPEN_ENDED, currentConfig.isOpenEnded)
            putExtra(MeditationTimerService.EXTRA_OVERRUN_ENABLED, currentConfig.isOverrunEnabled)
            putExtra(MeditationTimerService.EXTRA_SOUND_BELL, currentConfig.soundBellResName)
            putExtra(MeditationTimerService.EXTRA_MINDFULNESS_INTERVAL_MIN, currentConfig.mindfulnessIntervalMinutes)
            putExtra(MeditationTimerService.EXTRA_COMPLETION_BELL, currentConfig.isCompletionBellEnabled)
            putExtra(MeditationTimerService.EXTRA_START_BELL, currentConfig.isStartBellEnabled)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun continueSession(initialElapsedSec: Int = 0, initialStartTimeMs: Long = 0L) {
        val currentConfig = _config.value
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_START
            putExtra(MeditationTimerService.EXTRA_TARGET_SEC, 0)
            putExtra(MeditationTimerService.EXTRA_PREP_SEC, 0)
            putExtra(MeditationTimerService.EXTRA_AUTO_DND, currentConfig.isAutoDndEnabled && _isDndGranted.value)
            putExtra(MeditationTimerService.EXTRA_OPEN_ENDED, true)
            putExtra(MeditationTimerService.EXTRA_OVERRUN_ENABLED, true)
            putExtra(MeditationTimerService.EXTRA_SOUND_BELL, currentConfig.soundBellResName)
            putExtra(MeditationTimerService.EXTRA_MINDFULNESS_INTERVAL_MIN, currentConfig.mindfulnessIntervalMinutes)
            putExtra(MeditationTimerService.EXTRA_COMPLETION_BELL, currentConfig.isCompletionBellEnabled)
            putExtra(MeditationTimerService.EXTRA_START_BELL, false)
            putExtra(MeditationTimerService.EXTRA_INITIAL_ELAPSED_SEC, initialElapsedSec)
            putExtra(MeditationTimerService.EXTRA_INITIAL_START_TIME_MS, initialStartTimeMs)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun continueSessionWithTarget(targetSec: Int, initialElapsedSec: Int, initialStartTimeMs: Long) {
        val currentConfig = _config.value
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_START
            putExtra(MeditationTimerService.EXTRA_TARGET_SEC, targetSec)
            putExtra(MeditationTimerService.EXTRA_PREP_SEC, 0)
            putExtra(MeditationTimerService.EXTRA_AUTO_DND, currentConfig.isAutoDndEnabled && _isDndGranted.value)
            putExtra(MeditationTimerService.EXTRA_OPEN_ENDED, false)
            putExtra(MeditationTimerService.EXTRA_OVERRUN_ENABLED, currentConfig.isOverrunEnabled)
            putExtra(MeditationTimerService.EXTRA_SOUND_BELL, currentConfig.soundBellResName)
            putExtra(MeditationTimerService.EXTRA_MINDFULNESS_INTERVAL_MIN, currentConfig.mindfulnessIntervalMinutes)
            putExtra(MeditationTimerService.EXTRA_COMPLETION_BELL, currentConfig.isCompletionBellEnabled)
            putExtra(MeditationTimerService.EXTRA_START_BELL, false)
            putExtra(MeditationTimerService.EXTRA_INITIAL_ELAPSED_SEC, initialElapsedSec)
            putExtra(MeditationTimerService.EXTRA_INITIAL_START_TIME_MS, initialStartTimeMs)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun adjustSessionTime(deltaSec: Int) {
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_ADJUST_TIME
            putExtra(MeditationTimerService.EXTRA_ADJUST_SEC, deltaSec)
        }
        context.startService(intent)
    }

    fun pauseSession() {
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeSession() {
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun finishSession() {
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_FINISH
        }
        context.startService(intent)
    }

    fun cancelSession() {
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }
}
