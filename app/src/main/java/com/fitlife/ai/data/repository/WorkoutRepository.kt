package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.WorkoutDao
import com.fitlife.ai.data.local.entity.WorkoutEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val supabase: SupabaseClient
) {
    fun getWorkouts(userId: String): Flow<List<WorkoutEntity>> = workoutDao.getWorkouts(userId)

    fun getWorkoutsInRange(userId: String, start: Long, end: Long): Flow<List<WorkoutEntity>> =
        workoutDao.getWorkoutsInRange(userId, start, end)

    suspend fun addWorkout(workout: WorkoutEntity) {
        val id = workoutDao.insert(workout)
        val toSync = workout.copy(id = id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("workouts").upsert(toSync)
                workoutDao.markSynced(id)
            }
        } catch (_: Exception) { }
    }
    suspend fun deleteWorkout(id: Long) {
        workoutDao.delete(id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("workouts").delete { filter { eq("id", id) } }
            }
        } catch (_: Exception) { }
    }

    suspend fun syncUnsynced() {
        val unsynced = workoutDao.getUnsyncedWorkouts()
        for (w in unsynced) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("workouts").upsert(w)
                    workoutDao.markSynced(w.id)
                }
            } catch (_: Exception) { }
        }
    }
}
