package com.samadhi.meditationtimer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.samadhi.meditationtimer.data.local.entity.CompletedSessionEntity

@Dao
interface CompletedSessionDao {
    @Insert
    suspend fun insert(session: CompletedSessionEntity): Long

    @Query("SELECT COUNT(*) FROM completed_sessions WHERE completedTimeEpochMs >= :sinceEpochMs")
    suspend fun countSince(sinceEpochMs: Long): Int

    @Query("SELECT COALESCE(SUM(actualDurationSec), 0) FROM completed_sessions WHERE completedTimeEpochMs >= :sinceEpochMs")
    suspend fun totalDurationSecSince(sinceEpochMs: Long): Long
}