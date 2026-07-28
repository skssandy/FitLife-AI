package com.fitlife.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_logs")
data class NutritionLogEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val date: String = "",
    val mealType: String = "",
    val foodName: String = "",
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val servingSize: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val date: String = "",
    val amountMl: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blood_analyses")
data class BloodAnalysisEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val fileName: String = "",
    val fileUrl: String = "",
    val analysisDate: Long = System.currentTimeMillis(),
    val overallSummary: String = "",
    val recommendations: String = "[]",
    val geminiAnalysis: String = "",
    val status: String = "pending"
)

@Entity(tableName = "blood_markers")
data class BloodMarkerEntity(
    @PrimaryKey val id: String,
    val analysisId: String = "",
    val name: String = "",
    val value: Double = 0.0,
    val unit: String = "",
    val referenceMin: Double = 0.0,
    val referenceMax: Double = 0.0,
    val status: String = "normal"
)

@Entity(tableName = "cycle_entries")
data class CycleEntryEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val date: String = "",
    val dayOfCycle: Int = 0,
    val phase: String = "",
    val flow: String = "",
    val symptoms: String = "[]",
    val mood: String = "",
    val temperature: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "progress_entries")
data class ProgressEntryEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val date: String = "",
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val muscleMassKg: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null,
    val photoUrl: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String = "",
    val entityId: String = "",
    val action: String = "",
    val payload: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val retries: Int = 0
)
