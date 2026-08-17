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

    val timerStatus: StateFlow<TimerStatus> = MeditationTimerService.timerStatus

    private val _config = MutableStateFlow(TimerConfiguration())
    val config: StateFlow<TimerConfiguration> = _config.asStateFlow()

    private val _isDndGranted = MutableStateFlow(dndManager.isDndAccessGranted)
    val isDndGranted: StateFlow<Boolean> = _isDndGranted.asStateFlow()

    private val _isBatteryOptIgnored = MutableStateFlow(checkBatteryOptimization())
    val isBatteryOptIgnored: StateFlow<Boolean> = _isBatteryOptIgnored.asStateFlow()

    // Pre-session wisdom quote
    private val _currentQuote = MutableStateFlow(getRandomQuote())
    val currentQuote: StateFlow<WisdomQuote> = _currentQuote.asStateFlow()

    fun updateDurationMinutes(minutes: Int) {
        _config.value = _config.value.copy(targetDurationMinutes = minutes, isOpenEnded = false)
    }

    fun updatePrepTimeSeconds(seconds: Int) {
        _config.value = _config.value.copy(prepTimeSeconds = seconds)
    }

    fun toggleOpenEnded(isOpenEnded: Boolean) {
        _config.value = _config.value.copy(isOpenEnded = isOpenEnded)
    }

    fun toggleOverrun(isOverrunEnabled: Boolean) {
        _config.value = _config.value.copy(isOverrunEnabled = isOverrunEnabled)
    }

    fun toggleAutoDnd(enabled: Boolean) {
        _config.value = _config.value.copy(isAutoDndEnabled = enabled)
    }

    fun toggleShowLeavesDuringMeditation(enabled: Boolean) {
        _config.value = _config.value.copy(showLeavesDuringMeditation = enabled)
    }

    fun updateMeditationLeafCount(count: Int) {
        _config.value = _config.value.copy(meditationLeafCount = count)
    }

    fun updateSoundBell(soundResName: String) {
        _config.value = _config.value.copy(soundBellResName = soundResName)
    }

    fun refreshPermissions() {
        _isDndGranted.value = dndManager.isDndAccessGranted
        _isBatteryOptIgnored.value = checkBatteryOptimization()
    }

    fun cycleNextQuote() {
        val quotes = MeditationGuides.wisdomQuotes
        val currentIndex = quotes.indexOfFirst { it.id == _currentQuote.value.id }
        val nextIndex = if (currentIndex >= 0 && currentIndex < quotes.size - 1) currentIndex + 1 else 0
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
            putExtra(MeditationTimerService.EXTRA_SOUND_BELL, currentConfig.soundBellResName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
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
