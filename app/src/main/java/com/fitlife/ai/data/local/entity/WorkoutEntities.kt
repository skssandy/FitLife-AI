package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_programs")
data class WorkoutProgramEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val difficulty: String = "beginner",
    val category: String = "general",
    val durationWeeks: Int = 4,
    val goals: String = "[]",
    val isAiGenerated: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val programId: String = "",
    val dayIndex: Int = 0,
    val dayName: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val durationMinutes: Int = 0,
    val completed: Boolean = false,
    val skipped: Boolean = false,
    val notes: String = "",
    val rating: Int = 0,
    val feeling: String = "neutral"
)

@Entity(tableName = "exercise_logs")
data class ExerciseLogEntity(
    @PrimaryKey val id: String,
    val sessionId: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val sets: Int = 0,
    val reps: String = "",
    val weight: String = "",
    val duration: String = "",
    val distance: String = "",
    val restSeconds: Int = 60,
    val notes: String = "",
    val completed: Boolean = false,
    val orderIndex: Int = 0
)
