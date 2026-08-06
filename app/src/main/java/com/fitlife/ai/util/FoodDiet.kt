package com.fitlife.ai.util

object FoodDiet {

    const val VEG = "veg"
    const val NON_VEG = "non_veg"
    const val ANY = "any"

    val options: List<String> = listOf("Vegetarian", "Non-Vegetarian", "No preference")

    fun allowedByDiet(foodDietType: String?, userDietType: String?): Boolean {
        val keys = (userDietType ?: "")
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (keys.isEmpty() || keys.contains("any") || keys.contains("no preference") || keys.contains("none")) {
            return true
        }
        val food = foodDietType?.trim()?.lowercase() ?: "any"
        if (food == "any") return true
        val hasNonVeg = keys.any { it == "non_veg" || it == "non-veg" || it == "nonveg" }
        if (hasNonVeg) return true
        val hasVeg = keys.any { it == "veg" || it == "vegetarian" || it == "jain" }
        if (hasVeg) return food in setOf("veg", "jain")
        return true
    }

    fun labelForKey(key: String?): String = when (key?.trim()?.lowercase()) {
        "veg", "vegetarian", "jain" -> "Vegetarian"
        "non_veg", "non-veg", "nonveg" -> "Non-Vegetarian"
        else -> "No preference"
    }

    fun keyForLabel(label: String): String = when (label) {
        "Vegetarian" -> VEG
        "Non-Vegetarian" -> NON_VEG
        else -> ANY
    }

    fun selectedLabels(stored: String?): List<String> =
        (stored ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { labelForKey(it) }
            .distinct()

    fun keysForSelected(labels: List<String>): String {
        if (labels.contains("No preference") || labels.isEmpty()) return ANY
        val keys = labels.map { keyForLabel(it) }.distinct().filter { it != ANY }
        return keys.joinToString(",").ifBlank { ANY }
    }
}
