package com.fitlife.ai.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitlife.ai.R
import com.fitlife.ai.data.repository.AuthRepository
import com.fitlife.ai.data.repository.CycleRepository
import com.fitlife.ai.util.CycleCalculator
import com.fitlife.ai.util.SupportMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class CycleReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val cycleRepository: CycleRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        createChannel(context)

        val granted = android.os.Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val message = try {
            reminderMessage()
        } catch (_: Exception) {
            null
        } ?: return Result.success()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FitLife Cycle")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(REMINDER_ID, notification)
        } catch (_: SecurityException) { }

        return Result.success()
    }

    private suspend fun reminderMessage(): String? {
        val userId = authRepository.getUserId() ?: return null
        val profile = authRepository.getUserOnce(userId) ?: return null
        val lastPeriod = profile.lastPeriodStart ?: return null
        if (lastPeriod <= 0L) return null
        if (SupportMode.from(profile.supportMode) in listOf(
                SupportMode.PREGNANCY, SupportMode.POSTPARTUM, SupportMode.MENOPAUSE
            )) return null
        if (!profile.birthControl.isNullOrBlank()) return null

        val cycleLength = (profile.cycleLength ?: 28).coerceAtLeast(21)
        val entries = cycleRepository.getEntriesOnce(userId)
        val periodLength = entries.maxByOrNull { it.startDate }
            ?.durationDays?.coerceIn(1, 14) ?: 5
        val today = Calendar.getInstance()
        val todayStart = startOfDay(today.timeInMillis)
        val tomorrow = todayStart + DAY_MILLIS

        val expected = CycleCalculator.currentExpectedStart(todayStart, lastPeriod, cycleLength)
        val late = CycleCalculator.daysLate(todayStart, lastPeriod, cycleLength, periodLength)
        val inWindow = CycleCalculator.inCurrentExpectedWindow(
            todayStart, lastPeriod, cycleLength, periodLength
        ) > 0

        if (late > 0) {
            return "Your period is $late ${if (late == 1) "day" else "days"} late. Log it when it starts."
        }
        if (inWindow) {
            val day = CycleCalculator.inCurrentExpectedWindow(
                todayStart, lastPeriod, cycleLength, periodLength
            )
            return "Expected period window · Day $day. Log your period when it starts."
        }
        if (tomorrow == expected) {
            return "Your period may start tomorrow (${shortDate(expected)})."
        }
        if (todayStart == expected) {
            return "Your period is expected to start today. Log it when it begins."
        }

        val fertile = CycleCalculator.fertileWindow(lastPeriod, cycleLength)
        if (tomorrow == startOfDay(fertile.first)) {
            return "Your fertile window begins tomorrow (${shortDate(fertile.first)})."
        }
        if (todayStart == startOfDay(fertile.first)) {
            return "Your fertile window starts today (${shortDate(fertile.second)})."
        }
        return null
    }

    companion object {
        const val WORK_NAME = "cycle_reminder"
        private const val CHANNEL_ID = "fitlife_cycle"
        private const val REMINDER_ID = 1002
        private const val DAY_MILLIS = 86_400_000L

        /** Delay in ms until the next 8 AM occurrence. */
        fun nextReminderDelayMillis(): Long {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance()
            next.set(Calendar.HOUR_OF_DAY, 8)
            next.set(Calendar.MINUTE, 0)
            next.set(Calendar.SECOND, 0)
            next.set(Calendar.MILLISECOND, 0)
            if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1)
            return next.timeInMillis - now.timeInMillis
        }

        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cycle Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Reminders for period and fertile window predictions" }
            manager.createNotificationChannel(channel)
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

        private fun shortDate(millis: Long): String =
            java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                .format(java.util.Date(millis))
    }
}
