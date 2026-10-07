package com.samadhi.meditationtimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.screenTimeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "screen_time_prefs"
)

private val KEY_TRACKING_DISMISSED = booleanPreferencesKey("tracking_prompt_dismissed")
private val KEY_TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("screen_time_notification_enabled")

/**
 * Persisted settings for the Screen Usage screen.
 *
 * The daily goal is optional and defaults to off. Zero means "no target", which
 * every chart reads as "show the data without a reference marker" rather than
 * as a failed goal.
 */
class ScreenTimePreferences(private val context: Context) {

    private val dailyGoalKey = intPreferencesKey("daily_goal_minutes")

    /** Foreground minutes per day the user is aiming at, or 0 when unset. */
    val dailyGoalMinutes: Flow<Int> = context.screenTimeDataStore.data.map { prefs ->
        prefs[dailyGoalKey] ?: 0
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        val safe = minutes.coerceIn(0, 24 * 60)
        context.screenTimeDataStore.edit { prefs ->
            prefs[dailyGoalKey] = safe
        }
        // Keep the persistent status notification in step with goal changes.
        com.samadhi.meditationtimer.service.screentime.ScreenTimeNotification.showOrUpdate(context)
    }

    /**
     * Whether the optional persistent screen time notification (lock screen
     * and shade) is on. Defaults off; independent of the meditation channels.
     */
    val screenTimeNotificationEnabled: Flow<Boolean> =
        context.screenTimeDataStore.data.map { prefs ->
            prefs[KEY_NOTIFICATION_ENABLED] ?: false
        }

    suspend fun setScreenTimeNotificationEnabled(enabled: Boolean) {
        context.screenTimeDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_ENABLED] = enabled
        }
    }

    /** Goal in milliseconds, or 0 when no goal is set. */
    fun goalMs(minutes: Int): Long = if (minutes > 0) minutes * 60_000L else 0L

    /**
     * False until the user has answered the screen time tracking prompt.
     *
     * The prompt is shown once. Choosing "not now" still records an answer so
     * the card does not return on every visit; tracking can be started later
     * from Settings.
     */
    val trackingPromptAnswered: Flow<Boolean> = context.screenTimeDataStore.data.map { prefs ->
        prefs[KEY_TRACKING_DISMISSED] ?: false
    }

    suspend fun setTrackingPromptAnswered(answered: Boolean) {
        context.screenTimeDataStore.edit { prefs ->
            prefs[KEY_TRACKING_DISMISSED] = answered
        }
    }

    /**
     * Whether the user has turned screen time tracking on.
     *
     * Defaults to false because the prompt is shown before the choice is made,
     * so the first read cannot assume consent.
     */
    val trackingEnabled: Flow<Boolean> = context.screenTimeDataStore.data.map { prefs ->
        prefs[KEY_TRACKING_ENABLED] ?: false
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.screenTimeDataStore.edit { prefs ->
            prefs[KEY_TRACKING_ENABLED] = enabled
            prefs[KEY_TRACKING_DISMISSED] = true
        }
    }

    companion object {
        /** Selectable targets, in minutes. 0 turns the goal off. */
        val goalPresets = listOf(0, 60, 120, 180, 240, 300)

        fun presetLabel(minutes: Int): String = when {
            minutes <= 0 -> "Off"
            minutes % 60 == 0 -> "${minutes / 60}h"
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}
