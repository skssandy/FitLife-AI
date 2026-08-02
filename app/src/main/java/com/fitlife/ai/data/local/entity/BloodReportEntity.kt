package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "blood_reports")
data class BloodReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val reportDate: Long,
    val source: String,
    val rawText: String? = null,
    val markersJson: String? = null,
    val analysisText: String? = null,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
