package com.fitlife.ai.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.BuildConfig
import com.fitlife.ai.data.local.entity.BloodMarkerDto
import com.fitlife.ai.data.local.entity.BloodReportEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.BloodReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class BloodUiState(
    val reports: List<BloodReportEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isExtracting: Boolean = false,
    val isAnalyzing: Boolean = false,
    val draftMarkers: List<BloodMarkerDto> = emptyList(),
    val draftRawText: String? = null,
    val error: String? = null
)

@HiltViewModel
class BloodViewModel @Inject constructor(
    private val bloodReportRepository: BloodReportRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BloodUiState())
    val uiState: StateFlow<BloodUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "BloodViewModel"
        private const val GEMINI_MODEL = "gemini-3.5-flash"
        private const val BLOOD_ANALYZER_SYSTEM_PROMPT =
            "You are FitLife AI's medical data analyst. Interpret blood test results and provide actionable health insights. " +
            "Extract and validate all numerical markers, classify each against age/gender reference ranges, identify deficiencies, " +
            "suboptimal values, and critical flags, and provide food and supplement recommendations with dosages. " +
            "NEVER diagnose medical conditions, NEVER recommend prescription medications or dosage changes, " +
            "NEVER tell users to stop prescribed medications, NEVER provide emergency medical advice, NEVER replace a doctor's interpretation. " +
            "Supplement safety: max Vitamin D3 5000 IU/day, max Omega-3 3000mg EPA+DHA, never recommend iron without confirmed deficiency " +
            "and ferritin < 30, always recommend cofactors (D3 with K2). " +
            "Always end with: \"This analysis is for informational purposes only and does not constitute medical advice. " +
            "Consult your healthcare provider before making changes to medications, supplements, or health routines.\""
    }

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                bloodReportRepository.getReports(userId).collect { reports ->
                    _uiState.value = _uiState.value.copy(reports = reports)
                }
            } catch (_: Exception) { }
        }
    }

    fun extractFromPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExtracting = true, error = null)
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(isExtracting = false, error = "Gemini API key not configured")
                return@launch
            }
            try {
                val result = withContext(Dispatchers.IO) { callExtractionApi(apiKey, bitmap) }
                _uiState.value = _uiState.value.copy(
                    isExtracting = false,
                    draftMarkers = result.first,
                    draftRawText = result.second
                )
            } catch (e: Exception) {
                Log.e(TAG, "Extraction failed", e)
                _uiState.value = _uiState.value.copy(isExtracting = false, error = "Could not extract report: ${e.message}")
            }
        }
    }

    fun saveAndAnalyze(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, error = null)
            try {
                val userId = authRepository.getCurrentUserId()
                val markers = _uiState.value.draftMarkers
                val markersJson = json.encodeToString<List<BloodMarkerDto>>(markers)
                val report = BloodReportEntity(
                    userId = userId,
                    reportDate = System.currentTimeMillis(),
                    source = "scan",
                    rawText = _uiState.value.draftRawText,
                    markersJson = markersJson
                )
                val id = bloodReportRepository.addReport(report)
                _uiState.value = _uiState.value.copy(
                    draftMarkers = emptyList(),
                    draftRawText = null
                )
                analyzeReport(id)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isAnalyzing = false, error = e.message)
            }
        }
    }

    fun analyzeReport(reportId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, error = null)
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(isAnalyzing = false, error = "Gemini API key not configured")
                return@launch
            }
            try {
                val report = bloodReportRepository.getReportOnce(reportId) ?: return@launch
                val markers = decodeMarkers(report.markersJson)
                val userId = authRepository.getCurrentUserId()
                val profile = authRepository.getUserOnce(userId)
                val analysis = withContext(Dispatchers.IO) {
                    callAnalysisApi(apiKey, markers, profile?.displayName, profile?.gender)
                }
                bloodReportRepository.updateAnalysis(reportId, analysis)
                _uiState.value = _uiState.value.copy(isAnalyzing = false)
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed", e)
                _uiState.value = _uiState.value.copy(isAnalyzing = false, error = "Analysis failed: ${e.message}")
            }
        }
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch {
            bloodReportRepository.deleteReport(id)
        }
    }

    fun clearDraft() {
        _uiState.value = _uiState.value.copy(draftMarkers = emptyList(), draftRawText = null, error = null)
    }

    fun updateDraftMarkers(markers: List<BloodMarkerDto>) {
        _uiState.value = _uiState.value.copy(draftMarkers = markers)
    }

    private fun decodeMarkers(markersJson: String?): List<BloodMarkerDto> {
        if (markersJson.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<BloodMarkerDto>>(markersJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun callExtractionApi(apiKey: String, bitmap: Bitmap): Pair<List<BloodMarkerDto>, String> {
        val resized = resizeForUpload(bitmap)
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val prompt = "Analyze this photo of a laboratory blood test report. " +
            "Extract every marker visible: name, value, unit, and reference range low and high. " +
            "Return STRICT JSON only, no markdown, in this exact shape: " +
            "{\"markers\":[{\"name\":\"...\",\"value\":<number or null>,\"unit\":\"...\",\"refLow\":<number or null>,\"refHigh\":<number or null>}],\"rawText\":\"exact text on the report\"}"

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64)
                            })
                        })
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }

        val response = postJson("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$apiKey", body.toString())
        val text = parseGeminiText(response)
        return parseExtractionJson(text)
    }

    private fun callAnalysisApi(
        apiKey: String,
        markers: List<BloodMarkerDto>,
        displayName: String?,
        gender: String?
    ): String {
        val markersJson = JSONObject().apply {
            put("markers", JSONArray().apply {
                markers.forEach { m ->
                    put(JSONObject().apply {
                        put("name", m.name)
                        put("value", m.value)
                        put("unit", m.unit)
                        put("refLow", m.refLow)
                        put("refHigh", m.refHigh)
                    })
                }
            })
            put("patient", JSONObject().apply {
                put("displayName", displayName ?: "User")
                put("gender", gender ?: "Unknown")
            })
        }

        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", BLOOD_ANALYZER_SYSTEM_PROMPT) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Analyze these blood test results for ${displayName ?: "the user"}.\n\nBlood markers JSON:\n$markersJson\n\n" +
                                "Produce a clear, well-formatted markdown report with these sections:\n" +
                                "## Overview\n## Markers to address\n## Supplement and dietary suggestions\n## Doctor referral flags\n## Retest timeline\n\n" +
                                "Flag anything that needs medical attention. End with the standard disclaimer.")
                        })
                    })
                })
            })
        }

        val response = postJson("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$apiKey", body.toString())
        return parseGeminiText(response)
    }

    private fun postJson(urlString: String, body: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 30000
        conn.readTimeout = 60000
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
        val responseCode = conn.responseCode
        val reader = if (responseCode in 200..299) {
            BufferedReader(InputStreamReader(conn.inputStream))
        } else {
            BufferedReader(InputStreamReader(conn.errorStream))
        }
        val response = reader.readText()
        reader.close()
        if (responseCode !in 200..299) {
            throw Exception("Gemini API error $responseCode")
        }
        return response
    }

    private fun parseGeminiText(json: String): String {
        val obj = JSONObject(json)
        val candidates = obj.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).optJSONObject("content")
            if (content != null) {
                val parts = content.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        }
        val error = obj.optJSONObject("error")
        throw Exception(error?.optString("message") ?: "Unknown Gemini error")
    }

    private fun parseExtractionJson(text: String): Pair<List<BloodMarkerDto>, String> {
        val jsonText = text.trim()
        val obj = try {
            if (jsonText.startsWith("{")) JSONObject(jsonText) else JSONObject(extractJsonObject(jsonText))
        } catch (e: Exception) {
            throw Exception("Could not parse extraction: ${text.take(200)}")
        }
        val markers = mutableListOf<BloodMarkerDto>()
        val array = obj.optJSONArray("markers")
        if (array != null) {
            for (i in 0 until array.length()) {
                val m = array.getJSONObject(i)
                markers.add(
                    BloodMarkerDto(
                        name = m.optString("name").trim(),
                        value = m.opt("value")?.let { parseNumber(it) },
                        unit = m.optString("unit").trim(),
                        refLow = m.opt("refLow")?.let { parseNumber(it) },
                        refHigh = m.opt("refHigh")?.let { parseNumber(it) }
                    )
                )
            }
        }
        return markers.filter { it.name.isNotBlank() } to obj.optString("rawText")
    }

    private fun extractJsonObject(text: String): String {
        val start = text.indexOf("{")
        val end = text.lastIndexOf("}")
        if (start == -1 || end == -1 || end < start) throw Exception("No JSON found")
        return text.substring(start, end + 1)
    }

    private fun parseNumber(value: Any): Double? = when (value) {
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        is Double -> value
        is String -> value.replace(",", ".").toDoubleOrNull()
        else -> null
    }

    private fun resizeForUpload(bitmap: Bitmap): Bitmap {
        val maxDim = 1600
        val width = bitmap.width
        val height = bitmap.height
        val longest = maxOf(width, height)
        if (longest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    }
}
