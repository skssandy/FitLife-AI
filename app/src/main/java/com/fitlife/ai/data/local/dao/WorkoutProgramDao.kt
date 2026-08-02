package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitlife.ai.data.local.entity.WorkoutProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutProgramDao {
    @Query("SELECT * FROM workout_programs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPrograms(userId: String): Flow<List<WorkoutProgramEntity>>

    @Query("SELECT * FROM workout_programs WHERE id = :id")
    suspend fun getById(id: Long): WorkoutProgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(program: WorkoutProgramEntity): Long

    @Query("DELETE FROM workout_programs WHERE id = :id")
    suspend fun delete(id: Long)
}
