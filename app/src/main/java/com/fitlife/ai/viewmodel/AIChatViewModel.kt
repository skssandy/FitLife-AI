package com.fitlife.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.BuildConfig
import com.fitlife.ai.data.local.entity.ChatMessageEntity
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.CycleRepository
import com.fitlife.ai.data.local.dao.ChatMessageDao
import com.fitlife.ai.util.CycleCalculator
import com.fitlife.ai.util.CyclePhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val authRepository: AuthRepository,
    private val cycleRepository: CycleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "AIChatViewModel"
        private const val GEMINI_MODEL = "gemini-3.5-flash"
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
            val userId = try {
                authRepository.getCurrentUserId()
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(error = "Please sign in to use AI Coach")
                return@launch
            }
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "Gemini API key not configured")
                return@launch
            }

            val userMessage = ChatMessageEntity(userId = userId, role = "user", content = text)
            chatMessageDao.insert(userMessage)

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val profile = authRepository.getUserOnce(userId)
                val cycleContext = buildCycleContext(userId)
                val instruction = buildSystemInstruction(profile, cycleContext)
                val response = withContext(Dispatchers.IO) { callGeminiApi(apiKey, text, instruction) }
                val aiMessage = ChatMessageEntity(userId = userId, role = "assistant", content = response)
                chatMessageDao.insert(aiMessage)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun buildCycleContext(userId: String): String {
        val sb = StringBuilder()
        try {
            val entries = cycleRepository.getEntriesOnce(userId)
            if (entries.isNotEmpty()) {
                sb.append(" Period history (recent first): ")
                entries.take(6).forEach { e ->
                    sb.append("start ${dateLabel(e.startDate)} (${e.durationDays}d)")
                    if (e.symptomsJson.isNotBlank() && e.symptomsJson != "[]") sb.append(", symptoms ${e.symptomsJson}")
                    if (e.notes.isNotBlank()) sb.append(", note \"${e.notes}\"")
                    sb.append(" | ")
                }
            }
            val days = cycleRepository.getCycleDaysOnce(userId)
            if (days.isNotEmpty()) {
                sb.append(" Daily journal (recent first): ")
                days.take(7).forEach { d ->
                    val parts = mutableListOf<String>()
                    d.moodId?.let { parts.add("mood ${it}") }
                    d.weightKg?.let { parts.add("weight ${it}kg") }
                    if (d.note.isNotBlank()) parts.add("note \"${d.note}\"")
                    if (parts.isNotEmpty()) sb.append("${dateLabel(d.date)} [${parts.joinToString(", ")}] | ")
                }
            }
            val symptoms = cycleRepository.getSymptomsOnce(userId)
            if (symptoms.isNotEmpty()) {
                sb.append(" Recent symptoms: ")
                symptoms.take(7).forEach { s ->
                    if (s.symptomsJson.isNotBlank() && s.symptomsJson != "[]") {
                        sb.append("${dateLabel(s.date)} (${s.symptomsJson}) | ")
                    }
                }
            }
        } catch (_: Exception) { }
        return sb.toString().trim()
    }

    private fun buildSystemInstruction(profile: UserEntity?, cycleContext: String): String {
        val base = "You are a professional fitness trainer and nutritionist. " +
            "Provide helpful, accurate advice about exercise, nutrition, and healthy living. " +
            "Keep responses concise and actionable."
        val context = StringBuilder()
        if (profile != null) {
            context.append("User profile: ")
            context.append("gender=${profile.gender ?: "unknown"}")
            if (profile.displayName != null) context.append(", name=${profile.displayName}")
            if (profile.heightCm != null) context.append(", height=${profile.heightCm}cm")
            if (profile.weightKg != null) context.append(", weight=${profile.weightKg}kg")
            if (profile.fitnessGoal != null) context.append(", goal=${profile.fitnessGoal}")
            if (profile.activityLevel != null) context.append(", activity=${profile.activityLevel}")
            if (profile.injuries != null) context.append(", injuries=${profile.injuries}")
            if (profile.workoutFrequency != null) context.append(", workout frequency=${profile.workoutFrequency}")
            context.append(". ")
            val lastPeriod = profile.lastPeriodStart
            val cycleLength = profile.cycleLength
            if (profile.gender.equals("Female", ignoreCase = true) && lastPeriod != null && lastPeriod > 0) {
                val len = (cycleLength ?: 28).coerceAtLeast(21)
                val day = CycleCalculator.cycleDay(System.currentTimeMillis(), lastPeriod, len)
                if (day > 0) {
                    val phase = CycleCalculator.phaseForDay(day)
                    context.append("The user is on day $day of their menstrual cycle (${phase.displayName} phase). ")
                    context.append("Phase guidance: training=${phase.training} nutrition=${phase.nutrition}. ")
                }
                if (!profile.birthControl.isNullOrBlank()) {
                    context.append("The user is using birth control (${profile.birthControl}), so do not predict ovulation or a fertile window. ")
                }
            }
        }
        if (cycleContext.isNotBlank()) context.append(cycleContext).append(" ")
        return base + " " + context.toString()
    }

    private fun dateLabel(millis: Long): String =
        java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(millis))

    private fun callGeminiApi(apiKey: String, prompt: String, instruction: String): String {
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
                        put("text", instruction)
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
