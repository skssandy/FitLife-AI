package com.fitlife.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fitlife.ai.data.local.dao.*
import com.fitlife.ai.data.local.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        WorkoutProgramEntity::class,
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        NutritionLogEntity::class,
        WaterLogEntity::class,
        BloodAnalysisEntity::class,
        BloodMarkerEntity::class,
        CycleEntryEntity::class,
        ProgressEntryEntity::class,
        SyncQueueEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun workoutDao(): WorkoutProgramDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun bloodAnalysisDao(): BloodAnalysisDao
    abstract fun cycleDao(): CycleDao
    abstract fun progressDao(): ProgressDao
    abstract fun syncDao(): SyncDao
}
