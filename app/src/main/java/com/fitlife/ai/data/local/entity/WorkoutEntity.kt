package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double?,
    val durationMinutes: Int?,
    val caloriesBurned: Int?,
    val notes: String?,
    val date: Long,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
