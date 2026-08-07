package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fitlife.ai.data.local.entity.CycleDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDayDao {
    @Query("SELECT * FROM cycle_days WHERE userId = :userId ORDER BY date DESC")
    fun getDays(userId: String): Flow<List<CycleDayEntity>>

    @Query("SELECT * FROM cycle_days WHERE userId = :userId ORDER BY date DESC")
    suspend fun getDaysOnce(userId: String): List<CycleDayEntity>

    @Query("SELECT * FROM cycle_days WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getForDay(userId: String, date: Long): CycleDayEntity?

    @Query("SELECT * FROM cycle_days WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CycleDayEntity?

    @Upsert
    suspend fun upsert(day: CycleDayEntity): Long

    @Upsert
    suspend fun upsertAll(days: List<CycleDayEntity>)

    @Query("UPDATE cycle_days SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM cycle_days WHERE synced = 0")
    suspend fun getUnsynced(): List<CycleDayEntity>
}
