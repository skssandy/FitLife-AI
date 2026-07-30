package com.fitlife.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.BuildConfig
import com.fitlife.ai.data.local.entity.ChatMessageEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.local.dao.ChatMessageDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class ChatMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AIChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "AIChatViewModel"
        private const val GEMINI_MODEL = "gemini-2.0-flash-exp"
    }

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                chatMessageDao.getMessages(userId).collect { entities ->
                    _uiState.value = _uiState.value.copy(
                        messages = entities.map { ChatMessage(it.id, it.role, it.content, it.timestamp) }
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val userId = try { authRepository.getCurrentUserId() } catch (_: Exception) { return@launch }
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "Gemini API key not configured")
                return@launch
            }

            val userMessage = ChatMessageEntity(userId = userId, role = "user", content = text)
            chatMessageDao.insert(userMessage)

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = callGeminiApi(apiKey, text)
                val aiMessage = ChatMessageEntity(userId = userId, role = "assistant", content = response)
                chatMessageDao.insert(aiMessage)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun callGeminiApi(apiKey: String, prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are a professional fitness trainer and nutritionist. Provide helpful, accurate advice about exercise, nutrition, and healthy living. Keep responses concise and actionable.")
                    })
                })
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val responseCode = conn.responseCode
        val reader = if (responseCode in 200..299) {
            BufferedReader(InputStreamReader(conn.inputStream))
        } else {
            BufferedReader(InputStreamReader(conn.errorStream))
        }

        val response = reader.readText()
        reader.close()

        return parseGeminiResponse(response)
    }

    private fun parseGeminiResponse(json: String): String {
        val obj = JSONObject(json)
        val candidates = obj.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).optJSONObject("content")
            if (content != null) {
                val parts = content.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "No response")
                }
            }
        }
        val error = obj.optJSONObject("error")
        if (error != null) {
            throw Exception(error.optString("message", "Unknown API error"))
        }
        return "Sorry, I couldn't process that request."
    }
}
