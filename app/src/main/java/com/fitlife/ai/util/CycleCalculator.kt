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

object CycleCalculator {

    fun cycleDay(todayMillis: Long, lastPeriodStartMillis: Long, cycleLengthDays: Int): Int {
        if (lastPeriodStartMillis <= 0) return 0
        val days = ((todayMillis - lastPeriodStartMillis) / DAY_MILLIS).toInt()
        return ((days % cycleLengthDays) + 1).coerceAtLeast(1)
    }

    fun phaseForDay(day: Int): CyclePhase = when (day) {
        in 1..5 -> CyclePhase.MENSTRUAL
        in 6..13 -> CyclePhase.FOLLICULAR
        in 14..16 -> CyclePhase.OVULATORY
        else -> CyclePhase.LUTEAL
    }

    fun nextPeriodStartMillis(lastPeriodStartMillis: Long, cycleLengthDays: Int, todayMillis: Long): Long {
        val day = cycleDay(todayMillis, lastPeriodStartMillis, cycleLengthDays)
        val daysUntilNext = cycleLengthDays - day + 1
        val cal = Calendar.getInstance()
        cal.timeInMillis = todayMillis
        cal.add(Calendar.DAY_OF_YEAR, daysUntilNext)
        return cal.timeInMillis
    }

    private const val DAY_MILLIS = 86_400_000L
}
