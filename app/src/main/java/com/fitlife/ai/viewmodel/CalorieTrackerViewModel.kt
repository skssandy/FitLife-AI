package com.fitlife.ai.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.entity.CalorieEntryEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.CalorieRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalorieTrackerUiState(
    val entries: List<CalorieEntryEntity> = emptyList(),
    val recognizedText: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CalorieTrackerViewModel @Inject constructor(
    private val calorieRepository: CalorieRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalorieTrackerUiState())
    val uiState: StateFlow<CalorieTrackerUiState> = _uiState.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        loadEntries()
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

    fun recognizeText(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(image).await()
                val text = result.text
                _uiState.value = _uiState.value.copy(recognizedText = text, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun addEntry(foodName: String, calories: Int, mealType: String?) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                calorieRepository.addEntry(
                    CalorieEntryEntity(
                        userId = userId,
                        foodName = foodName,
                        calories = calories,
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

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}
