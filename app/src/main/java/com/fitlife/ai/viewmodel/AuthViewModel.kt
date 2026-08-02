package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.BloodReportRepository
import com.fitlife.ai.data.repository.CalorieRepository
import com.fitlife.ai.data.repository.WaterRepository
import com.fitlife.ai.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: UserEntity? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository,
    private val calorieRepository: CalorieRepository,
    private val bloodReportRepository: BloodReportRepository,
    private val waterRepository: WaterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn = MutableStateFlow(false)
    val sessionChecked = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val loggedIn = authRepository.restoreSession()
            isLoggedIn.value = loggedIn
            _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
            sessionChecked.value = true
            if (loggedIn) {
                loadUserProfile()
                pullAll()
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.signUp(email, password)
                _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                isLoggedIn.value = true
                pullAll()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.signIn(email, password)
                _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                isLoggedIn.value = true
                pullAll()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState()
            isLoggedIn.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.sendPasswordReset(email)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Password reset link sent. Check your email.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to send reset link")
            }
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val user = authRepository.loadUserFromSupabase()
            _uiState.value = _uiState.value.copy(user = user)
        }
    }

    private fun pullAll() {
        viewModelScope.launch {
            val userId = authRepository.getUserId() ?: return@launch
            workoutRepository.pullFromServer(userId)
            calorieRepository.pullFromServer(userId)
            bloodReportRepository.pullFromServer(userId)
            waterRepository.pullFromServer(userId)
        }
    }

    fun saveProfile(user: UserEntity) {
        viewModelScope.launch {
            authRepository.saveProfile(user)
            _uiState.value = _uiState.value.copy(user = user)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
