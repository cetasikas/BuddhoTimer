package com.samadhi.meditationtimer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samadhi.meditationtimer.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY completedTimeEpochMs DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    fun getSessionByIdFlow(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY completedTimeEpochMs DESC")
    suspend fun getAllSessionsList(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT COUNT(*) FROM sessions")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT SUM(actualDurationSec) FROM sessions")
    fun getTotalDurationSecFlow(): Flow<Long?>

    @Query("SELECT SUM(overrunSec) FROM sessions")
    fun getTotalOverrunSecFlow(): Flow<Long?>
}
