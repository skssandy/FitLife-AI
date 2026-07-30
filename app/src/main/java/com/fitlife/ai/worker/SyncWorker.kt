package com.fitlife.ai.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitlife.ai.data.repository.CalorieRepository
import com.fitlife.ai.data.repository.WorkoutRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val workoutRepository: WorkoutRepository,
    private val calorieRepository: CalorieRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            workoutRepository.syncUnsynced()
            calorieRepository.syncUnsynced()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
