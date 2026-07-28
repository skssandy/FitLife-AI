package com.fitlife.ai.data.local.dao

import androidx.room.*
import com.fitlife.ai.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_queue WHERE synced = 0 ORDER BY createdAt")
    fun getPendingItems(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE synced = 0")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM sync_queue WHERE synced = 1")
    suspend fun deleteSynced()

    @Query("SELECT * FROM sync_queue WHERE synced = 0")
    suspend fun getAllPending(): List<SyncQueueEntity>
}
