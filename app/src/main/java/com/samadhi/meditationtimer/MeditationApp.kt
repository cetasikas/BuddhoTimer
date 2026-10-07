package com.samadhi.meditationtimer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.samadhi.meditationtimer.data.local.AppDatabase
import com.samadhi.meditationtimer.data.repository.SessionRepositoryImpl
import com.samadhi.meditationtimer.domain.repository.SessionRepository

class MeditationApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(database.sessionDao(), database.completedSessionDao())
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var resumedActivities = 0

            override fun onActivityResumed(activity: Activity) {
                resumedActivities++
                isAppInForeground = resumedActivities > 0
            }

            override fun onActivityPaused(activity: Activity) {
                resumedActivities = (resumedActivities - 1).coerceAtLeast(0)
                isAppInForeground = resumedActivities > 0
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
            internal set
    }
}
