package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.UserDao
import com.fitlife.ai.data.local.entity.UserEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao
) {
    val currentUser = supabase.auth.currentUserOrNull()

    fun isLoggedIn() = currentUser != null

    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.auth.awaitInitialization()
            supabase.auth.currentUserOrNull() != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signUp(email: String, password: String) = withContext(Dispatchers.IO) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun sendPasswordReset(email: String) = withContext(Dispatchers.IO) {
        supabase.auth.resetPasswordForEmail(email)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        supabase.auth.signOut()
    }

    suspend fun getUserId(): String? =
        runCatching { supabase.auth.currentUserOrNull()?.id }.getOrNull()
            ?: userDao.getFirstUserId()

    suspend fun getCurrentUserId(): String {
        val fromAuth = runCatching { supabase.auth.currentUserOrNull()?.id }.getOrNull()
        if (!fromAuth.isNullOrBlank()) return fromAuth
        return userDao.getFirstUserId()
            ?: throw IllegalStateException("Not authenticated")
    }

    fun observeUser(userId: String): Flow<UserEntity?> = userDao.getUser(userId)

    suspend fun getUserOnce(userId: String): UserEntity? = userDao.getUserOnce(userId)

    suspend fun loadUserFromSupabase(): UserEntity? = withContext(Dispatchers.IO) {
        val userId = getUserId() ?: return@withContext null
        try {
            val profile = supabase.from("user_profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserEntity>()
            if (profile != null) {
                val local = userDao.getUserOnce(userId)
                val merged = if (local != null) profile.copy(
                    displayName = profile.displayName ?: local.displayName,
                    photoUrl = profile.photoUrl ?: local.photoUrl,
                    heightCm = profile.heightCm ?: local.heightCm,
                    weightKg = profile.weightKg ?: local.weightKg,
                    dateOfBirth = profile.dateOfBirth ?: local.dateOfBirth,
                    gender = profile.gender ?: local.gender,
                    fitnessGoal = profile.fitnessGoal ?: local.fitnessGoal,
                    activityLevel = profile.activityLevel ?: local.activityLevel,
                    workoutFrequency = profile.workoutFrequency ?: local.workoutFrequency,
                    equipment = profile.equipment ?: local.equipment,
                    injuries = profile.injuries ?: local.injuries,
                    lifestyle = profile.lifestyle ?: local.lifestyle,
                    sleepHours = profile.sleepHours ?: local.sleepHours,
                    stressLevel = profile.stressLevel ?: local.stressLevel,
                    cycleLength = profile.cycleLength ?: local.cycleLength,
                    lastPeriodStart = profile.lastPeriodStart ?: local.lastPeriodStart,
                    supportMode = profile.supportMode ?: local.supportMode,
                    birthControl = profile.birthControl ?: local.birthControl,
                    hydrationTargetMl = profile.hydrationTargetMl ?: local.hydrationTargetMl,
                    dietType = profile.dietType ?: local.dietType,
                    mealCount = profile.mealCount ?: local.mealCount,
                    calorieTarget = profile.calorieTarget ?: local.calorieTarget,
                    proteinTargetG = profile.proteinTargetG ?: local.proteinTargetG,
                    carbsTargetG = profile.carbsTargetG ?: local.carbsTargetG,
                    fatTargetG = profile.fatTargetG ?: local.fatTargetG
                ) else profile
                userDao.upsert(merged)
                merged
            } else profile
        } catch (e: Exception) { null }
    }

    suspend fun saveProfile(user: UserEntity) {
        userDao.upsert(user)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("user_profiles").upsert(user)
            }
        } catch (_: Exception) { }
    }
}
