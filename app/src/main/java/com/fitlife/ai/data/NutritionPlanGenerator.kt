package com.fitlife.ai.data

import com.fitlife.ai.data.local.entity.FoodItemEntity

data class MealPlan(
    val name: String,
    val share: Double,
    val targetCalories: Int,
    val items: List<FoodItemEntity>
)

object NutritionPlanGenerator {

    private val mealSplit = listOf(
        "Breakfast" to 0.25,
        "Lunch" to 0.35,
        "Dinner" to 0.30,
        "Snack" to 0.10
    )

    private val mealCategories = mapOf(
        "Breakfast" to listOf("Grains & Bread", "Dairy & Eggs", "Fruits"),
        "Lunch" to listOf("Grains & Bread", "Meat & Poultry", "Vegetables", "Legumes & Beans"),
        "Dinner" to listOf("Meat & Poultry", "Fish & Seafood", "Vegetables", "Legumes & Beans"),
        "Snack" to listOf("Fruits", "Nuts & Seeds", "Dairy & Eggs")
    )

    fun generatePlan(foods: List<FoodItemEntity>, targets: MacroTargets): List<MealPlan> {
        val byCategory = foods.groupBy { it.category }
        return mealSplit.map { (name, share) ->
            val targetCalories = (targets.calories * share).toInt()
            val categories = mealCategories[name] ?: emptyList()
            val items = categories
                .flatMap { byCategory[it].orEmpty() }
                .distinctBy { it.name.lowercase() }
                .take(3)
            MealPlan(name, share, targetCalories, items)
        }
    }
}
