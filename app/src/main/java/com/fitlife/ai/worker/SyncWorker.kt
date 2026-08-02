package com.fitlife.ai.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.BloodReportRepository
import com.fitlife.ai.data.repository.CalorieRepository
import com.fitlife.ai.data.repository.WorkoutRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val workoutRepository: WorkoutRepository,
    private val calorieRepository: CalorieRepository,
    private val bloodReportRepository: BloodReportRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            workoutRepository.syncUnsynced()
            calorieRepository.syncUnsynced()
            bloodReportRepository.syncUnsynced()
            val userId = authRepository.getUserId()
            if (userId != null) {
                workoutRepository.pullFromServer(userId)
                calorieRepository.pullFromServer(userId)
                bloodReportRepository.pullFromServer(userId)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
