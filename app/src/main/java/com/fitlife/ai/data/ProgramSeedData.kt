package com.fitlife.ai.data

import kotlinx.serialization.Serializable

@Serializable
data class ProgramExercise(
    val name: String,
    val sets: Int,
    val reps: String
)

@Serializable
data class ProgramDay(
    val name: String,
    val exercises: List<ProgramExercise>
)

@Serializable
data class WorkoutProgramTemplate(
    val name: String,
    val description: String,
    val goal: String,
    val days: List<ProgramDay>
)

object ProgramSeedData {
    val templates: List<WorkoutProgramTemplate> = listOf(
        WorkoutProgramTemplate(
            name = "Beginner Full Body",
            description = "3 days a week, full body each session. Perfect for building a base.",
            goal = "General Fitness",
            days = listOf(
                ProgramDay(
                    name = "Day 1",
                    exercises = listOf(
                        ProgramExercise("Goblet Squat", 3, "10-12"),
                        ProgramExercise("Push Ups", 3, "8-12"),
                        ProgramExercise("Seated Cable Row", 3, "10-12"),
                        ProgramExercise("Plank", 3, "30-45 sec"),
                        ProgramExercise("Bicep Curls", 2, "10-12")
                    )
                ),
                ProgramDay(
                    name = "Day 2",
                    exercises = listOf(
                        ProgramExercise("Romanian Deadlift", 3, "10-12"),
                        ProgramExercise("Dumbbell Bench Press", 3, "8-12"),
                        ProgramExercise("Lat Pulldown", 3, "10-12"),
                        ProgramExercise("Side Plank", 3, "20-30 sec/side")
                    )
                ),
                ProgramDay(
                    name = "Day 3",
                    exercises = listOf(
                        ProgramExercise("Lunges", 3, "10-12/leg"),
                        ProgramExercise("Overhead Press", 3, "8-10"),
                        ProgramExercise("Face Pulls", 3, "12-15"),
                        ProgramExercise("Cable Crunch", 3, "12-15")
                    )
                )
            )
        ),
        WorkoutProgramTemplate(
            name = "Push Pull Legs",
            description = "The classic 6-day split for strength and hypertrophy.",
            goal = "Muscle Gain",
            days = listOf(
                ProgramDay(
                    name = "Day 1 - Push",
                    exercises = listOf(
                        ProgramExercise("Barbell Bench Press", 4, "6-8"),
                        ProgramExercise("Incline Dumbbell Press", 3, "8-10"),
                        ProgramExercise("Overhead Press", 3, "8-10"),
                        ProgramExercise("Tricep Pushdown", 3, "10-12"),
                        ProgramExercise("Lateral Raises", 3, "12-15")
                    )
                ),
                ProgramDay(
                    name = "Day 2 - Pull",
                    exercises = listOf(
                        ProgramExercise("Deadlift", 4, "5-6"),
                        ProgramExercise("Lat Pulldown", 3, "8-10"),
                        ProgramExercise("Barbell Row", 3, "8-10"),
                        ProgramExercise("Face Pulls", 3, "12-15"),
                        ProgramExercise("Bicep Curls", 3, "10-12")
                    )
                ),
                ProgramDay(
                    name = "Day 3 - Legs",
                    exercises = listOf(
                        ProgramExercise("Squat", 4, "6-8"),
                        ProgramExercise("Romanian Deadlift", 3, "8-10"),
                        ProgramExercise("Leg Press", 3, "10-12"),
                        ProgramExercise("Walking Lunges", 3, "12/leg"),
                        ProgramExercise("Calf Raises", 4, "15-20")
                    )
                ),
                ProgramDay(
                    name = "Day 4 - Push",
                    exercises = listOf(
                        ProgramExercise("Incline Bench Press", 4, "6-8"),
                        ProgramExercise("Dips", 3, "8-12"),
                        ProgramExercise("Arnold Press", 3, "8-10"),
                        ProgramExercise("Skull Crushers", 3, "10-12"),
                        ProgramExercise("Front Raises", 3, "12-15")
                    )
                ),
                ProgramDay(
                    name = "Day 5 - Pull",
                    exercises = listOf(
                        ProgramExercise("Pull Ups", 4, "6-10"),
                        ProgramExercise("T-Bar Row", 3, "8-10"),
                        ProgramExercise("Seated Cable Row", 3, "10-12"),
                        ProgramExercise("Hammer Curls", 3, "10-12"),
                        ProgramExercise("Rear Delt Fly", 3, "12-15")
                    )
                ),
                ProgramDay(
                    name = "Day 6 - Legs",
                    exercises = listOf(
                        ProgramExercise("Front Squat", 4, "6-8"),
                        ProgramExercise("Bulgarian Split Squat", 3, "10/leg"),
                        ProgramExercise("Leg Curl", 3, "10-12"),
                        ProgramExercise("Hip Thrust", 3, "10-12"),
                        ProgramExercise("Seated Calf Raise", 4, "15-20")
                    )
                )
            )
        ),
        WorkoutProgramTemplate(
            name = "Upper Lower Split",
            description = "4 days a week, alternating upper and lower body.",
            goal = "Strength",
            days = listOf(
                ProgramDay(
                    name = "Day 1 - Upper",
                    exercises = listOf(
                        ProgramExercise("Bench Press", 4, "5-6"),
                        ProgramExercise("Bent Over Row", 4, "6-8"),
                        ProgramExercise("Overhead Press", 3, "6-8"),
                        ProgramExercise("Lat Pulldown", 3, "8-10"),
                        ProgramExercise("Bicep Curls", 3, "10-12")
                    )
                ),
                ProgramDay(
                    name = "Day 2 - Lower",
                    exercises = listOf(
                        ProgramExercise("Squat", 4, "5-6"),
                        ProgramExercise("Romanian Deadlift", 3, "8-10"),
                        ProgramExercise("Leg Press", 3, "10-12"),
                        ProgramExercise("Calf Raises", 4, "15-20")
                    )
                ),
                ProgramDay(
                    name = "Day 3 - Upper",
                    exercises = listOf(
                        ProgramExercise("Deadlift", 4, "5-6"),
                        ProgramExercise("Incline Press", 4, "6-8"),
                        ProgramExercise("Chin Ups", 3, "6-10"),
                        ProgramExercise("Lateral Raises", 3, "12-15"),
                        ProgramExercise("Tricep Pushdown", 3, "10-12")
                    )
                ),
                ProgramDay(
                    name = "Day 4 - Lower",
                    exercises = listOf(
                        ProgramExercise("Front Squat", 4, "6-8"),
                        ProgramExercise("Bulgarian Split Squat", 3, "10/leg"),
                        ProgramExercise("Hip Thrust", 3, "10-12"),
                        ProgramExercise("Leg Curl", 3, "10-12")
                    )
                )
            )
        )
    )
}
