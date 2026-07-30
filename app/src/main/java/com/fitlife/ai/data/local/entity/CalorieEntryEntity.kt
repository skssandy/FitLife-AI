package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calorie_entries")
data class CalorieEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val foodName: String,
    val calories: Int,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val mealType: String?,
    val date: Long,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
