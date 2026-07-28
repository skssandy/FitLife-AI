package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.UserProfileDao
import com.fitlife.ai.data.local.entity.UserProfileEntity
import com.fitlife.ai.domain.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ProfileRow(
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
class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userProfileDao: UserProfileDao
) {
    fun getProfile(userId: String): Flow<UserProfile?> {
        return userProfileDao.getProfile(userId).map { it?.toDomain() }
    }

    fun getAnyProfile(): Flow<UserProfile?> {
        return userProfileDao.getAnyProfile().map { it?.toDomain() }
    }

    suspend fun refreshProfile(userId: String) {
        try {
            val remote = supabase.from("user_profiles")
                .select { filter { eq("id", userId) } }
                .decodeList<ProfileRow>()
                .firstOrNull()
            if (remote != null) {
                userProfileDao.upsert(remote.toEntity())
            }
        } catch (_: Exception) { }
    }

    suspend fun updateProfile(profile: UserProfile) {
        val entity = profile.toEntity()
        userProfileDao.upsert(entity)
        try {
            supabase.from("user_profiles").upsert(profile.toRow())
        } catch (_: Exception) { }
    }

    suspend fun updateOnboardingStep(userId: String, step: Int) {
        val existing = userProfileDao.getProfile(userId)
        userProfileDao.upsert(
            UserProfileEntity(
                id = userId,
                onboardingStep = step,
                onboardingCompleted = step >= 7,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun completeOnboarding(userId: String, profile: UserProfile) {
        val entity = profile.copy(onboardingCompleted = true, onboardingStep = 7).toEntity()
        userProfileDao.upsert(entity)
        try {
            supabase.from("user_profiles")
                .upsert(profile.copy(onboardingCompleted = true, onboardingStep = 7).toRow())
        } catch (_: Exception) { }
    }
}

private fun UserProfileEntity.toDomain() = UserProfile(
    id = id, email = email, fullName = fullName, gender = gender,
    dateOfBirth = dateOfBirth, heightCm = heightCm, weightKg = weightKg,
    fitnessGoal = fitnessGoal, activityLevel = activityLevel,
    dietaryRestrictions = try { Json.decodeFromString(dietaryRestrictions) } catch (_: Exception) { emptyList() },
    medicalConditions = try { Json.decodeFromString(medicalConditions) } catch (_: Exception) { emptyList() },
    onboardingCompleted = onboardingCompleted, onboardingStep = onboardingStep
)

private fun UserProfile.toEntity() = UserProfileEntity(
    id = id, email = email, fullName = fullName, gender = gender,
    dateOfBirth = dateOfBirth, heightCm = heightCm, weightKg = weightKg,
    fitnessGoal = fitnessGoal, activityLevel = activityLevel,
    dietaryRestrictions = Json.encodeToString(dietaryRestrictions),
    medicalConditions = Json.encodeToString(medicalConditions),
    onboardingCompleted = onboardingCompleted, onboardingStep = onboardingStep,
    updatedAt = System.currentTimeMillis()
)

private fun ProfileRow.toEntity() = UserProfileEntity(
    id = id, email = email, fullName = full_name, gender = gender,
    dateOfBirth = date_of_birth, heightCm = height_cm, weightKg = weight_kg,
    fitnessGoal = fitness_goal, activityLevel = activity_level,
    dietaryRestrictions = Json.encodeToString(dietary_restrictions),
    medicalConditions = Json.encodeToString(medical_conditions),
    onboardingCompleted = onboarding_completed, onboardingStep = onboarding_step,
    createdAt = created_at, updatedAt = updated_at
)

private fun UserProfile.toRow() = ProfileRow(
    id = id, email = email, full_name = fullName, gender = gender,
    date_of_birth = dateOfBirth, height_cm = heightCm, weight_kg = weightKg,
    fitness_goal = fitnessGoal, activity_level = activityLevel,
    dietary_restrictions = dietaryRestrictions, medical_conditions = medicalConditions,
    onboarding_completed = onboardingCompleted, onboarding_step = onboardingStep,
    updated_at = System.currentTimeMillis()
)
