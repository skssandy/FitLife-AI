package com.fitlife.ai.ui.screens.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.ProfileRepository
import com.fitlife.ai.domain.model.UserProfile
import com.fitlife.ai.health.HealthConnectManager
import com.fitlife.ai.health.HealthData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userProfile: UserProfile? = null,
    val healthData: HealthData = HealthData(),
    val greeting: String = "Good Morning",
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "Good Morning"
                hour < 17 -> "Good Afternoon"
                else -> "Good Evening"
            }
            _uiState.value = _uiState.value.copy(greeting = greeting)

            profileRepository.getAnyProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            if (healthConnectManager.isAvailable && healthConnectManager.hasPermissions()) {
                val data = healthConnectManager.getTodayData()
                _uiState.value = _uiState.value.copy(healthData = data)
            }
        }
    }
}
