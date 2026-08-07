package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val heightCm: Double?,
    val weightKg: Double?,
    val dateOfBirth: String?,
    val gender: String?,
    val fitnessGoal: String?,
    val activityLevel: String?,
    val workoutFrequency: String?,
    val equipment: String?,
    val injuries: String?,
    val lifestyle: String?,
    val sleepHours: Double?,
    val stressLevel: String?,
    val cycleLength: Int? = null,
    val lastPeriodStart: Long? = null,
    val supportMode: String? = null,
    val birthControl: String? = null,
    val hydrationTargetMl: Int? = null,
    val dietType: String? = null,
    val mealCount: Int? = null,
    val calorieTarget: Int? = null,
    val proteinTargetG: Int? = null,
    val carbsTargetG: Int? = null,
    val fatTargetG: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
