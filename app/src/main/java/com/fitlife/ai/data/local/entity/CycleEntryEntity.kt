package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "cycle_entries")
data class CycleEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val startDate: Long,
    val flowLevel: String = "",
    val symptomsJson: String = "[]",
    val notes: String = "",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
