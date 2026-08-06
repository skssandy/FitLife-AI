package com.fitlife.ai.data

import java.net.URLEncoder

data class ExerciseInfo(
    val name: String,
    val imageUrl: String?,
    val videoUrl: String,
    val instructions: String
)

/**
 * Curated exercise reference: sample images (free-exercise-db, MIT), YouTube
 * how-to links and coaching instructions. Fallbacks are generated for any
 * exercise name not listed explicitly.
 */
object ExerciseLibrary {

    private const val IMG_BASE =
        "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"

    private val images = mapOf(
        "Goblet Squat" to "Goblet_Squat",
        "Push Ups" to "Push_Up_to_Side_Plank",
        "Seated Cable Row" to "Elevated_Cable_Rows",
        "Plank" to "Plank",
        "Bicep Curls" to "Dumbbell_Alternate_Bicep_Curl",
        "Romanian Deadlift" to "Romanian_Deadlift",
        "Dumbbell Bench Press" to "Decline_Dumbbell_Bench_Press",
        "Lat Pulldown" to "Close-Grip_Front_Lat_Pulldown",
        "Side Plank" to "Push_Up_to_Side_Plank",
        "Lunges" to "Barbell_Lunge",
        "Overhead Press" to "Alternating_Cable_Shoulder_Press",
        "Face Pulls" to "Face_Pull",
        "Cable Crunch" to "Bosu_Ball_Cable_Crunch_With_Side_Bends",
        "Barbell Bench Press" to "Barbell_Bench_Press_-_Medium_Grip",
        "Incline Dumbbell Press" to "Barbell_Incline_Bench_Press_-_Medium_Grip",
        "Tricep Pushdown" to "Body_Tricep_Press",
        "Lateral Raises" to "Cable_Seated_Lateral_Raise",
        "Deadlift" to "Axle_Deadlift",
        "Barbell Row" to "Bent_Over_Barbell_Row",
        "Squat" to "Barbell_Full_Squat",
        "Leg Press" to "Calf_Press_On_The_Leg_Press_Machine",
        "Walking Lunges" to "Barbell_Walking_Lunge",
        "Calf Raises" to "Rocking_Standing_Calf_Raise",
        "Incline Bench Press" to "Barbell_Incline_Bench_Press_-_Medium_Grip",
        "Dips" to "Bench_Dips",
        "Arnold Press" to "Kettlebell_Arnold_Press",
        "Skull Crushers" to "Band_Skull_Crusher",
        "Front Raises" to "Front_Raise_And_Pullover",
        "Pull Ups" to "Weighted_Pull_Ups",
        "T-Bar Row" to "Lying_T-Bar_Row",
        "Hammer Curls" to "Alternate_Hammer_Curl",
        "Rear Delt Fly" to "Cable_Rear_Delt_Fly",
        "Front Squat" to "Front_Squat_Clean_Grip",
        "Leg Curl" to "Ball_Leg_Curl",
        "Hip Thrust" to "Barbell_Hip_Thrust",
        "Seated Calf Raise" to "Barbell_Seated_Calf_Raise",
        "Bench Press" to "Barbell_Bench_Press_-_Medium_Grip",
        "Bent Over Row" to "Bent_Over_Barbell_Row",
        "Glute Bridges" to "Barbell_Glute_Bridge",
        "Incline Push Ups" to "Incline_Push-Up",
        "Mountain Climbers" to "Mountain_Climbers",
        "Band Pull Aparts" to "Band_Pull_Apart",
        "Diamond Push Ups" to "Clock_Push-Up",
        "Superman Hold" to "Superman",
        "One Arm Dumbbell Row" to "One-Arm_Dumbbell_Row",
        "Dumbbell Overhead Press" to "Dumbbell_Shoulder_Press",
        "Dumbbell Curls" to "Alternate_Incline_Dumbbell_Curl",
        "Dumbbell Shrugs" to "Dumbbell_Shrug",
        "Dumbbell Romanian Deadlift" to "Stiff-Legged_Dumbbell_Deadlift",
        "Dumbbell Lunges" to "Dumbbell_Lunges",
        "Dumbbell Floor Press" to "Dumbbell_Floor_Press",
        "Tricep Overhead Extension" to "Cable_One_Arm_Tricep_Extension",
        "Dumbbell Deadlift" to "Stiff-Legged_Dumbbell_Deadlift",
        "Step Ups" to "Barbell_Step_Ups",
        "Dumbbell Glute Bridge" to "Barbell_Glute_Bridge"
    )

