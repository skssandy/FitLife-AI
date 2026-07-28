package com.fitlife.ai.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class HealthData(
    val steps: Long = 0,
    val heartRateSamples: List<Double> = emptyList(),
    val avgHeartRate: Double = 0.0,
    val sleepMinutes: Int = 0,
    val caloriesBurned: Double = 0.0,
    val distanceMeters: Double = 0.0
)

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(CaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
    )

    val permissionContract = object : ActivityResultContract<Set<String>, Set<String>>() {
        override fun createIntent(context: Context, input: Set<String>): Intent {
            return HealthConnectClient.createPermissionControllerIntent(context, input)
        }
        override fun parseResult(resultCode: Int, intent: Intent?): Set<String> {
            return HealthConnectClient.parsePermissionsResult(resultCode, intent)
        }
    }

    suspend fun hasPermissions(): Boolean {
        return try {
            healthConnectClient.permissionController.getGrantedPermissions().containsAll(requiredPermissions)
        } catch (_: Exception) { false }
    }

    suspend fun getTodayData(): HealthData {
        if (!isAvailable) return HealthData()

        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.atTime(LocalTime.MAX).toInstant()
        val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

        return try {
            val steps = getSteps(timeRange)
            val heartRate = getHeartRate(timeRange)
            val sleep = getSleep(today)
            val calories = getCalories(timeRange)
            val distance = getDistance(timeRange)

            HealthData(
                steps = steps,
                heartRateSamples = heartRate,
                avgHeartRate = if (heartRate.isNotEmpty()) heartRate.average() else 0.0,
                sleepMinutes = sleep,
                caloriesBurned = calories,
                distanceMeters = distance
            )
        } catch (_: Exception) {
            HealthData()
        }
    }

    private suspend fun getSteps(timeRange: TimeRangeFilter): Long {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(StepsRecord::class, timeRange)
            )
            response.records.sumOf { it.count }
        } catch (_: Exception) { 0L }
    }

    private suspend fun getHeartRate(timeRange: TimeRangeFilter): List<Double> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, timeRange)
            )
            response.records.flatMap { record ->
                record.samples.map { it.beatsPerMinute.toDouble() }
            }
        } catch (_: Exception) { emptyList() }
    }

    private suspend fun getSleep(date: LocalDate): Int {
        return try {
            val start = date.minusDays(1).atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant()
            val end = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(start, end)
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, timeRange)
            )
            response.records.sumOf { record ->
                Duration.between(record.startTime, record.endTime).toMinutes().toInt()
            }
        } catch (_: Exception) { 0 }
    }

    private suspend fun getCalories(timeRange: TimeRangeFilter): Double {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(CaloriesBurnedRecord::class, timeRange)
            )
            response.records.sumOf { it.energy.inKilocalories }
        } catch (_: Exception) { 0.0 }
    }

    private suspend fun getDistance(timeRange: TimeRangeFilter): Double {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(DistanceRecord::class, timeRange)
            )
            response.records.sumOf { it.distance.inMeters }
        } catch (_: Exception) { 0.0 }
    }

    suspend fun writeSteps(count: Long) {
        if (!isAvailable) return
        try {
            healthConnectClient.insertRecords(
                listOf(
                    StepsRecord(
                        count = count,
                        startTime = Instant.now().minusSeconds(60),
                        startZoneId = ZoneId.systemDefault(),
                        endTime = Instant.now(),
                        endZoneId = ZoneId.systemDefault()
                    )
                )
            )
        } catch (_: Exception) { }
    }
}
