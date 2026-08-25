package com.samadhi.meditationtimer.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.domain.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MeditationTimerService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var wakeLockHelper: WakeLockHelper
    private lateinit var soundHelper: SoundHelper
    private lateinit var dndManager: DndManager

    private var startTimeEpochMs: Long = 0L
    private var targetDurationSec: Int = 0
    private var prepTimeSec: Int = 0
    private var elapsedSeconds: Int = 0
    private var overrunSeconds: Int = 0
    private var isOpenEnded: Boolean = false
    private var isDndEnabledForSession: Boolean = false
    private var isOverrunEnabledForSession: Boolean = false
    private var isCompletionBellEnabledForSession: Boolean = false
    private var isStartBellEnabledForSession: Boolean = false
    private var hasChimedForOverrun: Boolean = false

    private var soundBellResName: String = SoundHelper.SOUND_TIBETAN_BOWL

    inner class LocalBinder : Binder() {
        fun getService(): MeditationTimerService = this@MeditationTimerService
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        wakeLockHelper = WakeLockHelper(this)
        soundHelper = SoundHelper(this)
        dndManager = DndManager(this)
        Log.d(TAG, "MeditationTimerService created")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val targetSec = intent.getIntExtra(EXTRA_TARGET_SEC, 15 * 60)
                val prepSec = intent.getIntExtra(EXTRA_PREP_SEC, 5)
                val autoDnd = intent.getBooleanExtra(EXTRA_AUTO_DND, true)
                val openEnded = intent.getBooleanExtra(EXTRA_OPEN_ENDED, false)
                val isOverrunEnabled = intent.getBooleanExtra(EXTRA_OVERRUN_ENABLED, false)
                val soundBell = intent.getStringExtra(EXTRA_SOUND_BELL) ?: SoundHelper.SOUND_TIBETAN_BOWL
                val mindfulnessIntervalMin = intent.getIntExtra(EXTRA_MINDFULNESS_INTERVAL_MIN, 0)
                val completionBell = intent.getBooleanExtra(EXTRA_COMPLETION_BELL, false)
                val isStartBell = intent.getBooleanExtra(EXTRA_START_BELL, false)
                val initialElapsedSec = intent.getIntExtra(EXTRA_INITIAL_ELAPSED_SEC, 0)
                val initialStartTimeMs = intent.getLongExtra(EXTRA_INITIAL_START_TIME_MS, 0L)
                startTimer(
                    targetSec, prepSec, autoDnd, openEnded, isOverrunEnabled, soundBell,
                    mindfulnessIntervalMin, completionBell, isStartBell, initialElapsedSec, initialStartTimeMs
                )
            }
            ACTION_ADJUST_TIME -> {
                val deltaSec = intent.getIntExtra(EXTRA_ADJUST_SEC, 0)
                adjustTimerDuration(deltaSec)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_FINISH -> finishTimer()
            ACTION_CANCEL -> cancelTimer()
        }
        return START_NOT_STICKY
    }

    private fun adjustTimerDuration(deltaSec: Int) {
        if (isOpenEnded) {
            if (deltaSec > 0) {
                isOpenEnded = false
                targetDurationSec = elapsedSeconds + deltaSec
            } else {
                return
            }
        } else {
            val minTarget = (elapsedSeconds + 10).coerceAtLeast(60)
            val newTarget = (targetDurationSec + deltaSec).coerceAtLeast(minTarget)
            targetDurationSec = newTarget
        }
        soundHelper.triggerSoftTouchHaptic()

        val newRemaining = (targetDurationSec - elapsedSeconds).coerceAtLeast(0)
        val current = _timerStatus.value
        if (current is TimerStatus.Running) {
            _timerStatus.value = current.copy(
                remainingSeconds = newRemaining,
                elapsedSeconds = elapsedSeconds,
                isOverrun = false
            )
        } else if (current is TimerStatus.Paused) {
            _timerStatus.value = current.copy(
                remainingSeconds = newRemaining,
                elapsedSeconds = elapsedSeconds,
                isOverrun = false
            )
        }
    }

    private fun startTimer(
        targetSec: Int,
        prepSec: Int,
        autoDnd: Boolean,
        openEnded: Boolean,
        isOverrunEnabled: Boolean,
        soundBell: String,
        mindfulnessIntervalMin: Int,
        completionBell: Boolean,
        isStartBell: Boolean,
        initialElapsedSec: Int = 0,
        initialStartTimeMs: Long = 0L
    ) {
        targetDurationSec = targetSec
        prepTimeSec = prepSec
        isDndEnabledForSession = autoDnd
        isOpenEnded = openEnded
        isOverrunEnabledForSession = isOverrunEnabled
        soundBellResName = soundBell
        isCompletionBellEnabledForSession = completionBell
        isStartBellEnabledForSession = isStartBell
        elapsedSeconds = initialElapsedSec
        overrunSeconds = 0
        hasChimedForOverrun = false
        startTimeEpochMs = if (initialStartTimeMs > 0L) initialStartTimeMs else System.currentTimeMillis()

        wakeLockHelper.acquireWakeLock()

        if (isDndEnabledForSession) {
            dndManager.enableDnd()
        }

        // Set initial timer status immediately so UI updates instantaneously without a 1s lag
        if (prepSec > 0) {
            _timerStatus.value = TimerStatus.Preparing(prepSec)
        } else if (openEnded) {
            _timerStatus.value = TimerStatus.Running(
                remainingSeconds = 0,
                elapsedSeconds = elapsedSeconds,
                isOverrun = false
            )
        } else {
            _timerStatus.value = TimerStatus.Running(
                remainingSeconds = (targetSec - elapsedSeconds).coerceAtLeast(0),
                elapsedSeconds = elapsedSeconds,
                isOverrun = false
            )
        }

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            // 1. Preparation Phase (if any)
            var currentPrep = prepSec
            val countdownHaptics = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE).getBoolean("countdown_haptics", true)
            while (currentPrep > 0 && isActive) {
                _timerStatus.value = TimerStatus.Preparing(currentPrep)
                updateNotification(
                    timeText = "Starting in $currentPrep s",
                    subText = "Take a comfortable posture",
                    isPaused = false,
                    isOverrun = false
                )
                if (countdownHaptics) {
                    soundHelper.triggerSoftTouchHaptic()
                }
                delay(1000L)
                currentPrep--
            }

            if (prepSec > 0 && countdownHaptics) {
                soundHelper.triggerGentleVibration()
            }

            // Beginning chime (Only plays if explicitly enabled by user)
            if (isStartBellEnabledForSession) {
                soundHelper.playChime(soundBellResName, serviceScope)
            }

            // 2. Active Session Loop
            val intervalSec = mindfulnessIntervalMin * 60
            while (isActive) {
                elapsedSeconds++

                // Mindfulness Interval Bell (Chimes periodically if enabled)
                if (intervalSec > 0 && elapsedSeconds > 0 && elapsedSeconds % intervalSec == 0) {
                    // Only chime if not right at the end of a timed session (which plays the completion chime)
                    if (isOpenEnded || elapsedSeconds != targetDurationSec) {
                        soundHelper.triggerHarmonicIntervalHaptic()
                        soundHelper.playChime(soundBellResName, serviceScope)
                    }
                }

                if (isOpenEnded) {
                    // Open-ended Stopwatch mode
                    _timerStatus.value = TimerStatus.Running(
                        remainingSeconds = 0,
                        elapsedSeconds = elapsedSeconds,
                        isOverrun = false
                    )
                    updateNotification(
                        timeText = NotificationHelper.formatSeconds(elapsedSeconds),
                        subText = "Open-Ended Practice",
                        isPaused = false,
                        isOverrun = false
                    )
                } else {
                    // Target Countdown & Overrun mode
                    val remaining = targetDurationSec - elapsedSeconds
                    if (remaining > 0) {
                        _timerStatus.value = TimerStatus.Running(
                            remainingSeconds = remaining,
                            elapsedSeconds = elapsedSeconds,
                            isOverrun = false
                        )
                        updateNotification(
                            timeText = NotificationHelper.formatSeconds(remaining),
                            subText = "Elapsed: ${NotificationHelper.formatSeconds(elapsedSeconds)}",
                            isPaused = false,
                            isOverrun = false
                        )
                    } else {
                        if (isOverrunEnabledForSession) {
                            // In Overrun / Samadhi Extended Mode
                            overrunSeconds = elapsedSeconds - targetDurationSec
                            
                            if (!hasChimedForOverrun) {
                                hasChimedForOverrun = true
                                if (isCompletionBellEnabledForSession) {
                                    soundHelper.playChime(soundBellResName, serviceScope)
                                }
                                soundHelper.triggerHarmonicCompletionHaptic()
                            }

                            _timerStatus.value = TimerStatus.Running(
                                remainingSeconds = 0,
                                elapsedSeconds = elapsedSeconds,
                                isOverrun = true
                            )
                            updateNotification(
                                timeText = "+${NotificationHelper.formatSeconds(overrunSeconds)}",
                                subText = "Extended Time • Total: ${NotificationHelper.formatSeconds(elapsedSeconds)}",
                                isPaused = false,
                                isOverrun = true
                            )
                        } else {
                            // Overrun is OFF: Target reached, complete session immediately!
                            if (!hasChimedForOverrun) {
                                hasChimedForOverrun = true
                                if (isCompletionBellEnabledForSession) {
                                    soundHelper.playChime(soundBellResName, serviceScope)
                                }
                                soundHelper.triggerHarmonicCompletionHaptic()
                            }
                            finishTimer(showNotificationIfBackground = true)
                            break
                        }
                    }
                }

                delay(1000L)
            }
        }
    }

    private fun pauseTimer() {
        val current = _timerStatus.value
        if (current is TimerStatus.Running) {
            timerJob?.cancel()
            _timerStatus.value = TimerStatus.Paused(
                remainingSeconds = current.remainingSeconds,
                elapsedSeconds = current.elapsedSeconds,
                isOverrun = current.isOverrun
            )
            updateNotification(
                timeText = if (current.isOverrun) "+${NotificationHelper.formatSeconds(overrunSeconds)}" else NotificationHelper.formatSeconds(current.remainingSeconds),
                subText = "Paused",
                isPaused = true,
                isOverrun = current.isOverrun
            )
            wakeLockHelper.releaseWakeLock()
        }
    }

    private fun resumeTimer() {
        val current = _timerStatus.value
        if (current is TimerStatus.Paused) {
            wakeLockHelper.acquireWakeLock()
            timerJob = serviceScope.launch {
                while (isActive) {
                    elapsedSeconds++

                    if (isOpenEnded) {
                        _timerStatus.value = TimerStatus.Running(
                            remainingSeconds = 0,
                            elapsedSeconds = elapsedSeconds,
                            isOverrun = false
                        )
                        updateNotification(
                            timeText = NotificationHelper.formatSeconds(elapsedSeconds),
                            subText = "Open-Ended Practice",
                            isPaused = false,
                            isOverrun = false
                        )
                    } else {
                        val remaining = targetDurationSec - elapsedSeconds
                        if (remaining > 0) {
                            _timerStatus.value = TimerStatus.Running(
                                remainingSeconds = remaining,
                                elapsedSeconds = elapsedSeconds,
                                isOverrun = false
                            )
                            updateNotification(
                                timeText = NotificationHelper.formatSeconds(remaining),
                                subText = "Elapsed: ${NotificationHelper.formatSeconds(elapsedSeconds)}",
                                isPaused = false,
                                isOverrun = false
                            )
                        } else {
                            if (isOverrunEnabledForSession) {
                                overrunSeconds = elapsedSeconds - targetDurationSec
                                if (!hasChimedForOverrun) {
                                    hasChimedForOverrun = true
                                    if (isCompletionBellEnabledForSession) {
                                        soundHelper.playChime(soundBellResName, serviceScope)
                                    }
                                    soundHelper.triggerHarmonicCompletionHaptic()
                                }
                                _timerStatus.value = TimerStatus.Running(
                                    remainingSeconds = 0,
                                    elapsedSeconds = elapsedSeconds,
                                    isOverrun = true
                                )
                                updateNotification(
                                    timeText = "+${NotificationHelper.formatSeconds(overrunSeconds)}",
                                    subText = "Extended Time • Total: ${NotificationHelper.formatSeconds(elapsedSeconds)}",
                                    isPaused = false,
                                    isOverrun = true
                                )
                            } else {
                                if (!hasChimedForOverrun) {
                                    hasChimedForOverrun = true
                                    if (isCompletionBellEnabledForSession) {
                                        soundHelper.playChime(soundBellResName, serviceScope)
                                    }
                                    soundHelper.triggerHarmonicCompletionHaptic()
                                }
                                finishTimer(showNotificationIfBackground = true)
                                break
                            }
                        }
                    }

                    delay(1000L)
                }
            }
        }
    }

    private fun finishTimer(showNotificationIfBackground: Boolean = true) {
        timerJob?.cancel()
        val completedMs = System.currentTimeMillis()

        _timerStatus.value = TimerStatus.Completed(
            targetDurationSec = targetDurationSec,
            actualDurationSec = elapsedSeconds,
            overrunSec = overrunSeconds,
            startTimeEpochMs = startTimeEpochMs,
            completedTimeEpochMs = completedMs
        )

        if (isDndEnabledForSession) {
            dndManager.restoreDnd()
        }
        wakeLockHelper.releaseWakeLock()

        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationHelper.clearTimerNotification()

        if (showNotificationIfBackground && !MeditationApp.isAppInForeground) {
            val totalSec = elapsedSeconds
            notificationHelper.showCompletionNotification(
                title = "Meditation Complete 🪷",
                message = "Completed ${NotificationHelper.formatSeconds(totalSec)} of meditation."
            )
        }

        stopSelf()
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.Idle
        cleanupAndStop()
    }

    private fun cleanupAndStop() {
        if (isDndEnabledForSession) {
            dndManager.restoreDnd()
        }
        wakeLockHelper.releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationHelper.clearTimerNotification()
        stopSelf()
    }

    private fun startForegroundServiceWithNotification(
        timeText: String,
        subText: String,
        isPaused: Boolean,
        isOverrun: Boolean
    ) {
        val notification = notificationHelper.buildTimerNotification(
            timeText = timeText,
            subText = subText,
            isPaused = isPaused,
            isOverrun = isOverrun
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(
        timeText: String,
        subText: String,
        isPaused: Boolean,
        isOverrun: Boolean
    ) {
        val notification = notificationHelper.buildTimerNotification(
            timeText = timeText,
            subText = subText,
            isPaused = isPaused,
            isOverrun = isOverrun
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        cleanupAndStop()
        super.onDestroy()
        Log.d(TAG, "MeditationTimerService destroyed")
    }

    companion object {
        private const val TAG = "MeditationTimerService"

        const val ACTION_START = "com.samadhi.meditationtimer.action.START"
        const val ACTION_PAUSE = "com.samadhi.meditationtimer.action.PAUSE"
        const val ACTION_RESUME = "com.samadhi.meditationtimer.action.RESUME"
        const val ACTION_FINISH = "com.samadhi.meditationtimer.action.FINISH"
        const val ACTION_CANCEL = "com.samadhi.meditationtimer.action.CANCEL"
        const val ACTION_ADJUST_TIME = "com.samadhi.meditationtimer.action.ADJUST_TIME"

        const val EXTRA_TARGET_SEC = "extra_target_sec"
        const val EXTRA_PREP_SEC = "extra_prep_sec"
        const val EXTRA_AUTO_DND = "extra_auto_dnd"
        const val EXTRA_OPEN_ENDED = "extra_open_ended"
        const val EXTRA_OVERRUN_ENABLED = "extra_overrun_enabled"
        const val EXTRA_SOUND_BELL = "extra_sound_bell"
        const val EXTRA_MINDFULNESS_INTERVAL_MIN = "extra_mindfulness_interval_min"
        const val EXTRA_COMPLETION_BELL = "extra_completion_bell"
        const val EXTRA_START_BELL = "extra_start_bell"
        const val EXTRA_ADJUST_SEC = "extra_adjust_sec"
        const val EXTRA_INITIAL_ELAPSED_SEC = "extra_initial_elapsed_sec"
        const val EXTRA_INITIAL_START_TIME_MS = "extra_initial_start_time_ms"

        private val _timerStatus = MutableStateFlow<TimerStatus>(TimerStatus.Idle)
        val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

        fun resetStatus() {
            _timerStatus.value = TimerStatus.Idle
        }
    }
}
