package com.fitlife.ai.domain.model

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val gender: String = "male",
    val dateOfBirth: Long = 0L,
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val fitnessGoal: String = "general_fitness",
    val activityLevel: String = "moderate",
    val dietaryRestrictions: List<String> = emptyList(),
    val medicalConditions: List<String> = emptyList(),
    val onboardingCompleted: Boolean = false,
    val onboardingStep: Int = 0
)

data class WorkoutProgram(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val difficulty: String = "beginner",
    val category: String = "general",
    val durationWeeks: Int = 4,
    val goals: List<String> = emptyList(),
    val days: List<WorkoutDay> = emptyList(),
    val isAiGenerated: Boolean = false,
    val isActive: Boolean = true
)

data class WorkoutDay(
    val name: String = "",
    val exercises: List<Exercise> = emptyList(),
    val isRestDay: Boolean = false
)

data class Exercise(
    val id: String = "",
    val name: String = "",
    val sets: Int = 3,
    val reps: String = "8-12",
    val weight: String = "",
    val restSeconds: Int = 60,
    val notes: String = "",
    val muscleGroup: String = ""
)

data class WorkoutSession(
    val id: String = "",
    val programId: String = "",
    val dayIndex: Int = 0,
    val dayName: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val durationMinutes: Int = 0,
    val completed: Boolean = false,
    val exerciseLogs: List<ExerciseLog> = emptyList()
)

data class ExerciseLog(
    val id: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val sets: Int = 0,
    val reps: String = "",
    val weight: String = "",
    val completed: Boolean = false
)

data class NutritionLog(
    val id: String = "",
    val date: String = "",
    val mealType: String = "",
    val foodName: String = "",
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0
)

data class DailyNutritionSummary(
    val date: String = "",
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val waterMl: Int = 0,
    val goalCalories: Double = 2000.0,
    val goalProtein: Double = 150.0,
    val goalCarbs: Double = 250.0,
    val goalFat: Double = 65.0,
    val goalWaterMl: Int = 3000
)

data class BloodAnalysis(
    val id: String = "",
    val fileName: String = "",
    val analysisDate: Long = System.currentTimeMillis(),
    val overallSummary: String = "",
    val recommendations: List<String> = emptyList(),
    val markers: List<BloodMarker> = emptyList(),
    val status: String = "pending"
)

data class BloodMarker(
    val id: String = "",
    val name: String = "",
    val value: Double = 0.0,
    val unit: String = "",
    val referenceMin: Double = 0.0,
    val referenceMax: Double = 0.0,
    val status: String = "normal"
)

data class CycleEntry(
    val id: String = "",
    val date: String = "",
    val dayOfCycle: Int = 0,
    val phase: String = "",
    val flow: String = "",
    val symptoms: List<String> = emptyList(),
    val mood: String = "",
    val temperature: Double = 0.0
)

data class ProgressEntry(
    val id: String = "",
    val date: String = "",
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val muscleMassKg: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val notes: String = ""
)
