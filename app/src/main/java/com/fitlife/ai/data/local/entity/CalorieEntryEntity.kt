package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "calorie_entries")
data class CalorieEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val foodName: String,
    val calories: Int,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val mealType: String? = null,
    val date: Long,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
