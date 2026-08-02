package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitlife.ai.data.local.entity.BloodReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodReportDao {
    @Query("SELECT * FROM blood_reports WHERE userId = :userId ORDER BY reportDate DESC")
    fun getReports(userId: String): Flow<List<BloodReportEntity>>

    @Query("SELECT * FROM blood_reports WHERE id = :id")
    suspend fun getById(id: Long): BloodReportEntity?

    @Insert
    suspend fun insert(report: BloodReportEntity): Long

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reports: List<BloodReportEntity>)

    @Update
    suspend fun update(report: BloodReportEntity)

    @Query("DELETE FROM blood_reports WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE blood_reports SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM blood_reports WHERE synced = 0")
    suspend fun getUnsynced(): List<BloodReportEntity>
}
