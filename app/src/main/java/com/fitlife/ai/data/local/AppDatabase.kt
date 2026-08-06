package com.fitlife.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.fitlife.ai.data.local.entity.BloodReportEntity
import com.fitlife.ai.data.local.entity.CalorieEntryEntity
import com.fitlife.ai.data.local.entity.ChatMessageEntity
import com.fitlife.ai.data.local.entity.CycleEntryEntity
import com.fitlife.ai.data.local.entity.DailyMetricEntity
import com.fitlife.ai.data.local.entity.FoodItemEntity
import com.fitlife.ai.data.local.entity.SymptomLogEntity
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.local.entity.WaterLogEntity
import com.fitlife.ai.data.local.entity.WorkoutEntity
import com.fitlife.ai.data.local.entity.WorkoutProgramEntity

@Database(
    entities = [
        UserEntity::class,
        WorkoutEntity::class,
        CalorieEntryEntity::class,
        ChatMessageEntity::class,
        BloodReportEntity::class,
        WaterLogEntity::class,
        FoodItemEntity::class,
        WorkoutProgramEntity::class,
        DailyMetricEntity::class,
        CycleEntryEntity::class,
        SymptomLogEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun calorieEntryDao(): CalorieEntryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun bloodReportDao(): BloodReportDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun workoutProgramDao(): WorkoutProgramDao
    abstract fun dailyMetricDao(): DailyMetricDao
    abstract fun cycleEntryDao(): CycleEntryDao
    abstract fun symptomLogDao(): SymptomLogDao
}
