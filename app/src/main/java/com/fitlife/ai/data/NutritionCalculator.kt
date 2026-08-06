package com.fitlife.ai.data

data class MacroTargets(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

object NutritionCalculator {

    fun ageFromDateOfBirth(dateOfBirth: String?): Int {
        val parts = dateOfBirth?.split("-") ?: return 30
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return 30
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return (currentYear - year).coerceIn(13, 120)
    }

    fun activityMultiplier(activityLevel: String?): Double = when (activityLevel) {
        "Sedentary" -> 1.2
        "Light" -> 1.375
        "Moderate" -> 1.55
        "Active" -> 1.725
        "Very Active" -> 1.9
        else -> 1.375
    }

    fun calculateBMR(weightKg: Double, heightCm: Double, ageYears: Int, gender: String?): Int {
        val genderConstant = if (gender.equals("Female", ignoreCase = true)) -161 else 5
        return ((10 * weightKg) + (6.25 * heightCm) - (5 * ageYears) + genderConstant).toInt()
    }

    fun calculateTDEE(weightKg: Double, heightCm: Double, dateOfBirth: String?, gender: String?, activityLevel: String?): Int {
        val bmr = calculateBMR(weightKg, heightCm, ageFromDateOfBirth(dateOfBirth), gender)
        return (bmr * activityMultiplier(activityLevel)).toInt()
    }

    fun primaryGoal(goal: String?): String? =
        goal?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    fun calorieTarget(tdee: Int, goal: String?): Int = when (primaryGoal(goal)) {
        "Weight Loss" -> tdee - 400
        "Muscle Gain" -> tdee + 300
        else -> tdee
    }

    fun calculateMacros(tdee: Int, goal: String?, weightKg: Double): MacroTargets {
        val calories = calorieTarget(tdee, goal)
        val proteinG = when (primaryGoal(goal)) {
            "Weight Loss" -> (weightKg * 2.0).toInt()
            "Muscle Gain" -> (weightKg * 1.8).toInt()
            else -> (weightKg * 1.6).toInt()
        }
        val fatG = (calories * 0.25 / 9).toInt()
        val carbCalories = (calories - (proteinG * 4) - (fatG * 9)).coerceAtLeast(0)
        val carbsG = (carbCalories / 4).toInt()
        return MacroTargets(calories, proteinG, carbsG, fatG)
    }

    fun calculateHydrationTarget(weightKg: Double, activityLevel: String?): Int {
        val base = (weightKg * 35).toInt()
        val extra = when (activityLevel) {
            "Active", "Very Active" -> 500
            else -> 0
        }
        return base + extra
    }

    /**
     * Weighted daily adherence (0.0 - 1.0).
     * Calories 40%, Protein 30%, Carbs 15%, Fat 10%, Hydration 5%.
     * Components with no target are excluded and the remaining weights renormalized.
     */
    fun adherence(
        calories: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        waterMl: Int,
        targets: MacroTargets?,
        hydrationTargetMl: Int?
    ): Double {
        if (targets == null) return 0.0
        var weightSum = 0.0
        var score = 0.0

        fun add(consumed: Double, target: Int, weight: Double) {
            if (target > 0) {
                weightSum += weight
                score += weight * (consumed / target).coerceIn(0.0, 1.0)
            }
        }

        add(calories, targets.calories, 0.40)
        add(proteinG, targets.proteinG, 0.30)
        add(carbsG, targets.carbsG, 0.15)
        add(fatG, targets.fatG, 0.10)
        if (hydrationTargetMl != null && hydrationTargetMl > 0) {
            weightSum += 0.05
            score += 0.05 * (waterMl.toDouble() / hydrationTargetMl).coerceIn(0.0, 1.0)
        }

        return if (weightSum == 0.0) 0.0 else score / weightSum
    }
}
