package com.fitlife.ai.util

import java.util.Calendar

enum class CyclePhase(
    val displayName: String,
    val description: String,
    val training: String,
    val nutrition: String
) {
    MENSTRUAL(
        "Menstrual",
        "Days 1-5. Energy may be lower. Focus on recovery.",
        "Light movement: walking, yoga, mobility. Rest is productive.",
        "Iron-rich foods, magnesium, warm cooked meals, hydrate well."
    ),
    FOLLICULAR(
        "Follicular",
        "Days 6-13. Rising estrogen. Great time for progressive overload.",
        "Best phase for strength and new PRs. Higher training volume.",
        "Higher carb tolerance. Lean protein, plenty of fiber."
    ),
    OVULATORY(
        "Ovulatory",
        "Days 14-16. Peak energy and strength.",
        "Peak intensity: power work, heavier weights, sprint intervals.",
        "Slight calorie surplus is well tolerated. Balance macros."
    ),
    LUTEAL(
        "Luteal",
        "Days 17-28. Progesterone rises; energy and mood may dip.",
        "Moderate volume, deload if fatigued. Prioritize sleep and recovery.",
        "Add ~10-15% carbs, 100-300 extra kcal, magnesium + B6, reduce salt."
    )
}

enum class SupportMode(
    val displayName: String,
    val description: String,
    val training: String,
    val nutrition: String
) {
    STANDARD(
        "Standard",
        "General cycle-based training and nutrition guidance.",
        "Follow phase-based training. Prioritize recovery in the menstrual phase.",
        "Follow phase-based nutrition. Iron-rich foods during your period."
    ),
    TTC(
        "Trying to conceive",
        "Focus on fertile-window tracking and cycle awareness.",
        "Stay active; moderate exercise supports fertility. Avoid overtraining.",
        "Adequate folate, iron and healthy fats. Limit caffeine and alcohol."
    ),
    PREGNANCY(
        "Pregnancy",
        "Prenatal-safe movement and nutrition. Cycle phases paused.",
        "Low-impact: walking, prenatal yoga, swimming. Avoid heavy lifting.",
        "Balanced meals, iron, calcium, DHA. Small frequent meals for nausea."
    ),
    POSTPARTUM(
        "Postpartum",
        "Postnatal recovery guidance, especially in the early weeks.",
        "Gradual return: pelvic floor work, walking, gentle core. Avoid high-impact early on.",
        "Nutrient-dense meals, protein for recovery, hydration for milk supply."
    ),
    PCOS(
        "PCOS",
        "Insulin-friendly nutrition with cycle support.",
        "Mix strength and cardio; consistent routine supports hormone balance.",
        "Balanced macros, fiber and protein at meals. Limit refined sugar."
    ),
    MENOPAUSE(
        "Menopause",
        "Symptom-aware guidance for hot flashes and sleep.",
        "Weight-bearing exercise and strength training support bone health.",
        "Calcium, vitamin D, soy foods. Reduce caffeine and alcohol near bedtime."
    );

    companion object {
        fun from(value: String?): SupportMode =
            entries.firstOrNull { it.name == value } ?: STANDARD
    }
}

data class NutritionAdjustment(
    val calorieDelta: Int,
    val carbBoost: Boolean,
    val proteinGuidance: String,
    val hydrationGuidance: String
)

object CycleCalculator {

    /**
     * 1-based cycle day (1 = first day of the period). Returns 0 when the
     * period anchor is missing or the anchor is in the future (not started yet).
     */
    fun cycleDay(todayMillis: Long, lastPeriodStartMillis: Long, cycleLengthDays: Int): Int {
        if (lastPeriodStartMillis <= 0) return 0
        val anchor = startOfDay(lastPeriodStartMillis)
        if (todayMillis < anchor) return 0
        val days = ((todayMillis - anchor) / DAY_MILLIS).toInt()
        val cycleLength = cycleLengthDays.coerceAtLeast(21)
        return (days % cycleLength) + 1
    }

    fun phaseForDay(day: Int): CyclePhase = when (day) {
        in 1..5 -> CyclePhase.MENSTRUAL
        in 6..13 -> CyclePhase.FOLLICULAR
        in 14..16 -> CyclePhase.OVULATORY
        else -> CyclePhase.LUTEAL
    }

    /** Most recent predicted period start on or before [dateMillis], or -1 if the anchor is after it. */
    fun periodStartForDate(dateMillis: Long, lastPeriodStartMillis: Long, cycleLengthDays: Int): Long {
        val anchor = startOfDay(lastPeriodStartMillis)
        if (dateMillis < anchor) return -1L
        val cycleLength = cycleLengthDays.coerceAtLeast(21)
        val days = ((dateMillis - anchor) / DAY_MILLIS).toInt()
        val offset = (days / cycleLength) * cycleLength
        return anchor + offset * DAY_MILLIS
    }

