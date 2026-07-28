package com.fitlife.ai.ui.screens.workout.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.dao.WorkoutDao
import com.fitlife.ai.data.local.dao.WorkoutSessionDao
import com.fitlife.ai.data.local.entity.WorkoutProgramEntity
import com.fitlife.ai.data.local.entity.WorkoutSessionEntity
import com.fitlife.ai.data.remote.api.GeminiService
import com.fitlife.ai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WorkoutUiState(
    val programs: List<WorkoutProgramEntity> = emptyList(),
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val completedCount: Int = 0,
    val aiRecommendation: String? = null,
    val isLoadingAi: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val sessionDao: WorkoutSessionDao,
    private val authRepository: AuthRepository,
    private val geminiService: GeminiService,
    private val profileRepository: com.fitlife.ai.data.repository.ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val userId = authRepository.currentUserId ?: return

        viewModelScope.launch {
            combine(
                workoutDao.getPrograms(userId),
                sessionDao.getRecentCompleted(userId, 7),
                sessionDao.getCompletedCount(userId)
            ) { programs, sessions, count ->
                WorkoutUiState(
                    programs = programs,
                    recentSessions = sessions,
                    completedCount = count
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun getAiWorkoutRecommendation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAi = true)
            geminiService.getWorkoutRecommendation(
                profileRepository.getAnyProfile().first()
            ).onSuccess { recommendation ->
                _uiState.value = _uiState.value.copy(
                    aiRecommendation = recommendation,
                    isLoadingAi = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingAi = false)
            }
        }
    }

    fun startSession(programId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            sessionDao.upsert(
                WorkoutSessionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    programId = programId,
                    startedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun completeSession(sessionId: String, durationMinutes: Int) {
        viewModelScope.launch {
            sessionDao.upsert(
                WorkoutSessionEntity(
                    id = sessionId,
                    completed = true,
                    completedAt = System.currentTimeMillis(),
                    durationMinutes = durationMinutes
                )
            )
        }
    }
}
