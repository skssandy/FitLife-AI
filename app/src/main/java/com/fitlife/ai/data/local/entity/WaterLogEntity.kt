package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val amountMl: Int,
    val date: Long,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
