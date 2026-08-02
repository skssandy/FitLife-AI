package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitlife.ai.data.local.entity.CalorieEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieEntryDao {
    @Query("SELECT * FROM calorie_entries WHERE userId = :userId ORDER BY date DESC")
    fun getEntries(userId: String): Flow<List<CalorieEntryEntity>>

    @Query("SELECT * FROM calorie_entries WHERE userId = :userId AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getEntriesInRange(userId: String, start: Long, end: Long): Flow<List<CalorieEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CalorieEntryEntity): Long

    @Query("UPDATE calorie_entries SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM calorie_entries WHERE synced = 0")
    suspend fun getUnsyncedEntries(): List<CalorieEntryEntity>

    @Query("DELETE FROM calorie_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
