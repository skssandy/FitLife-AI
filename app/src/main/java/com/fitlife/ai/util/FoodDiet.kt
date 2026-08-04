package com.fitlife.ai.util

object FoodDiet {

    fun allowedByDiet(foodDietType: String?, userDietType: String?): Boolean {
        val user = userDietType?.trim()?.lowercase() ?: return true
        val food = foodDietType?.trim()?.lowercase() ?: "any"
        if (food == "any") return true
        return when (user) {
            "non_veg", "non-veg", "nonveg" -> true
            "veg", "vegetarian" -> food in setOf("veg", "jain")
            "jain" -> food == "jain"
            else -> true
        }
    }
}
