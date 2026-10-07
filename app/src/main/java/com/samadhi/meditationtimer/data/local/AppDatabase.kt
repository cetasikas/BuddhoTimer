package com.samadhi.meditationtimer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.samadhi.meditationtimer.data.local.dao.CompletedSessionDao
import com.samadhi.meditationtimer.data.local.dao.SessionDao
import com.samadhi.meditationtimer.data.local.entity.CompletedSessionEntity
import com.samadhi.meditationtimer.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, CompletedSessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun completedSessionDao(): CompletedSessionDao

    companion object {
        private const val DB_NAME = "samadhi_meditation.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `completed_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`completedTimeEpochMs` INTEGER NOT NULL, " +
                        "`actualDurationSec` INTEGER NOT NULL, " +
                        "`isBreathing` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}