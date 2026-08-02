package com.fitlife.ai.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.BuildConfig
import com.fitlife.ai.data.local.entity.CalorieEntryEntity
import com.fitlife.ai.data.local.entity.FoodItemEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.CalorieRepository
import com.fitlife.ai.data.repository.FoodRepository
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class CalorieTrackerUiState(
    val entries: List<CalorieEntryEntity> = emptyList(),
    val searchResults: List<FoodItemEntity> = emptyList(),
    val recognizedText: String? = null,
    val scannedFood: String? = null,
    val scannedCalories: Int? = null,
    val scannedProtein: Double? = null,
    val scannedCarbs: Double? = null,
    val scannedFat: Double? = null,
    val calorieTarget: Int? = null,
    val proteinTargetG: Int? = null,
    val carbsTargetG: Int? = null,
    val fatTargetG: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CalorieTrackerViewModel @Inject constructor(
    private val calorieRepository: CalorieRepository,
    private val authRepository: AuthRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalorieTrackerUiState())
    val uiState: StateFlow<CalorieTrackerUiState> = _uiState.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient()

    companion object {
        private const val TAG = "CalorieTrackerViewModel"
        private const val GEMINI_MODEL = "gemini-3.5-flash"
    }

    init {
        loadEntries()
        loadTargets()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                calorieRepository.getEntries(userId).collect { entries ->
                    _uiState.value = _uiState.value.copy(entries = entries)
                }
            } catch (_: Exception) { }
        }
    }

    fun loadTargets() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUserId()
                    .let { authRepository.getUserOnce(it) }
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        calorieTarget = user.calorieTarget,
                        proteinTargetG = user.proteinTargetG,
                        carbsTargetG = user.carbsTargetG,
                        fatTargetG = user.fatTargetG
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun saveMacroTargets(
        calorieTarget: Int?,
        proteinTargetG: Int?,
        carbsTargetG: Int?,
        fatTargetG: Int?
    ) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val current = authRepository.getUserOnce(userId)
                val updated = (current ?: com.fitlife.ai.data.local.entity.UserEntity(
                    id = userId,
                    email = "",
                    displayName = null,
                    photoUrl = null,
                    heightCm = null,
                    weightKg = null,
                    dateOfBirth = null,
                    gender = null,
                    fitnessGoal = null,
                    activityLevel = null,
                    workoutFrequency = null,
                    equipment = null,
                    injuries = null,
                    lifestyle = null,
                    sleepHours = null,
                    stressLevel = null
                )).copy(
                    calorieTarget = calorieTarget,
                    proteinTargetG = proteinTargetG,
                    carbsTargetG = carbsTargetG,
                    fatTargetG = fatTargetG
                )
                authRepository.saveProfile(updated)
                _uiState.value = _uiState.value.copy(
                    calorieTarget = calorieTarget,
                    proteinTargetG = proteinTargetG,
                    carbsTargetG = carbsTargetG,
                    fatTargetG = fatTargetG
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun searchFoods(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(searchResults = emptyList())
                return@launch
            }
            foodRepository.search(query).collect { results ->
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
        }
    }

    fun clearFoodSearch() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }

    fun scanBarcode(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                recognizedText = null,
                scannedFood = null,
                scannedCalories = null,
                scannedProtein = null,
                scannedCarbs = null,
                scannedFat = null
            )
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val barcodes = barcodeScanner.process(image).await()
                val value = barcodes.firstOrNull()?.rawValue
                if (value.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No barcode detected. Try again with better lighting."
                    )
                    return@launch
                }
                val food = foodRepository.findByBarcode(value)
                if (food != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recognizedText = "Barcode $value · ${food.name}",
                        scannedFood = food.name,
                        scannedCalories = food.calories,
                        scannedProtein = food.proteinG,
                        scannedCarbs = food.carbsG,
                        scannedFat = food.fatG
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recognizedText = "Barcode $value",
                        scannedFood = "Product $value",
                        scannedCalories = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun analyzeFoodImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                recognizedText = null,
                scannedFood = null,
                scannedCalories = null,
                scannedProtein = null,
                scannedCarbs = null,
                scannedFat = null
            )
            val apiKey = BuildConfig.GEMINI_API_KEY
            val visionResult = if (apiKey.isNotBlank()) {
                try {
                    withContext(Dispatchers.IO) { callGeminiVision(apiKey, bitmap) }
                } catch (e: Exception) {
                    Log.w(TAG, "Gemini vision failed, falling back to OCR", e)
                    null
                }
            } else null

            if (visionResult != null) {
                _uiState.value = _uiState.value.copy(
                    recognizedText = "Analyzed with AI vision",
                    scannedFood = visionResult.foodName,
                    scannedCalories = visionResult.calories,
                    scannedProtein = visionResult.proteinG,
                    scannedCarbs = visionResult.carbsG,
                    scannedFat = visionResult.fatG,
                    isLoading = false
                )
            } else {
                try {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val result = recognizer.process(image).await()
                    val text = result.text
                    val (foodName, calories) = parseNutrition(text)
                    _uiState.value = _uiState.value.copy(
                        recognizedText = text,
                        scannedFood = foodName,
                        scannedCalories = calories,
                        isLoading = false
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun clearScan() {
        _uiState.value = _uiState.value.copy(
            recognizedText = null,
            scannedFood = null,
            scannedCalories = null,
            scannedProtein = null,
            scannedCarbs = null,
            scannedFat = null
        )
    }

    private fun parseNutrition(text: String): Pair<String?, Int?> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val foodName = lines.firstOrNull()
        var calories: Int? = null

        val energyRegex = Regex("""(?i)energy[^0-9]*(\d+)[^0-9]*(kcal|cal\b|calories)""")
        val calRegex = Regex("""(?i)(?:calories|calories per|cal\b)[^0-9]*(\d+)""")
        val bareRegex = Regex("""(?i)(\d+)\s*(?:kcal|cal\b|calories)""")

        for (line in lines) {
            calories = energyRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
            if (calories != null) break
            calories = calRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
            if (calories != null) break
            calories = bareRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
            if (calories != null) break
        }

        return foodName to calories
    }

    private data class VisionResult(
        val foodName: String,
        val calories: Int?,
        val proteinG: Double?,
        val carbsG: Double?,
        val fatG: Double?
    )

    private fun callGeminiVision(apiKey: String, bitmap: Bitmap): VisionResult {
        val resized = resizeForUpload(bitmap)
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 30000
        conn.readTimeout = 60000
        conn.doOutput = true

        val prompt = "Analyze this photo of a food nutrition label or a meal. " +
            "Extract the food name, total calories per serving, protein (g), carbs (g), and fat (g). " +
            "If it is a photo of a meal (not a label), estimate the values. " +
            "Return STRICT JSON only, no markdown and no surrounding text, in this exact shape: " +
            """{"foodName":"...","calories":<number>,"protein":<number or null>,"carbs":<number or null>,"fat":<number or null>}"""

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

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

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

        val text = parseGeminiText(response)
        return parseVisionJson(text)
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

    private fun parseVisionJson(text: String): VisionResult {
        val jsonText = text.trim()
        val jsonObject = try {
            if (jsonText.startsWith("{")) JSONObject(jsonText) else JSONObject(extractJsonObject(jsonText))
        } catch (e: Exception) {
            throw Exception("Could not parse AI response: ${text.take(200)}")
        }
        return VisionResult(
            foodName = jsonObject.optString("foodName").takeIf { it.isNotBlank() } ?: "Food",
            calories = jsonObject.opt("calories")?.let { parseNumber(it)?.toInt() },
            proteinG = jsonObject.opt("protein")?.let { parseNumber(it) },
            carbsG = jsonObject.opt("carbs")?.let { parseNumber(it) },
            fatG = jsonObject.opt("fat")?.let { parseNumber(it) }
        )
    }

    private fun extractJsonObject(text: String): String {
        val start = text.indexOf("{")
        val end = text.lastIndexOf("}")
        if (start == -1 || end == -1 || end < start) throw Exception("No JSON found in AI response")
        return text.substring(start, end + 1)
    }

    private fun parseNumber(value: Any): Double? = when (value) {
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        is Double -> value
        is String -> value.toDoubleOrNull()
        else -> null
    }

    private fun resizeForUpload(bitmap: Bitmap): Bitmap {
        val maxDim = 1280
        val width = bitmap.width
        val height = bitmap.height
        val longest = maxOf(width, height)
        if (longest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    fun addEntry(
        foodName: String,
        calories: Int,
        mealType: String?,
        proteinG: Double? = null,
        carbsG: Double? = null,
        fatG: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                calorieRepository.addEntry(
                    CalorieEntryEntity(
                        userId = userId,
                        foodName = foodName,
                        calories = calories,
                        proteinG = proteinG,
                        carbsG = carbsG,
                        fatG = fatG,
                        mealType = mealType,
                        date = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            calorieRepository.deleteEntry(id)
        }
    }

    fun updateEntry(id: Long, foodName: String, calories: Int, mealType: String?) {
        viewModelScope.launch {
            try {
                val current = _uiState.value.entries.find { it.id == id } ?: return@launch
                calorieRepository.updateEntry(
                    current.copy(foodName = foodName, calories = calories, mealType = mealType)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
        barcodeScanner.close()
    }
}
