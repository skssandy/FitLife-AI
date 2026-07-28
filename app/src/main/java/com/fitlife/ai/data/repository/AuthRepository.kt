package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.UserProfileDao
import com.fitlife.ai.data.local.entity.UserProfileEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SupabaseUserProfile(
    val id: String = "",
    val email: String = "",
    val full_name: String = "",
    val gender: String = "male",
    val date_of_birth: Long = 0L,
    val height_cm: Double = 0.0,
    val weight_kg: Double = 0.0,
    val fitness_goal: String = "general_fitness",
    val activity_level: String = "moderate",
    val dietary_restrictions: List<String> = emptyList(),
    val medical_conditions: List<String> = emptyList(),
    val onboarding_completed: Boolean = false,
    val onboarding_step: Int = 0,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userProfileDao: UserProfileDao
) {
    val currentUserId: String?
        get() = supabase.auth.currentSessionOrNull()?.user?.id

    val isLoggedIn: Boolean
        get() = supabase.auth.currentSessionOrNull() != null

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: ""
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(email: String, password: String, fullName: String): Result<String> {
        return try {
            val result = supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                }
            }
            val userId = supabase.auth.currentSessionOrNull()?.user?.id
                ?: result.user?.id?.toString()
                ?: ""
            if (userId.isNotEmpty()) {
                createProfile(userId, email, fullName)
            }
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createProfile(userId: String, email: String, fullName: String) {
        val profile = UserProfileEntity(
            id = userId,
            email = email,
            fullName = fullName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userProfileDao.upsert(profile)
        try {
            supabase.from("user_profiles").insert(
                SupabaseUserProfile(
                    id = userId,
                    email = email,
                    full_name = fullName,
                    created_at = System.currentTimeMillis(),
                    updated_at = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) { }
    }
}
