package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fitlife.ai.data.local.entity.SymptomLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomLogDao {
    @Query("SELECT * FROM symptom_logs WHERE userId = :userId ORDER BY date DESC")
    fun getLogs(userId: String): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getForDay(userId: String, date: Long): SymptomLogEntity?

    @Upsert
    suspend fun upsert(log: SymptomLogEntity)

    @Upsert
    suspend fun upsertAll(logs: List<SymptomLogEntity>)

    @Query("UPDATE symptom_logs SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM symptom_logs WHERE synced = 0")
    suspend fun getUnsynced(): List<SymptomLogEntity>
}
