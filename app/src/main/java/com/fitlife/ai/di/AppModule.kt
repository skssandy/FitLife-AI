package com.fitlife.ai.di

import android.content.Context
import androidx.room.Room
import com.fitlife.ai.data.local.AppDatabase
import com.fitlife.ai.data.local.dao.*
import com.fitlife.ai.data.remote.api.GeminiApi
import com.fitlife.ai.data.remote.api.GeminiService
import com.fitlife.ai.data.remote.SupabaseConfig
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.ProfileRepository
import com.fitlife.ai.health.HealthConnectManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fitlife.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides @Singleton
    fun provideGeminiApi(): GeminiApi = GeminiApi()

    @Provides @Singleton
    fun provideGeminiService(geminiApi: GeminiApi): GeminiService = GeminiService(geminiApi)

    @Provides @Singleton
    fun provideHealthConnectManager(@ApplicationContext context: Context): HealthConnectManager {
        return HealthConnectManager(context)
    }

    @Provides @Singleton
    fun provideAuthRepository(
        supabase: SupabaseClient,
        userProfileDao: UserProfileDao
    ): AuthRepository = AuthRepository(supabase, userProfileDao)

    @Provides @Singleton
    fun provideProfileRepository(
        supabase: SupabaseClient,
        userProfileDao: UserProfileDao
    ): ProfileRepository = ProfileRepository(supabase, userProfileDao)

    @Provides fun provideUserProfileDao(db: AppDatabase) = db.userProfileDao()
    @Provides fun provideWorkoutDao(db: AppDatabase) = db.workoutDao()
    @Provides fun provideWorkoutSessionDao(db: AppDatabase) = db.workoutSessionDao()
    @Provides fun provideNutritionDao(db: AppDatabase) = db.nutritionDao()
    @Provides fun provideBloodAnalysisDao(db: AppDatabase) = db.bloodAnalysisDao()
    @Provides fun provideCycleDao(db: AppDatabase) = db.cycleDao()
    @Provides fun provideProgressDao(db: AppDatabase) = db.progressDao()
    @Provides fun provideSyncDao(db: AppDatabase) = db.syncDao()
    @Provides fun provideWaterDao(db: AppDatabase) = db.waterDao()
    @Provides fun provideExerciseLogDao(db: AppDatabase) = db.exerciseLogDao()
}
