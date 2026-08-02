package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fitlife.ai.data.local.entity.DailyMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyMetricDao {
    @Query("SELECT * FROM daily_metrics WHERE userId = :userId ORDER BY date DESC")
    fun getMetrics(userId: String): Flow<List<DailyMetricEntity>>

    @Query("SELECT * FROM daily_metrics WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getForDay(userId: String, date: Long): DailyMetricEntity?

    @Upsert
    suspend fun upsert(metric: DailyMetricEntity)

    @Upsert
    suspend fun upsertAll(metrics: List<DailyMetricEntity>)

    @Query("UPDATE daily_metrics SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM daily_metrics WHERE synced = 0")
    suspend fun getUnsynced(): List<DailyMetricEntity>
}
