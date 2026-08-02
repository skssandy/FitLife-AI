package com.fitlife.ai.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitlife.ai.data.local.AppDatabase
import com.fitlife.ai.data.local.dao.BloodReportDao
import com.fitlife.ai.data.local.dao.CalorieEntryDao
import com.fitlife.ai.data.local.dao.ChatMessageDao
import com.fitlife.ai.data.local.dao.UserDao
import com.fitlife.ai.data.local.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN workoutFrequency TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN equipment TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN injuries TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN lifestyle TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN sleepHours REAL")
            db.execSQL("ALTER TABLE users ADD COLUMN stressLevel TEXT")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS blood_reports (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "reportDate INTEGER NOT NULL, " +
                    "source TEXT NOT NULL, " +
                    "rawText TEXT, " +
                    "markersJson TEXT, " +
                    "analysisText TEXT, " +
                    "synced INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL)"
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN cycleLength INTEGER")
            db.execSQL("ALTER TABLE users ADD COLUMN lastPeriodStart INTEGER")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "fitlife_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()
    @Provides fun provideCalorieEntryDao(db: AppDatabase): CalorieEntryDao = db.calorieEntryDao()
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
    @Provides fun provideBloodReportDao(db: AppDatabase): BloodReportDao = db.bloodReportDao()
}
