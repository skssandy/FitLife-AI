package com.fitlife.ai.data.local.dao

import androidx.room.*
import com.fitlife.ai.data.local.entity.BloodAnalysisEntity
import com.fitlife.ai.data.local.entity.BloodMarkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodAnalysisDao {
    @Query("SELECT * FROM blood_analyses WHERE userId = :userId ORDER BY analysisDate DESC")
    fun getAnalyses(userId: String): Flow<List<BloodAnalysisEntity>>

    @Query("SELECT * FROM blood_analyses WHERE id = :id")
    fun getAnalysis(id: String): Flow<BloodAnalysisEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(analysis: BloodAnalysisEntity)

    @Delete
    suspend fun delete(analysis: BloodAnalysisEntity)

    @Query("SELECT * FROM blood_markers WHERE analysisId = :analysisId")
    fun getMarkers(analysisId: String): Flow<List<BloodMarkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMarkers(markers: List<BloodMarkerEntity>)

    @Query("DELETE FROM blood_markers WHERE analysisId = :analysisId")
    suspend fun deleteMarkers(analysisId: String)
}