    /** 1..periodLength if [dateMillis] falls inside a predicted bleeding window, otherwise 0. */
    fun bleedingDay(dateMillis: Long, lastPeriodStartMillis: Long, cycleLengthDays: Int, periodLengthDays: Int): Int {
        if (lastPeriodStartMillis <= 0) return 0
        val start = periodStartForDate(dateMillis, lastPeriodStartMillis, cycleLengthDays)
        if (start < 0) return 0
        val periodLength = periodLengthDays.coerceIn(1, 14)
        val dayIndex = ((dateMillis - start) / DAY_MILLIS).toInt()
        return if (dayIndex in 0 until periodLength) dayIndex + 1 else 0
    }

    /** Expected period start for the window that should be happening right now (or the nearest future one). */
    fun currentExpectedStart(todayMillis: Long, lastPeriodStartMillis: Long, cycleLengthDays: Int): Long {
        val anchor = startOfDay(lastPeriodStartMillis)
        if (todayMillis < anchor) return anchor
        return periodStartForDate(todayMillis, lastPeriodStartMillis, cycleLengthDays)
    }

    /** Whole days a period is late; 0 if today is still inside the expected bleeding window. */
    fun daysLate(todayMillis: Long, lastPeriodStartMillis: Long, cycleLengthDays: Int, periodLengthDays: Int): Int {
        if (lastPeriodStartMillis <= 0) return 0
        val start = currentExpectedStart(todayMillis, lastPeriodStartMillis, cycleLengthDays)
        val periodLength = periodLengthDays.coerceIn(1, 14)
        val windowEnd = start + (periodLength - 1) * DAY_MILLIS
        if (todayMillis <= windowEnd) return 0
        return (((todayMillis - windowEnd) / DAY_MILLIS).toInt()).coerceAtLeast(1)
    }

    fun nextPeriodStartMillis(lastPeriodStartMillis: Long, cycleLengthDays: Int, todayMillis: Long): Long {
        val day = cycleDay(todayMillis, lastPeriodStartMillis, cycleLengthDays)
        if (day <= 0) return startOfDay(lastPeriodStartMillis)
        val cycleLength = cycleLengthDays.coerceAtLeast(21)
        val daysUntilNext = cycleLength - day + 1
        return addDays(todayMillis, daysUntilNext)
    }

    fun fertileWindow(lastPeriodStartMillis: Long, cycleLengthDays: Int): Pair<Long, Long> {
        val cycleLength = cycleLengthDays.coerceAtLeast(21)
        val ovulationDay = (cycleLength - 14).coerceAtLeast(1)
        val ovulation = addDays(lastPeriodStartMillis, ovulationDay)
        return addDays(ovulation, -5) to addDays(ovulation, 1)
    }

    fun nutritionAdjustment(phase: CyclePhase, calorieTarget: Int?): NutritionAdjustment = when (phase) {
        CyclePhase.LUTEAL -> NutritionAdjustment(
            calorieDelta = (calorieTarget ?: 2000) / 20,
            carbBoost = true,
            proteinGuidance = "Prioritize protein at every meal to curb cravings.",
            hydrationGuidance = "Add ~500ml water; reduce salt to limit bloating."
        )
        CyclePhase.MENSTRUAL -> NutritionAdjustment(
            calorieDelta = 0,
            carbBoost = false,
            proteinGuidance = "Iron-rich protein: lean beef, spinach, lentils.",
            hydrationGuidance = "Hydrate well; magnesium-rich foods can ease cramps."
        )
        CyclePhase.FOLLICULAR -> NutritionAdjustment(
            calorieDelta = 0,
            carbBoost = false,
            proteinGuidance = "Lean protein supports muscle as energy rises.",
            hydrationGuidance = "Keep baseline hydration."
        )
        CyclePhase.OVULATORY -> NutritionAdjustment(
            calorieDelta = 0,
            carbBoost = false,
            proteinGuidance = "Balanced macros to fuel peak training.",
            hydrationGuidance = "Maintain hydration around workouts."
        )
    }

    fun isInFertileWindow(todayMillis: Long, lastPeriodStartMillis: Long, cycleLengthDays: Int): Boolean {
        val (start, end) = fertileWindow(lastPeriodStartMillis, cycleLengthDays)
        return todayMillis in start..end
    }

    private fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private const val DAY_MILLIS = 86_400_000L
}
