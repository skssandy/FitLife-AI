package com.fitlife.ai.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitlife.ai.data.health.HealthConnectManager
import com.fitlife.ai.data.local.entity.DailyMetricEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.DailyMetricRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val healthConnectManager: HealthConnectManager,
    private val dailyMetricRepository: DailyMetricRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!healthConnectManager.isAvailable()) return Result.success()
        return try {
            val userId = authRepository.getCurrentUserId()
            val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val dayEnd = dayStart.plus(1, ChronoUnit.DAYS)
            val metrics = healthConnectManager.readDailyMetrics(dayStart, dayEnd)
            if (metrics.steps == null && metrics.sleepMinutes == null && metrics.caloriesBurned == null) {
                return Result.success()
            }
            val existing = dailyMetricRepository.getForDay(userId, dayStart.toEpochMilli())
            dailyMetricRepository.upsert(
                DailyMetricEntity(
                    userId = userId,
                    date = dayStart.toEpochMilli(),
                    steps = metrics.steps ?: existing?.steps,
                    heartRateAvg = metrics.heartRateAvg ?: existing?.heartRateAvg,
                    hrvAvg = metrics.hrvAvg ?: existing?.hrvAvg,
                    sleepMinutes = metrics.sleepMinutes ?: existing?.sleepMinutes,
                    sleepStagesJson = if (metrics.sleepStages.isNotEmpty())
                        kotlinx.serialization.json.Json.encodeToString(
                            MapSerializer(String.serializer(), Int.serializer()),
                            metrics.sleepStages
                        )
                    else existing?.sleepStagesJson,
                    caloriesBurned = metrics.caloriesBurned ?: existing?.caloriesBurned,
                    activeMinutes = metrics.activeMinutes ?: existing?.activeMinutes,
                    weightKg = metrics.weightKg ?: existing?.weightKg,
                    bodyFatPct = metrics.bodyFatPct ?: existing?.bodyFatPct,
                    source = "health"
                )
            )
            dailyMetricRepository.syncUnsynced()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
