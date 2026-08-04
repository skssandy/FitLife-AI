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

data class InitialTargets(
    val bmi: Double?,
    val tdee: Int?,
    val calorieTarget: Int?,
    val proteinTarget: Int?
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
        workoutFrequency: String? = null,
        equipment: String? = null,
        injuries: String? = null,
        lifestyle: String? = null,
        sleepHours: Double? = null,
        stressLevel: String? = null,
        cycleLength: Int? = null,
        lastPeriodStart: Long? = null,
        dietType: String? = null,
        mealCount: Int? = null,
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
                        activityLevel = activityLevel,
                        workoutFrequency = workoutFrequency,
                        equipment = equipment,
                        injuries = injuries,
                        lifestyle = lifestyle,
                        sleepHours = sleepHours,
                        stressLevel = stressLevel,
                        cycleLength = cycleLength,
                        lastPeriodStart = lastPeriodStart,
                        dietType = dietType,
                        mealCount = mealCount
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun computeTargets(
        heightCm: Double?,
        weightKg: Double?,
        dateOfBirth: String?,
        gender: String?,
        activityLevel: String?,
        fitnessGoal: String?
    ): InitialTargets {
        val h = heightCm ?: return InitialTargets(null, null, null, null)
        val w = weightKg ?: return InitialTargets(null, null, null, null)
        val bmi = w / ((h / 100.0) * (h / 100.0))

        val age = dateOfBirth?.let {
            val parts = it.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull() ?: return@let null
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            currentYear - year
        }

        val genderConstant = if (gender.equals("Female", ignoreCase = true)) -161 else 5
        val bmr = ((10 * w) + (6.25 * h) - (5 * (age ?: 30)) + genderConstant).toInt()
        val activityMultiplier = when (activityLevel) {
            "Sedentary" -> 1.2
            "Light" -> 1.375
            "Moderate" -> 1.55
            "Active" -> 1.725
            "Very Active" -> 1.9
            else -> 1.375
        }
        val tdee = (bmr * activityMultiplier).toInt()

        val calorieTarget = when (fitnessGoal) {
            "Weight Loss" -> tdee - 400
            "Muscle Gain" -> tdee + 300
            else -> tdee
        }
        val proteinTarget = (w * 1.8).toInt()
        return InitialTargets(bmi = bmi, tdee = tdee, calorieTarget = calorieTarget, proteinTarget = proteinTarget)
    }
}
