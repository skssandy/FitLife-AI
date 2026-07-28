package com.fitlife.ai.ui.screens.nutrition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.local.dao.NutritionDao
import com.fitlife.ai.data.local.dao.WaterDao
import com.fitlife.ai.data.local.entity.NutritionLogEntity
import com.fitlife.ai.data.local.entity.WaterLogEntity
import com.fitlife.ai.data.remote.api.GeminiService
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class NutritionUiState(
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalWaterMl: Int = 0,
    val goalCalories: Double = 2000.0,
    val goalProtein: Double = 150.0,
    val goalCarbs: Double = 250.0,
    val goalFat: Double = 65.0,
    val goalWaterMl: Int = 3000,
    val meals: List<NutritionLogEntity> = emptyList(),
    val aiSuggestion: String? = null,
    val isLoadingAi: Boolean = false
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionDao: NutritionDao,
    private val waterDao: WaterDao,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now().toString()

    init {
        loadData()
    }

    private fun loadData() {
        val userId = authRepository.currentUserId ?: return

        viewModelScope.launch {
            combine(
                nutritionDao.getLogsForDate(userId, today),
                nutritionDao.getTotalCalories(userId, today),
                waterDao.getTotalMl(userId, today)
            ) { meals, calories, water ->
                _uiState.value.copy(
                    meals = meals,
                    totalCalories = calories ?: 0.0,
                    totalProtein = meals.sumOf { it.proteinG },
                    totalCarbs = meals.sumOf { it.carbsG },
                    totalFat = meals.sumOf { it.fatG },
                    totalWaterMl = water ?: 0
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addWater(amountMl: Int) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            waterDao.upsert(
                WaterLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    date = today,
                    amountMl = amountMl
                )
            )
        }
    }

    fun logMeal(name: String, calories: Double, protein: Double, carbs: Double, fat: Double, mealType: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            nutritionDao.upsert(
                NutritionLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    date = today,
                    mealType = mealType,
                    foodName = name,
                    calories = calories,
                    proteinG = protein,
                    carbsG = carbs,
                    fatG = fat
                )
            )
        }
    }

    fun deleteMeal(meal: NutritionLogEntity) {
        viewModelScope.launch {
            nutritionDao.delete(meal)
        }
    }

    fun getAiNutritionAdvice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAi = true)
            val mealsText = _uiState.value.meals.joinToString("\n") {
                "${it.mealType}: ${it.foodName} - ${it.calories}kcal"
            }
            geminiService.getNutritionAdvice(
                profileRepository.getAnyProfile().first(),
                mealsText.ifEmpty { "No meals logged today" }
            ).onSuccess { advice ->
                _uiState.value = _uiState.value.copy(aiSuggestion = advice, isLoadingAi = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingAi = false)
            }
        }
    }
}
