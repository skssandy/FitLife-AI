package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.util.CycleCalculator
import com.fitlife.ai.util.CyclePhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CycleUiState(
    val user: UserEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CycleViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CycleUiState())
    val uiState: StateFlow<CycleUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                authRepository.observeUser(userId).collect { user ->
                    _uiState.value = _uiState.value.copy(user = user, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun logPeriodToday() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val user = _uiState.value.user
                val updated = (user ?: UserEntity(
                    id = userId,
                    email = "",
                    displayName = null,
                    photoUrl = null,
                    heightCm = null,
                    weightKg = null,
                    dateOfBirth = null,
                    gender = null,
                    fitnessGoal = null,
                    activityLevel = null,
                    workoutFrequency = null,
                    equipment = null,
                    injuries = null,
                    lifestyle = null,
                    sleepHours = null,
                    stressLevel = null
                )).copy(
                    lastPeriodStart = System.currentTimeMillis()
                )
                authRepository.saveProfile(updated)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setCycleLength(days: Int) {
        viewModelScope.launch {
            try {
                val user = _uiState.value.user ?: return@launch
                authRepository.saveProfile(user.copy(cycleLength = days))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun phaseInfo(): Triple<CyclePhase, Int, Long?>? {
        val user = _uiState.value.user ?: return null
        val lastPeriod = user.lastPeriodStart ?: return null
        val cycleLength = (user.cycleLength ?: 28).coerceAtLeast(21)
        val today = System.currentTimeMillis()
        val day = CycleCalculator.cycleDay(today, lastPeriod, cycleLength)
        val phase = CycleCalculator.phaseForDay(day)
        val nextPeriod = CycleCalculator.nextPeriodStartMillis(lastPeriod, cycleLength, today)
        return Triple(phase, day, nextPeriod)
    }
}
