package com.fitlife.ai.data.local.dao

import androidx.room.*
import com.fitlife.ai.data.local.entity.WorkoutProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutProgramDao {
    @Query("SELECT * FROM workout_programs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPrograms(userId: String): Flow<List<WorkoutProgramEntity>>

    @Query("SELECT * FROM workout_programs WHERE id = :id")
    fun getProgram(id: String): Flow<WorkoutProgramEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(program: WorkoutProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(programs: List<WorkoutProgramEntity>)

    @Delete
    suspend fun delete(program: WorkoutProgramEntity)

    @Query("DELETE FROM workout_programs WHERE id = :id")
    suspend fun deleteById(id: String)
}
