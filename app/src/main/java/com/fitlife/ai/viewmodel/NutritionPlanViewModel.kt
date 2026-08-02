package com.fitlife.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlife.ai.data.MacroTargets
import com.fitlife.ai.data.MealPlan
import com.fitlife.ai.data.NutritionCalculator
import com.fitlife.ai.data.NutritionPlanGenerator
import com.fitlife.ai.data.local.entity.UserEntity
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.CalorieRepository
import com.fitlife.ai.data.repository.FoodRepository
import com.fitlife.ai.data.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class NutritionPlanUiState(
    val targets: MacroTargets? = null,
    val hydrationTargetMl: Int? = null,
    val caloriesEaten: Int = 0,
    val proteinEaten: Double = 0.0,
    val carbsEaten: Double = 0.0,
    val fatEaten: Double = 0.0,
    val waterMl: Int = 0,
    val adherenceToday: Double = 0.0,
    val weeklyAdherence: List<Pair<String, Int>> = emptyList(),
    val plan: List<MealPlan> = emptyList(),
    val hasProfileData: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NutritionPlanViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val calorieRepository: CalorieRepository,
    private val waterRepository: WaterRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionPlanUiState())
    val uiState: StateFlow<NutritionPlanUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val user = authRepository.getUserOnce(userId)
                val foods = foodRepository.allOnce()

                val targets = user?.let {
                    val tdee = NutritionCalculator.calculateTDEE(
                        it.weightKg ?: 0.0,
                        it.heightCm ?: 0.0,
                        it.dateOfBirth,
                        it.gender,
                        it.activityLevel
                    )
                    NutritionCalculator.calculateMacros(tdee, it.fitnessGoal, it.weightKg ?: 60.0)
                }
                val hydrationTarget = user?.let {
                    it.hydrationTargetMl
                        ?: (it.weightKg?.let { w -> NutritionCalculator.calculateHydrationTarget(w, it.activityLevel) })
                }

                val today = startOfDay()
                val weekStart = startOfDay(6)

                calorieRepository.getEntries(userId).collect { entries ->
                    val todayEntries = entries.filter { isSameDay(it.date, today) }
                    val caloriesEaten = todayEntries.sumOf { it.calories }
                    val proteinEaten = todayEntries.sumOf { it.proteinG ?: 0.0 }
                    val carbsEaten = todayEntries.sumOf { it.carbsG ?: 0.0 }
                    val fatEaten = todayEntries.sumOf { it.fatG ?: 0.0 }

                    val water = waterRepository.getLogs(userId)
                    water.collect { logs ->
                        val waterMl = logs.filter { isSameDay(it.date, today) }.sumOf { it.amountMl }
                        val adherence = NutritionCalculator.adherence(
                            caloriesEaten.toDouble(),
                            proteinEaten,
                            carbsEaten,
                            fatEaten,
                            waterMl,
                            targets,
                            hydrationTarget
                        )
                        val weekly = buildWeeklyAdherence(entries, logs, weekStart, targets, hydrationTarget)
                        val plan = if (targets != null) NutritionPlanGenerator.generatePlan(foods, targets) else emptyList()
                        _uiState.value = NutritionPlanUiState(
                            targets = targets,
                            hydrationTargetMl = hydrationTarget,
                            caloriesEaten = caloriesEaten,
                            proteinEaten = proteinEaten,
                            carbsEaten = carbsEaten,
                            fatEaten = fatEaten,
                            waterMl = waterMl,
                            adherenceToday = adherence,
                            weeklyAdherence = weekly,
                            plan = plan,
                            hasProfileData = user?.weightKg != null && user.heightCm != null,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun recalculateTargets() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val user = authRepository.getUserOnce(userId) ?: return@launch
                val weight = user.weightKg ?: return@launch
                val height = user.heightCm ?: return@launch
                val tdee = NutritionCalculator.calculateTDEE(weight, height, user.dateOfBirth, user.gender, user.activityLevel)
                val macros = NutritionCalculator.calculateMacros(tdee, user.fitnessGoal, weight)
                val hydration = user.hydrationTargetMl
                    ?: NutritionCalculator.calculateHydrationTarget(weight, user.activityLevel)
                authRepository.saveProfile(
                    user.copy(
                        calorieTarget = macros.calories,
                        proteinTargetG = macros.proteinG,
                        carbsTargetG = macros.carbsG,
                        fatTargetG = macros.fatG,
                        hydrationTargetMl = hydration
                    )
                )
                _uiState.value = _uiState.value.copy(
                    targets = macros,
                    hydrationTargetMl = hydration,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun buildWeeklyAdherence(
        entries: List<com.fitlife.ai.data.local.entity.CalorieEntryEntity>,
        logs: List<com.fitlife.ai.data.local.entity.WaterLogEntity>,
        weekStart: Long,
        targets: MacroTargets?,
        hydrationTarget: Int?
    ): List<Pair<String, Int>> {
        val dayFormat = java.text.SimpleDateFormat("E", java.util.Locale.getDefault())
        return (0..6).map { offset ->
            val dayStart = weekStart + offset * 86400000L
            val dayEntries = entries.filter { isSameDay(it.date, dayStart) }
            val calories = dayEntries.sumOf { it.calories }
            val protein = dayEntries.sumOf { it.proteinG ?: 0.0 }
            val carbs = dayEntries.sumOf { it.carbsG ?: 0.0 }
            val fat = dayEntries.sumOf { it.fatG ?: 0.0 }
            val water = logs.filter { isSameDay(it.date, dayStart) }.sumOf { it.amountMl }
            val score = NutritionCalculator.adherence(
                calories.toDouble(), protein, carbs, fat, water, targets, hydrationTarget
            )
            dayFormat.format(java.util.Date(dayStart)) to (score * 100).toInt()
        }
    }

    private fun isSameDay(ts: Long, dayStart: Long): Boolean =
        ts in dayStart until (dayStart + 86400000L)

    private fun startOfDay(daysAgo: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
