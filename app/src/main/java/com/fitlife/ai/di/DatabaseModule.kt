package com.fitlife.ai.di

import android.content.Context
import androidx.room.Room
import com.fitlife.ai.data.local.AppDatabase
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "fitlife_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()
    @Provides fun provideCalorieEntryDao(db: AppDatabase): CalorieEntryDao = db.calorieEntryDao()
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
}
