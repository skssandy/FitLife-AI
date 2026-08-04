package com.fitlife.ai.data

import com.fitlife.ai.data.local.entity.FoodItemEntity
import com.fitlife.ai.util.FoodDiet

data class MealPlan(
    val name: String,
    val share: Double,
    val targetCalories: Int,
    val items: List<FoodItemEntity>
)

object NutritionPlanGenerator {

    private val mealCategories = mapOf(
        "Breakfast" to listOf("Grains & Bread", "Dairy & Eggs", "Fruits", "Indian Staples"),
        "Mid-Morning" to listOf("Fruits", "Nuts & Seeds", "Indian Sweets"),
        "Lunch" to listOf(
            "Grains & Bread", "Meat & Poultry", "Vegetables", "Legumes & Beans",
            "Indian Staples", "Indian Curries", "Indian Non-Veg"
        ),
        "Dinner" to listOf(
            "Meat & Poultry", "Fish & Seafood", "Vegetables", "Legumes & Beans",
            "Indian Curries", "Indian Non-Veg"
        ),
        "Snack" to listOf("Fruits", "Nuts & Seeds", "Dairy & Eggs", "Indian Snacks"),
        "Evening Snack" to listOf("Fruits", "Nuts & Seeds", "Indian Snacks")
    )

    fun generatePlan(
        foods: List<FoodItemEntity>,
        targets: MacroTargets,
        dietType: String? = null,
        mealCount: Int? = null
    ): List<MealPlan> {
        val allowed = foods.filter { FoodDiet.allowedByDiet(it.dietType, dietType) }
        val byCategory = allowed.groupBy { it.category }
        return mealSplitFor(mealCount).map { (name, share) ->
            val targetCalories = (targets.calories * share).toInt()
            val categories = mealCategories[name] ?: emptyList()
            val items = categories
                .flatMap { byCategory[it].orEmpty() }
                .distinctBy { it.name.lowercase() }
                .take(3)
            MealPlan(name, share, targetCalories, items)
        }
    }

    private fun mealSplitFor(mealCount: Int?): List<Pair<String, Double>> = when (mealCount) {
        3 -> listOf(
            "Breakfast" to 0.30,
            "Lunch" to 0.40,
            "Dinner" to 0.30
        )
        5 -> listOf(
            "Breakfast" to 0.20,
            "Lunch" to 0.30,
            "Snack" to 0.15,
            "Dinner" to 0.25,
            "Evening Snack" to 0.10
        )
        6 -> listOf(
            "Breakfast" to 0.20,
            "Mid-Morning" to 0.10,
            "Lunch" to 0.30,
            "Snack" to 0.10,
            "Dinner" to 0.25,
            "Evening Snack" to 0.05
        )
        else -> listOf(
            "Breakfast" to 0.25,
            "Lunch" to 0.35,
            "Dinner" to 0.30,
            "Snack" to 0.10
        )
    }
}
