package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.entity.WorkoutEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUiState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadWorkouts()
    }

    private fun loadWorkouts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userId = authRepository.getCurrentUserId()
                workoutRepository.getWorkouts(userId).collect { workouts ->
                    _uiState.value = _uiState.value.copy(workouts = workouts, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun addWorkout(
        exerciseName: String,
        sets: Int,
        reps: Int,
        weightKg: Double?,
        durationMinutes: Int?,
        caloriesBurned: Int?,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                workoutRepository.addWorkout(
                    WorkoutEntity(
                        userId = userId,
                        exerciseName = exerciseName,
                        sets = sets,
                        reps = reps,
                        weightKg = weightKg,
                        durationMinutes = durationMinutes,
                        caloriesBurned = caloriesBurned,
                        notes = notes,
                        date = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(id)
        }
    }
}
