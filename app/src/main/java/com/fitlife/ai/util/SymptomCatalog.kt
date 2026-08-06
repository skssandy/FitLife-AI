package com.fitlife.ai.util

data class Symptom(
    val id: String,
    val name: String,
    val category: String
)

object SymptomCatalog {

    val all: List<Symptom> = listOf(
        // Physical
        Symptom("cramps", "Cramps", "Physical"),
        Symptom("bloating", "Bloating", "Physical"),
        Symptom("breast_tenderness", "Breast tenderness", "Physical"),
        Symptom("breast_swelling", "Breast swelling", "Physical"),
        Symptom("backache", "Backache", "Physical"),
        Symptom("headache", "Headache", "Physical"),
        Symptom("fatigue", "Fatigue", "Physical"),
        Symptom("nausea", "Nausea", "Physical"),
        Symptom("acne", "Acne breakouts", "Physical"),
        Symptom("oily_skin", "Oily skin", "Physical"),
        Symptom("water_retention", "Water retention", "Physical"),
        Symptom("muscle_aches", "Muscle aches", "Physical"),
        Symptom("joint_pain", "Joint pain", "Physical"),
        Symptom("dizziness", "Dizziness", "Physical"),
        Symptom("hot_flashes", "Hot flashes", "Physical"),
        Symptom("night_sweats", "Night sweats", "Physical"),
        Symptom("cold_sensitivity", "Feeling cold", "Physical"),
        Symptom("cravings", "Food cravings", "Physical"),
        Symptom("increased_appetite", "Increased appetite", "Physical"),
        Symptom("appetite_loss", "Loss of appetite", "Physical"),

        // Mood & Energy
        Symptom("low_energy", "Low energy", "Mood & Energy"),
        Symptom("mood_swings", "Mood swings", "Mood & Energy"),
        Symptom("irritability", "Irritability", "Mood & Energy"),
        Symptom("anxiety", "Anxiety", "Mood & Energy"),
        Symptom("low_mood", "Low mood / sadness", "Mood & Energy"),
        Symptom("crying_spells", "Crying spells", "Mood & Energy"),
        Symptom("rejection_sensitivity", "Rejection sensitivity", "Mood & Energy"),
        Symptom("brain_fog", "Brain fog", "Mood & Energy"),
        Symptom("difficulty_focusing", "Difficulty focusing", "Mood & Energy"),
        Symptom("restlessness", "Restlessness", "Mood & Energy"),
        Symptom("decreased_libido", "Decreased libido", "Mood & Energy"),
        Symptom("increased_libido", "Increased libido", "Mood & Energy"),

        // Sleep
        Symptom("insomnia", "Trouble sleeping", "Sleep"),
        Symptom("vivid_dreams", "Vivid dreams", "Sleep"),
        Symptom("waking_tired", "Waking up tired", "Sleep"),
        Symptom("restless_sleep", "Restless sleep", "Sleep"),
        Symptom("oversleeping", "Sleeping too much", "Sleep"),

        // Digestive
        Symptom("constipation", "Constipation", "Digestive"),
        Symptom("diarrhea", "Diarrhea", "Digestive"),
        Symptom("gas", "Gas", "Digestive"),
        Symptom("heartburn", "Heartburn", "Digestive"),

        // Menstrual
        Symptom("normal_flow", "Normal flow", "Menstrual"),
        Symptom("heavy_flow", "Heavy flow", "Menstrual"),
        Symptom("light_flow", "Light flow", "Menstrual"),
        Symptom("spotting", "Spotting", "Menstrual"),
        Symptom("clots", "Clots", "Menstrual"),
        Symptom("prolonged_bleeding", "Prolonged bleeding", "Menstrual"),
        Symptom("missed_period", "Missed period", "Menstrual"),
        Symptom("irregular_period", "Irregular period", "Menstrual")
    )

    val categories: List<String> = listOf(
        "Physical", "Mood & Energy", "Sleep", "Digestive", "Menstrual"
    )

    fun byCategory(category: String): List<Symptom> = all.filter { it.category == category }

    fun nameForId(id: String): String? = all.firstOrNull { it.id == id }?.name
}
