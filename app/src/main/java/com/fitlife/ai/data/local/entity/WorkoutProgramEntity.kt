package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "workout_programs")
data class WorkoutProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val description: String,
    val goal: String,
    val daysJson: String,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
