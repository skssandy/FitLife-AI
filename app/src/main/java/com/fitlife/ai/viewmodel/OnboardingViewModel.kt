package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isProfileComplete: Boolean = false,
    val prefill: UserEntity? = null,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val user = authRepository.observeUser(userId).first()
                _uiState.value = OnboardingUiState(
                    isLoading = false,
                    isProfileComplete = user?.displayName?.isNotBlank() == true,
                    prefill = user
                )
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState(isLoading = false)
            }
        }
    }

    fun saveProfile(
        displayName: String,
        heightCm: Double?,
        weightKg: Double?,
        dateOfBirth: String?,
        gender: String?,
        fitnessGoal: String?,
        activityLevel: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val userId = authRepository.getCurrentUserId()
                val existing = _uiState.value.prefill
                authRepository.saveProfile(
                    UserEntity(
                        id = userId,
                        email = existing?.email ?: authRepository.currentUser?.email ?: "",
                        displayName = displayName.ifBlank { null },
                        photoUrl = existing?.photoUrl,
                        heightCm = heightCm,
                        weightKg = weightKg,
                        dateOfBirth = dateOfBirth,
                        gender = gender,
                        fitnessGoal = fitnessGoal,
                        activityLevel = activityLevel
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}
