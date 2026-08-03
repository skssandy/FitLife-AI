package com.fitlife.ai.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitlife.ai.data.local.AppDatabase
import com.fitlife.ai.data.local.dao.BloodReportDao
import com.fitlife.ai.data.local.dao.CalorieEntryDao
import com.fitlife.ai.data.local.dao.ChatMessageDao
import com.fitlife.ai.data.local.dao.CycleEntryDao
import com.fitlife.ai.data.local.dao.DailyMetricDao
import com.fitlife.ai.data.local.dao.FoodItemDao
import com.fitlife.ai.data.local.dao.SymptomLogDao
import com.fitlife.ai.data.local.dao.UserDao
import com.fitlife.ai.data.local.dao.WaterLogDao
import com.fitlife.ai.data.local.dao.WorkoutDao
import com.fitlife.ai.data.local.dao.WorkoutProgramDao
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

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS water_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "amountMl INTEGER NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "synced INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL)"
            )
            db.execSQL("ALTER TABLE users ADD COLUMN hydrationTargetMl INTEGER")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS food_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "servingSize TEXT NOT NULL, " +
                    "calories INTEGER NOT NULL, " +
                    "proteinG REAL NOT NULL, " +
                    "carbsG REAL NOT NULL, " +
                    "fatG REAL NOT NULL, " +
                    "barcode TEXT, " +
                    "source TEXT NOT NULL)"
            )
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN calorieTarget INTEGER")
            db.execSQL("ALTER TABLE users ADD COLUMN proteinTargetG INTEGER")
            db.execSQL("ALTER TABLE users ADD COLUMN carbsTargetG INTEGER")
            db.execSQL("ALTER TABLE users ADD COLUMN fatTargetG INTEGER")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS workout_programs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "goal TEXT NOT NULL, " +
                    "daysJson TEXT NOT NULL, " +
                    "synced INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL)"
            )
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS daily_metrics (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "steps INTEGER, " +
                    "heartRateAvg INTEGER, " +
                    "hrvAvg INTEGER, " +
                    "sleepMinutes INTEGER, " +
                    "sleepStagesJson TEXT, " +
                    "caloriesBurned INTEGER, " +
                    "activeMinutes INTEGER, " +
                    "weightKg REAL, " +
                    "bodyFatPct REAL, " +
                    "source TEXT NOT NULL, " +
                    "synced INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_daily_metrics_userId_date " +
                    "ON daily_metrics (userId, date)"
            )
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS cycle_entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "startDate INTEGER NOT NULL, " +
                    "flowLevel TEXT NOT NULL, " +
                    "symptomsJson TEXT NOT NULL, " +
                    "notes TEXT NOT NULL, " +
                    "synced INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS symptom_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "symptomsJson TEXT NOT NULL, " +
                    "synced INTEGER NOT NULL DEFAULT 0, " +
                    "createdAt INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_symptom_logs_userId_date " +
                    "ON symptom_logs (userId, date)"
            )
            db.execSQL("ALTER TABLE users ADD COLUMN supportMode TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "fitlife_db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()
    @Provides fun provideCalorieEntryDao(db: AppDatabase): CalorieEntryDao = db.calorieEntryDao()
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
    @Provides fun provideBloodReportDao(db: AppDatabase): BloodReportDao = db.bloodReportDao()
    @Provides fun provideWaterLogDao(db: AppDatabase): WaterLogDao = db.waterLogDao()
    @Provides fun provideFoodItemDao(db: AppDatabase): FoodItemDao = db.foodItemDao()
    @Provides fun provideWorkoutProgramDao(db: AppDatabase): WorkoutProgramDao = db.workoutProgramDao()
    @Provides fun provideDailyMetricDao(db: AppDatabase): DailyMetricDao = db.dailyMetricDao()
    @Provides fun provideCycleEntryDao(db: AppDatabase): CycleEntryDao = db.cycleEntryDao()
    @Provides fun provideSymptomLogDao(db: AppDatabase): SymptomLogDao = db.symptomLogDao()
}
