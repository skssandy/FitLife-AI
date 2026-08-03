package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "symptom_logs",
    indices = [Index(value = ["userId", "date"], unique = true)]
)
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: Long,
    val symptomsJson: String = "[]",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
