package com.fitlife.ai.viewmodel

import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.health.ActivityScoreCalculator
import com.fitlife.ai.data.health.DetectedExerciseSession
import com.fitlife.ai.data.health.HealthConnectManager
import com.fitlife.ai.data.local.entity.DailyMetricEntity
import com.fitlife.ai.data.local.entity.WorkoutEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.DailyMetricRepository
import com.fitlife.ai.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import javax.inject.Inject

data class HealthUiState(
    val isLoading: Boolean = true,
    val isAvailable: Boolean = false,
    val permissionsGranted: Boolean = false,
    val hasAnyData: Boolean = false,
    val today: DailyMetricEntity? = null,
    val weeklyMetrics: List<DailyMetricEntity> = emptyList(),
    val detectedWorkouts: List<DetectedExerciseSession> = emptyList(),
    val activityScore: Int = 0,
    val lastSync: Long? = null,
    val error: String? = null
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val dailyMetricRepository: DailyMetricRepository,
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    val permissionContract: ActivityResultContract<Set<String>, Set<String>> =
        healthConnectManager.permissionRequestContract()
    val requiredPermissions: Set<String> = healthConnectManager.requiredPermissions

    fun refresh() {
        viewModelScope.launch {
            val available = healthConnectManager.isAvailable()
            val granted = healthConnectManager.hasPermissions()
            _uiState.value = _uiState.value.copy(isLoading = true, isAvailable = available, permissionsGranted = granted)
            try {
                val userId = authRepository.getCurrentUserId()

                if (available && granted) {
                    val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val dayEnd = dayStart.plus(1, ChronoUnit.DAYS)
                    val metrics = healthConnectManager.readDailyMetrics(dayStart, dayEnd)
                    val sessions = healthConnectManager.readExerciseSessions(dayStart, dayEnd)
                    val existing = dailyMetricRepository.getForDay(userId, dayStart.toEpochMilli())
                    if (metrics.steps != null || metrics.sleepMinutes != null || metrics.caloriesBurned != null) {
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
                    }
                    _uiState.value = _uiState.value.copy(
                        detectedWorkouts = sessions,
                        hasAnyData = sessions.isNotEmpty() || metrics.steps != null,
                        lastSync = System.currentTimeMillis()
                    )
                }

                val weekStart = startOfDayMillis(6)
                dailyMetricRepository.getMetrics(userId).collect { metrics ->
                    val today = metrics.firstOrNull { it.date == startOfDayMillis() }
                    val weekly = metrics.filter { it.date >= weekStart }.sortedByDescending { it.date }
                    _uiState.value = _uiState.value.copy(
                        today = today,
                        weeklyMetrics = weekly,
                        activityScore = ActivityScoreCalculator.score(today),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun addManualSteps(steps: Int) {
        updateToday { it.copy(steps = steps) }
    }

    fun addManualSleep(minutes: Int) {
        updateToday { it.copy(sleepMinutes = minutes) }
    }

    fun addManualWeight(kg: Double) {
        updateToday { it.copy(weightKg = kg) }
    }

    fun addManualBodyFat(pct: Double) {
        updateToday { it.copy(bodyFatPct = pct) }
    }

    private fun updateToday(transform: (DailyMetricEntity) -> DailyMetricEntity) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val dayStart = startOfDayMillis()
                val existing = dailyMetricRepository.getForDay(userId, dayStart)
                    ?: DailyMetricEntity(userId = userId, date = dayStart, source = "manual")
                dailyMetricRepository.upsert(transform(existing).copy(source = "manual"))
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun logDetectedWorkout(session: DetectedExerciseSession) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                workoutRepository.addWorkout(
                    WorkoutEntity(
                        userId = userId,
                        exerciseName = session.title,
                        sets = 1,
                        reps = 1,
                        weightKg = null,
                        durationMinutes = session.durationMinutes,
                        caloriesBurned = session.calories,
                        notes = "Detected via Health Connect",
                        date = session.startTime.toEpochMilli()
                    )
                )
                _uiState.value = _uiState.value.copy(
                    detectedWorkouts = _uiState.value.detectedWorkouts.filterNot { it == session },
                    lastSync = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun startOfDayMillis(daysAgo: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