    private val instructions = mapOf(
        "Goblet Squat" to "Hold a dumbbell at your chest, feet shoulder-width. Sit back and down, keep your chest up and heels down, then drive through your heels to stand.",
        "Push Ups" to "Hands slightly wider than shoulders, body in one straight line. Lower your chest toward the floor, then press back up.",
        "Seated Cable Row" to "Sit tall with a soft knee bend. Pull the handle to your waist, squeeze your shoulder blades, then slowly extend your arms.",
        "Plank" to "Support on your forearms, body in a straight line from head to heels. Brace your core and breathe steadily.",
        "Bicep Curls" to "Pin elbows at your sides. Curl the weight up, squeeze the biceps at the top, lower under control.",
        "Romanian Deadlift" to "Hinge at the hips with soft knees. Slide the weight down your legs keeping your back flat, then drive your hips forward to stand.",
        "Dumbbell Bench Press" to "Lie on a bench, press the dumbbells up over your chest, lower with control until your elbows are below chest level.",
        "Lat Pulldown" to "Grip the bar wider than shoulders. Pull it to your upper chest while squeezing your lats, then return slowly.",
        "Side Plank" to "Support on one forearm with stacked feet. Raise your hips into a straight line and hold.",
        "Lunges" to "Step forward and lower until both knees are about 90 degrees. Push through the front heel to return.",
        "Overhead Press" to "Stand tall with the weight at shoulder height. Press straight overhead with a braced core, then lower with control.",
        "Face Pulls" to "Pull a rope or band toward your face with elbows high, finishing with your hands beside your ears. Squeeze your rear delts.",
        "Cable Crunch" to "Kneel under the cable with the rope at your head. Crunch your torso down, bringing elbows to knees, and control the return.",
        "Barbell Bench Press" to "Lie flat with a grip slightly wider than shoulders. Lower the bar to mid-chest and press it back up.",
        "Incline Dumbbell Press" to "On an incline bench, press the dumbbells up and slightly together, then lower them with control.",
        "Tricep Pushdown" to "Keep elbows fixed at your sides. Push the rope or bar down until your arms are straight, then return slowly.",
        "Lateral Raises" to "With a slight bend in the elbows, raise the weights out to shoulder height and lower slowly.",
        "Deadlift" to "Hinge at the hips with a flat back. Grip the bar, drive through your legs and push your hips forward to stand tall.",
        "Barbell Row" to "Hinge forward with a flat back. Row the bar to your belly button, squeeze your back, then lower with control.",
        "Squat" to "Feet shoulder-width apart. Sit back and down, keep your knees tracking over your toes and your chest up, then stand tall.",
        "Leg Press" to "Place feet shoulder-width on the platform. Lower the sled until knees reach about 90 degrees, then press back up.",
        "Walking Lunges" to "Step forward into a lunge, then bring the back leg through and step again, alternating legs.",
        "Calf Raises" to "Stand on the edge of a step. Rise up onto your toes, pause at the top, then lower with control.",
        "Incline Bench Press" to "On an incline bench, lower the bar to your upper chest and press it back up.",
        "Dips" to "Support your body on bars or a bench. Bend your elbows to lower, keep your shoulders away from your ears, then press back up.",
        "Arnold Press" to "Start with palms facing you. Rotate the dumbbells outward as you press them overhead, then reverse.",
        "Skull Crushers" to "Lie back and extend the weight over your forehead. Bend your elbows to lower it toward your head, then extend back up.",
        "Front Raises" to "With straight arms, raise the weight to shoulder height in front of you, then lower slowly.",
        "Pull Ups" to "Grip the bar overhand, wider than shoulders. Pull your chest toward the bar, then lower under control.",
        "T-Bar Row" to "Hinge forward with a flat back. Row the weight to your chest, squeeze your back, then lower.",
        "Hammer Curls" to "Palms facing each other. Curl the weights up, squeeze, then lower slowly without swinging.",
        "Rear Delt Fly" to "Hinge forward with a flat back. Open your arms out and back like a reverse fly, pinching your shoulder blades.",
        "Front Squat" to "Rest the bar on your front shoulders with elbows high. Squat down keeping your chest tall, then stand.",
        "Bulgarian Split Squat" to "Rear foot elevated on a bench. Lower straight down until your front thigh is parallel to the floor, then press back up.",
        "Leg Curl" to "Lie face down on the machine. Curl your heels toward your glutes, then lower with control.",
        "Hip Thrust" to "Rest your shoulders on a bench. Drive your hips up until your body forms a straight line, squeeze your glutes at the top.",
        "Seated Calf Raise" to "Seated with weight on your knees. Rise onto your toes, pause, then lower slowly.",
        "Bench Press" to "Lie flat and grip the bar. Lower it to mid-chest, then press it up in a controlled motion.",
        "Bent Over Row" to "Hinge forward with a flat back. Row the weight to your torso, squeeze your back, then lower.",
        "Chin Ups" to "Grip the bar underhand. Pull your chin over the bar, then lower under control.",
        "Squat Jumps" to "Squat down, then explode upward into a jump. Land softly and repeat.",
        "Band Rows" to "Anchor the band and hinge forward. Row the band to your ribs, squeezing your back, then return.",
        "Glute Bridges" to "Lie on your back with knees bent. Drive your hips up squeezing your glutes, hold for a second at the top.",
        "Incline Push Ups" to "Place your hands on a raised surface. Lower your chest toward it, then press back up.",
        "Mountain Climbers" to "In a plank position, drive your knees toward your chest alternately while keeping your hips low.",
        "Band Pull Aparts" to "Hold the band in front of you at chest height. Pull it apart, squeezing your rear delts, then return.",
        "Burpees" to "Squat down, kick back to a plank, lower into a push up, jump your feet back in, then jump up.",
        "Diamond Push Ups" to "Place your hands close together under your chest to form a diamond. Lower your chest, then press back up.",
        "Superman Hold" to "Lie face down. Lift your arms and legs off the floor at the same time, hold, then lower.",
        "Band Bicep Curls" to "Stand on the band and curl the handles up with your elbows pinned at your sides. Lower slowly.",
        "One Arm Dumbbell Row" to "Brace one hand and knee on a bench. Row the dumbbell toward your hip, squeeze, then lower.",
        "Dumbbell Overhead Press" to "Press the dumbbells overhead from shoulder height with a braced core, then lower with control.",
        "Dumbbell Curls" to "Curl the dumbbells up while rotating your palms, squeeze at the top, then lower slowly.",
        "Dumbbell Shrugs" to "Let the dumbbells hang at your sides. Shrug your shoulders up toward your ears, pause, then lower.",
        "Dumbbell Romanian Deadlift" to "Hinge at the hips with soft knees, sliding the dumbbells down your thighs. Keep your back flat and stand.",
        "Dumbbell Lunges" to "Hold dumbbells at your sides. Step into a lunge, lower, then push back up through the front heel.",
        "Dumbbell Floor Press" to "Lie on the floor and press the dumbbells up. Lower until your elbows touch the floor, then press up.",
        "Bent Over Rows" to "Hinge forward with a flat back and row the weight to your torso. Squeeze your back, then lower.",
        "Dumbbell Lateral Raises" to "Raise the dumbbells out to shoulder height with slightly bent elbows, then lower slowly.",
        "Tricep Overhead Extension" to "Hold one dumbbell overhead with both hands. Lower it behind your head, then extend back up.",
        "Dumbbell Deadlift" to "Same hinge pattern as the Romanian deadlift: flat back, soft knees, drive your hips forward to stand.",
        "Step Ups" to "Step onto a bench or box. Drive through the top leg to stand tall, then step back down.",
        "Dumbbell Glute Bridge" to "Hold a dumbbell on your hips. Drive your hips up, squeeze your glutes, then lower."
    )

    fun get(name: String): ExerciseInfo {
        val normalized = name.trim()
        val image = images[normalized]?.let { "$IMG_BASE/$it/0.jpg" }
        return ExerciseInfo(
            name = normalized,
            imageUrl = image,
            videoUrl = youtubeSearch(normalized),
            instructions = instructions[normalized]
                ?: "Focus on a full range of motion. Warm up first, use a weight you can control, and stop if you feel sharp pain."
        )
    }

    private fun youtubeSearch(exercise: String): String {
        val query = URLEncoder.encode("how to do $exercise exercise", "UTF-8")
            .replace("+", "%20")
        return "https://www.youtube.com/results?search_query=$query"
    }
}
