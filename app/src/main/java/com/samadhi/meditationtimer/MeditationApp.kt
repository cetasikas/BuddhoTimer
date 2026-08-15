package com.samadhi.meditationtimer

import android.app.Application
import com.samadhi.meditationtimer.data.local.AppDatabase
import com.samadhi.meditationtimer.data.repository.SessionRepositoryImpl
import com.samadhi.meditationtimer.domain.repository.SessionRepository

class MeditationApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(database.sessionDao())
    }

    override fun onCreate() {
        super.onCreate()
    }
}
