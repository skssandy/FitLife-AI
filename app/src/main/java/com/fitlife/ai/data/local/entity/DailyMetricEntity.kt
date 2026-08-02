package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "daily_metrics",
    indices = [Index(value = ["userId", "date"], unique = true)]
)
data class DailyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: Long,
    val steps: Int? = null,
    val heartRateAvg: Int? = null,
    val hrvAvg: Int? = null,
    val sleepMinutes: Int? = null,
    val sleepStagesJson: String? = null,
    val caloriesBurned: Int? = null,
    val activeMinutes: Int? = null,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val source: String = "health",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
