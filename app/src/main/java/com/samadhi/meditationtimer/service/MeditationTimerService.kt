package com.samadhi.meditationtimer.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
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

    private var sessionActiveStartRealtimeMs: Long = 0L
    private var baseElapsedSec: Int = 0
    private var lastMindfulnessChimeInterval: Int = 0

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
            ACTION_SET_OVERRUN -> {
                val enabled = intent.getBooleanExtra(EXTRA_OVERRUN_ENABLED, true)
                isOverrunEnabledForSession = enabled
                soundHelper.triggerSoftTouchHaptic()
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
            // Rebase chronometer to the new remaining duration
            val newChrono = System.currentTimeMillis() + (newRemaining * 1000L)
            updateNotification(
                contentText = "${targetDurationSec / 60} min session",
                isPaused = false,
                isOverrun = false,
                chronoBaseMs = newChrono,
                isCountdown = true,
            )
        } else if (current is TimerStatus.Paused) {
            _timerStatus.value = current.copy(
                remainingSeconds = newRemaining,
                elapsedSeconds = elapsedSeconds,
                isOverrun = false
            )
            updateNotification(
                contentText = "${targetDurationSec / 60} min session",
                isPaused = true,
                isOverrun = false,
                chronoBaseMs = 0L,
                isCountdown = false,
                staticTimeText = NotificationHelper.formatSeconds(newRemaining)
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
        baseElapsedSec = initialElapsedSec
        overrunSeconds = 0
        hasChimedForOverrun = false
        lastMindfulnessChimeInterval = if (mindfulnessIntervalMin > 0 && initialElapsedSec > 0) {
            initialElapsedSec / (mindfulnessIntervalMin * 60)
        } else 0
        startTimeEpochMs = if (initialStartTimeMs > 0L) initialStartTimeMs else System.currentTimeMillis()

        wakeLockHelper.acquireWakeLock()

        if (isDndEnabledForSession) {
            dndManager.enableDnd()
        }

        // Promote service to foreground immediately
        val now = System.currentTimeMillis()
        val initialChrono = when {
            prepSec > 0 -> now + (prepSec * 1000L)
            openEnded   -> startTimeEpochMs
            else        -> now + ((targetSec - initialElapsedSec).coerceAtLeast(0) * 1000L)
        }
        val initialContentText = when {
            prepSec > 0 -> "Take a comfortable posture"
            openEnded   -> "Open-Ended Practice"
            else        -> "${targetSec / 60} min session"
        }

        startForegroundServiceWithNotification(
            contentText = initialContentText,
            isPaused = false,
            isOverrun = false,
            chronoBaseMs = initialChrono,
            isCountdown = !openEnded,
        )

        // Set initial timer status immediately
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
            // 3. Preparation Phase (if any)
            var currentPrep = prepSec
            val countdownHaptics = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE).getBoolean("countdown_haptics", true)
            while (currentPrep > 0 && isActive) {
                _timerStatus.value = TimerStatus.Preparing(currentPrep)
                if (countdownHaptics) {
                    soundHelper.triggerSoftTouchHaptic()
                }
                delay(1000L)
                currentPrep--
            }

            if (prepSec > 0 && countdownHaptics) {
                soundHelper.triggerGentleVibration()
            }

            // Beginning chime
            if (isStartBellEnabledForSession) {
                soundHelper.playChime(soundBellResName, serviceScope)
            }

            // 4. Active Session Loop with Monotonic Clock
            sessionActiveStartRealtimeMs = SystemClock.elapsedRealtime()
            val intervalSec = mindfulnessIntervalMin * 60

            val sessionStartNow = System.currentTimeMillis()
            val sessionChrono = when {
                openEnded -> startTimeEpochMs
                else      -> sessionStartNow + ((targetDurationSec - elapsedSeconds).coerceAtLeast(0) * 1000L)
            }
            updateNotification(
                contentText = if (openEnded) "Open-Ended Practice" else "${targetDurationSec / 60} min session",
                isPaused = false,
                isOverrun = false,
                chronoBaseMs = sessionChrono,
                isCountdown = !openEnded,
            )

            while (isActive) {
                val currentRealtimeMs = SystemClock.elapsedRealtime()
                val elapsedSinceStartSec = ((currentRealtimeMs - sessionActiveStartRealtimeMs) / 1000L).toInt()
                elapsedSeconds = baseElapsedSec + elapsedSinceStartSec

                // Mindfulness Interval Bell
                if (intervalSec > 0 && elapsedSeconds > 0) {
                    val currentIntervalIndex = elapsedSeconds / intervalSec
                    if (currentIntervalIndex > lastMindfulnessChimeInterval) {
                        lastMindfulnessChimeInterval = currentIntervalIndex
                        if (isOpenEnded || elapsedSeconds != targetDurationSec) {
                            soundHelper.triggerHarmonicIntervalHaptic()
                            soundHelper.playChime(soundBellResName, serviceScope)
                        }
                    }
                }

                if (isOpenEnded) {
                    _timerStatus.value = TimerStatus.Running(
                        remainingSeconds = 0,
                        elapsedSeconds = elapsedSeconds,
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
                    } else {
                        if (isOverrunEnabledForSession) {
                            overrunSeconds = elapsedSeconds - targetDurationSec

                            if (!hasChimedForOverrun) {
                                hasChimedForOverrun = true
                                if (isCompletionBellEnabledForSession) {
                                    soundHelper.playChime(soundBellResName, serviceScope)
                                }
                                soundHelper.triggerHarmonicCompletionHaptic()

                                val overrunChrono = System.currentTimeMillis() - (overrunSeconds * 1000L)
                                updateNotification(
                                    contentText = "Overrun • Mindful Stillness",
                                    isPaused = false,
                                    isOverrun = true,
                                    chronoBaseMs = overrunChrono,
                                    isCountdown = false,
                                )
                            }

                            _timerStatus.value = TimerStatus.Running(
                                remainingSeconds = 0,
                                elapsedSeconds = elapsedSeconds,
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

    private fun pauseTimer() {
        val current = _timerStatus.value
        if (current is TimerStatus.Running) {
            timerJob?.cancel()
            baseElapsedSec = elapsedSeconds
            _timerStatus.value = TimerStatus.Paused(
                remainingSeconds = current.remainingSeconds,
                elapsedSeconds = current.elapsedSeconds,
                isOverrun = current.isOverrun
            )
            val pausedTimeText = if (current.isOverrun) {
                "+${NotificationHelper.formatSeconds(overrunSeconds)}"
            } else {
                NotificationHelper.formatSeconds(current.remainingSeconds)
            }
            updateNotification(
                contentText = "Paused",
                isPaused = true,
                isOverrun = current.isOverrun,
                chronoBaseMs = 0L,
                isCountdown = false,
                staticTimeText = pausedTimeText,
            )
            wakeLockHelper.releaseWakeLock()
        }
    }

    private fun resumeTimer() {
        val current = _timerStatus.value
        if (current is TimerStatus.Paused) {
            wakeLockHelper.acquireWakeLock()
            sessionActiveStartRealtimeMs = SystemClock.elapsedRealtime()

            val resumeNow = System.currentTimeMillis()
            val resumeChrono = when {
                isOpenEnded      -> startTimeEpochMs
                current.isOverrun -> resumeNow - (overrunSeconds * 1000L)
                else              -> resumeNow + (current.remainingSeconds * 1000L)
            }
            val resumeContentText = when {
                isOpenEnded       -> "Open-Ended Practice"
                current.isOverrun -> "Overrun • Mindful Stillness"
                else              -> "${targetDurationSec / 60} min session"
            }
            updateNotification(
                contentText = resumeContentText,
                isPaused = false,
                isOverrun = current.isOverrun,
                chronoBaseMs = resumeChrono,
                isCountdown = !isOpenEnded && !current.isOverrun,
            )

            timerJob = serviceScope.launch {
                while (isActive) {
                    val currentRealtimeMs = SystemClock.elapsedRealtime()
                    val elapsedSinceResumeSec = ((currentRealtimeMs - sessionActiveStartRealtimeMs) / 1000L).toInt()
                    elapsedSeconds = baseElapsedSec + elapsedSinceResumeSec

                    if (isOpenEnded) {
                        _timerStatus.value = TimerStatus.Running(
                            remainingSeconds = 0,
                            elapsedSeconds = elapsedSeconds,
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
                        } else {
                            if (isOverrunEnabledForSession) {
                                overrunSeconds = elapsedSeconds - targetDurationSec
                                if (!hasChimedForOverrun) {
                                    hasChimedForOverrun = true
                                    if (isCompletionBellEnabledForSession) {
                                        soundHelper.playChime(soundBellResName, serviceScope)
                                    }
                                    soundHelper.triggerHarmonicCompletionHaptic()
                                    val overrunChrono = System.currentTimeMillis() - (overrunSeconds * 1000L)
                                    updateNotification(
                                        contentText = "Overrun • Mindful Stillness",
                                        isPaused = false,
                                        isOverrun = true,
                                        chronoBaseMs = overrunChrono,
                                        isCountdown = false,
                                    )
                                }
                                _timerStatus.value = TimerStatus.Running(
                                    remainingSeconds = 0,
                                    elapsedSeconds = elapsedSeconds,
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
        contentText: String,
        isPaused: Boolean,
        isOverrun: Boolean,
        chronoBaseMs: Long,
        isCountdown: Boolean,
        staticTimeText: String = "",
    ) {
        val notification = notificationHelper.buildTimerNotification(
            contentText = contentText,
            isPaused = isPaused,
            isOverrun = isOverrun,
            chronoBaseMs = chronoBaseMs,
            isCountdown = isCountdown,
            staticTimeText = staticTimeText,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
        contentText: String,
        isPaused: Boolean,
        isOverrun: Boolean,
        chronoBaseMs: Long,
        isCountdown: Boolean,
        staticTimeText: String = "",
    ) {
        val notification = notificationHelper.buildTimerNotification(
            contentText = contentText,
            isPaused = isPaused,
            isOverrun = isOverrun,
            chronoBaseMs = chronoBaseMs,
            isCountdown = isCountdown,
            staticTimeText = staticTimeText,
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
        const val ACTION_SET_OVERRUN = "com.samadhi.meditationtimer.action.SET_OVERRUN"

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
