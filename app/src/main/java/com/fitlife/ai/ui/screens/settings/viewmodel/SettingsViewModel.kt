package com.fitlife.ai.ui.screens.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
    val biometrics: Boolean = false,
    val autoSync: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(darkMode = enabled)
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notifications = enabled)
    }

    fun toggleBiometrics(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(biometrics = enabled)
    }

    fun toggleAutoSync(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSync = enabled)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
