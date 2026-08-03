package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fitlife.ai.data.local.entity.CycleEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleEntryDao {
    @Query("SELECT * FROM cycle_entries WHERE userId = :userId ORDER BY startDate DESC")
    fun getEntries(userId: String): Flow<List<CycleEntryEntity>>

    @Query("SELECT * FROM cycle_entries WHERE userId = :userId ORDER BY startDate DESC")
    suspend fun getEntriesOnce(userId: String): List<CycleEntryEntity>

    @Query("SELECT * FROM cycle_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CycleEntryEntity?

    @Upsert
    suspend fun upsert(entry: CycleEntryEntity): Long

    @Upsert
    suspend fun upsertAll(entries: List<CycleEntryEntity>)

    @Query("UPDATE cycle_entries SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM cycle_entries WHERE synced = 0")
    suspend fun getUnsynced(): List<CycleEntryEntity>
}
