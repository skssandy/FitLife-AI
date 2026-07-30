package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val updatedAt: Long = System.currentTimeMillis()
)
