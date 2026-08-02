package com.fitlife.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fitlife.ai.data.local.dao.BloodReportDao
import com.fitlife.ai.data.local.dao.CalorieEntryDao
import com.fitlife.ai.data.local.dao.ChatMessageDao
import com.fitlife.ai.data.local.dao.UserDao
import com.fitlife.ai.data.local.dao.WorkoutDao
import com.fitlife.ai.data.local.entity.BloodReportEntity
import com.fitlife.ai.data.local.entity.CalorieEntryEntity
import com.fitlife.ai.data.local.entity.ChatMessageEntity
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.local.entity.WorkoutEntity

@Database(
    entities = [
        UserEntity::class,
        WorkoutEntity::class,
        CalorieEntryEntity::class,
        ChatMessageEntity::class,
        BloodReportEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun calorieEntryDao(): CalorieEntryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun bloodReportDao(): BloodReportDao
}
