package com.fitlife.ai.data.remote.api

import com.fitlife.ai.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor(
    private val geminiApi: GeminiApi
) {
    companion object {
        const val API_KEY = "AIzaSyDummyKeyReplaceWithRealKey"
    }

    private val systemPrompt = """
        You are FitLife AI, an expert personal fitness coach and nutrition advisor.
        
        Your capabilities:
        - Create personalized workout programs based on user goals, fitness level, and available equipment
        - Provide nutrition advice and meal planning
        - Analyze fitness progress and suggest adjustments
        - Answer questions about exercise form, recovery, and injury prevention
        - Analyze blood test results
        - Provide cycle-aware fitness and nutrition recommendations
        
        Guidelines:
        - Always be encouraging and motivational
        - Give specific, actionable advice
        - Consider the user's profile (age, weight, height, fitness goal, activity level)
        - Keep responses concise but informative
        - Use emoji sparingly for emphasis
        - If asked about medical conditions, recommend consulting a healthcare professional
    """.trimIndent()

    suspend fun chat(
        userProfile: UserProfile?,
        messages: List<Pair<String, String>>
    ): Result<String> {
        val contextPrompt = buildContextPrompt(userProfile)
        val fullSystemPrompt = "$systemPrompt\n\nUser Context:\n$contextPrompt"
        return geminiApi.generateContent(API_KEY, fullSystemPrompt, messages)
    }

    suspend fun getWorkoutRecommendation(
        userProfile: UserProfile?,
        focus: String = "general"
    ): Result<String> {
        val prompt = """
            User Context: ${buildContextPrompt(userProfile)}
            
            Task: Create a personalized workout recommendation for today focusing on $focus.
            Include specific exercises, sets, reps, and rest periods.
        """.trimIndent()
        return geminiApi.generateContent(API_KEY, systemPrompt, listOf("user" to prompt))
    }

    suspend fun getNutritionAdvice(
        userProfile: UserProfile?,
        recentMeals: String = "No meals logged today"
    ): Result<String> {
        val prompt = """
            User Context: ${buildContextPrompt(userProfile)}
            Recent meals: $recentMeals
            
            Task: Provide nutrition advice based on what they've eaten and what they should eat for remaining meals.
        """.trimIndent()
        return geminiApi.generateContent(API_KEY, systemPrompt, listOf("user" to prompt))
    }

    suspend fun analyzeBloodReport(
        userProfile: UserProfile?,
        reportText: String
    ): Result<String> {
        val prompt = """
            User Context: ${buildContextPrompt(userProfile)}
            Blood Report Data: $reportText
            
            Task: Analyze this blood report. Identify abnormal values, health concerns, and lifestyle recommendations.
        """.trimIndent()
        return geminiApi.generateContent(API_KEY, systemPrompt, listOf("user" to prompt))
    }

    suspend fun getCycleAdvice(
        userProfile: UserProfile?,
        cycleDay: Int,
        phase: String,
        symptoms: List<String>
    ): Result<String> {
        val prompt = """
            User Context: ${buildContextPrompt(userProfile)}
            Cycle Day: $cycleDay, Phase: $phase, Symptoms: ${symptoms.joinToString(", ")}
            
            Task: Provide fitness and nutrition advice for this cycle phase.
        """.trimIndent()
        return geminiApi.generateContent(API_KEY, systemPrompt, listOf("user" to prompt))
    }

    private fun buildContextPrompt(profile: UserProfile?): String {
        if (profile == null) return "No user profile available yet."
        return """
            Name: ${profile.fullName}
            Gender: ${profile.gender}
            Age: ${if (profile.dateOfBirth > 0) ((System.currentTimeMillis() - profile.dateOfBirth) / (365.25 * 24 * 60 * 60 * 1000)).toInt() else "Unknown"}
            Height: ${profile.heightCm} cm
            Weight: ${profile.weightKg} kg
            Fitness Goal: ${profile.fitnessGoal}
            Activity Level: ${profile.activityLevel}
            Dietary Restrictions: ${profile.dietaryRestrictions.joinToString(", ").ifEmpty { "None" }}
            Medical Conditions: ${profile.medicalConditions.joinToString(", ").ifEmpty { "None" }}
        """.trimIndent()
    }
}
