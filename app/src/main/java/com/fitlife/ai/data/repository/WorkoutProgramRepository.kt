package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.WorkoutProgramDao
import com.fitlife.ai.data.local.entity.WorkoutProgramEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutProgramRepository @Inject constructor(
    private val workoutProgramDao: WorkoutProgramDao
) {
    fun getPrograms(userId: String): Flow<List<WorkoutProgramEntity>> =
        workoutProgramDao.getPrograms(userId)

    suspend fun addProgram(program: WorkoutProgramEntity): Long =
        workoutProgramDao.insert(program)

    suspend fun deleteProgram(id: Long) =
        workoutProgramDao.delete(id)
}
