package com.fitlife.ai.ui.navigation

sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object Signup : Routes("signup")
    data object Onboarding : Routes("onboarding")
    data object Home : Routes("home")
    data object Profile : Routes("profile")
    data object Workout : Routes("workout")
    data object Programs : Routes("programs")
    data object AIChat : Routes("ai_chat")
    data object CalorieTracker : Routes("calorie_tracker")
    data object NutritionPlan : Routes("nutrition_plan")
    data object Blood : Routes("blood")
    data object Cycle : Routes("cycle")
    data object Water : Routes("water")
    data object Settings : Routes("settings")
}
