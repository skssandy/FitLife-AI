package com.fitlife.ai.data.local.dao

import androidx.room.*
import com.fitlife.ai.data.local.entity.ProgressEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress_entries WHERE userId = :userId ORDER BY date DESC")
    fun getEntries(userId: String): Flow<List<ProgressEntryEntity>>

    @Query("SELECT * FROM progress_entries WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    fun getLatestEntry(userId: String): Flow<ProgressEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ProgressEntryEntity)

    @Delete
    suspend fun delete(entry: ProgressEntryEntity)
}
