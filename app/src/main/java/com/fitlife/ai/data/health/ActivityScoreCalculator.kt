package com.fitlife.ai.data.health

import com.fitlife.ai.data.local.entity.DailyMetricEntity

/**
 * Activity score (0-100) per the roadmap:
 * Steps 25%, Workout 25%, Sleep 20%, HRV 15%, Active minutes 15%.
 * Components with no data are excluded and remaining weights renormalized.
 */
object ActivityScoreCalculator {

    fun score(metric: DailyMetricEntity?): Int {
        if (metric == null) return 0
        var weightSum = 0.0
        var score = 0.0

        fun add(value: Int?, target: Int, weight: Double, cap: Int = target) {
            if (value != null && target > 0) {
                weightSum += weight
                score += weight * (value.toDouble() / cap).coerceIn(0.0, 1.0)
            }
        }

        add(metric.steps, 8000, 0.25)
        metric.caloriesBurned?.let { if (it > 0) add(it, 300, 0.25, 500) }
        add(metric.sleepMinutes, 420, 0.20, 480)
        add(metric.hrvAvg, 30, 0.15, 60)
        add(metric.activeMinutes, 30, 0.15, 60)

        return if (weightSum == 0.0) 0 else (score / weightSum * 100).toInt().coerceIn(0, 100)
    }

    fun stepsProgress(steps: Int?): Float =
        (steps ?: 0).toFloat() / 8000f

    fun sleepProgress(minutes: Int?): Float =
        (minutes ?: 0).toFloat() / 480f
}
