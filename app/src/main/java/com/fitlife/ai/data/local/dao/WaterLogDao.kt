package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitlife.ai.data.local.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE userId = :userId ORDER BY date DESC")
    fun getLogs(userId: String): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE userId = :userId AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getLogsInRange(userId: String, start: Long, end: Long): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WaterLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<WaterLogEntity>)

    @Query("UPDATE water_logs SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM water_logs WHERE synced = 0")
    suspend fun getUnsynced(): List<WaterLogEntity>

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun delete(id: Long)
}
