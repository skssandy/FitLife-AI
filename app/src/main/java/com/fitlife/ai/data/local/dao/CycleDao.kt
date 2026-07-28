package com.fitlife.ai.data.local.dao

import androidx.room.*
import com.fitlife.ai.data.local.entity.CycleEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycle_entries WHERE userId = :userId ORDER BY date DESC")
    fun getEntries(userId: String): Flow<List<CycleEntryEntity>>

    @Query("SELECT * FROM cycle_entries WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    fun getLatestEntry(userId: String): Flow<CycleEntryEntity?>

    @Query("SELECT * FROM cycle_entries WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getEntriesInRange(userId: String, startDate: String, endDate: String): Flow<List<CycleEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CycleEntryEntity)

    @Delete
    suspend fun delete(entry: CycleEntryEntity)
}
