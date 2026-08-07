package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "cycle_days",
    indices = [Index(value = ["userId", "date"], unique = true)]
)
data class CycleDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: Long,
    val note: String = "",
    val moodId: String? = null,
    val weightKg: Double? = null,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
