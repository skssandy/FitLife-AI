package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.entity.WorkoutEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.CalorieRepository
import com.fitlife.ai.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val userGender: String? = null,
    val todayWorkouts: List<WorkoutEntity> = emptyList(),
    val totalWorkouts: Int = 0,
    val totalCalories: Int = 0,
    val caloriesGoal: Int = 500,
    val weeklyData: List<Pair<String, Int>> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    private val calorieRepository: CalorieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userId = authRepository.getCurrentUserId()
                val user = authRepository.loadUserFromSupabase()
                    ?: authRepository.getUserOnce(userId)

                val todayStart = startOfDay(0)
                val todayEnd = todayStart + 86400000L
                val weekStart = startOfDay(6)

                combine(
                    workoutRepository.getWorkoutsInRange(userId, todayStart, todayEnd),
                    calorieRepository.getEntriesInRange(userId, weekStart, todayEnd)
                ) { workouts, entries ->
                    HomeUiState(
                        userName = user?.displayName ?: user?.email ?: "User",
                        userGender = user?.gender,
                        todayWorkouts = workouts,
                        totalWorkouts = workouts.size,
                        totalCalories = entries.filter { it.date in todayStart until todayEnd }
                            .sumOf { it.calories },
                        caloriesGoal = 500,
                        weeklyData = buildWeeklyData(entries, weekStart),
                        isLoading = false
                    )
                }.collect { _uiState.value = it }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun buildWeeklyData(entries: List<com.fitlife.ai.data.local.entity.CalorieEntryEntity>, weekStart: Long): List<Pair<String, Int>> {
        val dayFormat = SimpleDateFormat("E", Locale.getDefault())
        return (0..6).map { offset ->
            val dayStart = weekStart + offset * 86400000L
            val dayEnd = dayStart + 86400000L
            val label = dayFormat.format(Date(dayStart))
            val total = entries.filter { it.date in dayStart until dayEnd }.sumOf { it.calories }
            label to total
        }
    }

    private fun startOfDay(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
