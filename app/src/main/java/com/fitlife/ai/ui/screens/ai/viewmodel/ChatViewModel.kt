package com.fitlife.ai.ui.screens.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.remote.api.GeminiService
import com.fitlife.ai.data.repository.ProfileRepository
import com.fitlife.ai.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val suggestions: List<String> = listOf(
        "Create a workout plan for me",
        "What should I eat today?",
        "How can I improve my sleep?",
        "Analyze my fitness progress",
        "Tips for muscle recovery"
    )
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiService: GeminiService,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var userProfile: UserProfile? = null

    init {
        viewModelScope.launch {
            profileRepository.getAnyProfile().collect { profile ->
                userProfile = profile
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + ChatMessage(text, isUser = true),
                isLoading = true,
                error = null
            )

            val chatHistory = _uiState.value.messages.map { msg ->
                if (msg.isUser) "user" to msg.text else "model" to msg.text
            }

            geminiService.chat(userProfile, chatHistory)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + ChatMessage(response, isUser = false),
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to get response: ${e.message}"
                    )
                }
        }
    }

    fun useSuggestion(suggestion: String) {
        sendMessage(suggestion)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
