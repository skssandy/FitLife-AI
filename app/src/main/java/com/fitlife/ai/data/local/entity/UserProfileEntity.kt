package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val email: String = "",
    val fullName: String = "",
    val gender: String = "male",
    val dateOfBirth: Long = 0L,
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val fitnessGoal: String = "general_fitness",
    val activityLevel: String = "moderate",
    val dietaryRestrictions: String = "[]",
    val medicalConditions: String = "[]",
    val onboardingCompleted: Boolean = false,
    val onboardingStep: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
