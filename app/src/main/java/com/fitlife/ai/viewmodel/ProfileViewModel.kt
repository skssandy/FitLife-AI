package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserEntity? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val user = authRepository.loadUserFromSupabase()
            _uiState.value = _uiState.value.copy(user = user, isLoading = false)
        }
    }

    fun updateProfile(
        displayName: String,
        heightCm: Double?,
        weightKg: Double?,
        dateOfBirth: String?,
        gender: String?,
        fitnessGoal: String?,
        activityLevel: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val userId = authRepository.getCurrentUserId()
                val currentUser = _uiState.value.user
                val updatedUser = UserEntity(
                    id = userId,
                    email = currentUser?.email ?: "",
                    displayName = displayName.ifBlank { null },
                    photoUrl = currentUser?.photoUrl,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    fitnessGoal = fitnessGoal,
                    activityLevel = activityLevel,
                    workoutFrequency = currentUser?.workoutFrequency,
                    equipment = currentUser?.equipment,
                    injuries = currentUser?.injuries,
                    lifestyle = currentUser?.lifestyle,
                    sleepHours = currentUser?.sleepHours,
                    stressLevel = currentUser?.stressLevel,
                    cycleLength = currentUser?.cycleLength,
                    lastPeriodStart = currentUser?.lastPeriodStart
                )
                authRepository.saveProfile(updatedUser)
                _uiState.value = _uiState.value.copy(user = updatedUser, isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}
