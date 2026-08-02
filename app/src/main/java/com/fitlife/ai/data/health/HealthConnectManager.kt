package com.fitlife.ai.data.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.contracts.HealthPermissionsRequestContract
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class HealthDailyMetrics(
    val steps: Int? = null,
    val heartRateAvg: Int? = null,
    val hrvAvg: Int? = null,
    val sleepMinutes: Int? = null,
    val sleepStages: Map<String, Int> = emptyMap(),
    val caloriesBurned: Int? = null,
    val activeMinutes: Int? = null,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null
)

data class DetectedExerciseSession(
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int,
    val calories: Int?
)

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class)
    )

    @Volatile
    private var cachedClient: HealthConnectClient? = null

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun permissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> =
        HealthPermissionsRequestContract()

    private fun client(): HealthConnectClient? {
        cachedClient?.let { return it }
        return runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull().also { cachedClient = it }
    }

    suspend fun hasPermissions(): Boolean {
        val c = client() ?: return false
        return runCatching {
            val granted = c.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        }.getOrDefault(false)
    }

    suspend fun readDailyMetrics(start: Instant, end: Instant): HealthDailyMetrics {
        val c = client() ?: return HealthDailyMetrics()
        if (!hasPermissions()) return HealthDailyMetrics()
        return runCatching {
            val safeEnd = end.minus(1, ChronoUnit.SECONDS)
            val filter = TimeRangeFilter.between(start, safeEnd)

            val aggregateResponse = c.aggregate(
                AggregateRequest(
                    metrics = setOf<AggregateMetric<*>>(
                        StepsRecord.COUNT_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                    ),
                    timeRangeFilter = filter
                )
            )
            val steps = aggregateResponse[StepsRecord.COUNT_TOTAL]?.toInt()
            val totalCalories = aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt()
            val activeCalories = aggregateResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt()

            val restingHr = c.readRecords(
                ReadRecordsRequest(
                    recordType = RestingHeartRateRecord::class,
                    timeRangeFilter = filter
                )
            ).records
            val hrv = c.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = filter
                )
            ).records

            val sleep = c.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start.minus(1, ChronoUnit.DAYS), safeEnd)
                )
            ).records
            val sleepResult = computeSleep(sleep, start, safeEnd)

            val weight = c.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start.minus(90, ChronoUnit.DAYS), safeEnd)
                )
            ).records.maxByOrNull { it.time }?.weight?.inKilograms

            val bodyFat = c.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start.minus(90, ChronoUnit.DAYS), safeEnd)
                )
            ).records.maxByOrNull { it.time }?.percentage?.value?.times(100)

            HealthDailyMetrics(
                steps = steps,
                heartRateAvg = restingHr.map { it.beatsPerMinute.toInt() }.takeIf { it.isNotEmpty() }?.average()?.toInt(),
                hrvAvg = hrv.map { it.heartRateVariabilityMillis.toInt() }.takeIf { it.isNotEmpty() }?.average()?.toInt(),
                sleepMinutes = sleepResult.first,
                sleepStages = sleepResult.second,
                caloriesBurned = totalCalories,
                activeMinutes = activeCalories?.let { (it / 8).coerceAtLeast(0) },
                weightKg = weight,
                bodyFatPct = bodyFat
            )
        }.getOrElse { HealthDailyMetrics() }
    }

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<DetectedExerciseSession> {
        val c = client() ?: return emptyList()
        if (!hasPermissions()) return emptyList()
        return runCatching {
            val safeEnd = end.minus(1, ChronoUnit.SECONDS)
            val sessions = c.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, safeEnd)
                )
            ).records
            sessions.map { session ->
                val durationMinutes = Duration.between(session.startTime, session.endTime).toMinutes().toInt()
                val calories = runCatching {
                    c.aggregate(
                        AggregateRequest(
                            metrics = setOf<AggregateMetric<*>>(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                        )
                    )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt()
                }.getOrNull()
                DetectedExerciseSession(
                    title = (session.title ?: "").ifBlank { "Health Connect Workout" },
                    startTime = session.startTime,
                    endTime = session.endTime,
                    durationMinutes = durationMinutes,
                    calories = calories
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun computeSleep(
        sessions: List<SleepSessionRecord>,
        dayStart: Instant,
        dayEnd: Instant
    ): Pair<Int?, Map<String, Int>> {
        var total = 0
        val stageMinutes = linkedMapOf(
            "awake" to 0, "light" to 0, "deep" to 0, "rem" to 0, "sleeping" to 0, "unknown" to 0
        )
        sessions.forEach { session ->
            session.stages.forEach { stage ->
                val start = if (stage.startTime.isBefore(dayStart)) dayStart else stage.startTime
                val end = if (stage.endTime.isAfter(dayEnd)) dayEnd else stage.endTime
                if (end.isAfter(start)) {
                    val minutes = Duration.between(start, end).toMinutes().toInt()
                    val label = when (stage.stage) {
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> "light"
                        SleepSessionRecord.STAGE_TYPE_DEEP -> "deep"
                        SleepSessionRecord.STAGE_TYPE_REM -> "rem"
                        SleepSessionRecord.STAGE_TYPE_AWAKE,
                        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> "awake"
                        SleepSessionRecord.STAGE_TYPE_SLEEPING -> "sleeping"
                        else -> "unknown"
                    }
                    stageMinutes[label] = (stageMinutes[label] ?: 0) + minutes
                    if (label == "light" || label == "deep" || label == "rem" || label == "sleeping") {
                        total += minutes
                    }
                }
            }
        }
        return if (sessions.isEmpty()) null to emptyMap() else total to stageMinutes
    }

    suspend fun hasAnyData(): Boolean {
        val c = client() ?: return false
        return runCatching {
            val end = Instant.now()
            val start = end.minus(7, ChronoUnit.DAYS)
            val response = c.aggregate(
                AggregateRequest(
                    metrics = setOf<AggregateMetric<*>>(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            (response[StepsRecord.COUNT_TOTAL] ?: 0) > 0
        }.getOrDefault(false)
    }
}
