package com.fitlife.ai.worker

import android.Manifest
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
import com.fitlife.ai.data.repository.ReminderSettingsRepository
import com.fitlife.ai.data.repository.WaterRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class HydrationReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val waterRepository: WaterRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: ReminderSettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        createChannel(context)

        val granted = android.os.Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val settings = settingsRepository.settings.first()
        if (!settings.enabled) return Result.success()

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        if (isQuietHours(hour, settings.quietStartHour, settings.quietEndHour)) return Result.success()

        val userId = authRepository.getUserId()
        if (userId == null) return Result.success()

        val user = authRepository.getUserOnce(userId)
        val targetMl = user?.hydrationTargetMl
            ?: user?.weightKg?.let { (it * 35).toInt() }
            ?: 2500

        val todayStart = startOfDay()
        val todayEnd = todayStart + 86400000L
        val todayMl = waterRepository.getLogs(userId).first()
            .filter { it.date in todayStart until todayEnd }
            .sumOf { it.amountMl }

        if (todayMl >= targetMl) return Result.success()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to hydrate!")
            .setContentText("You've logged $todayMl ml of your $targetMl ml goal today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(REMINDER_ID, notification)
        } catch (_: SecurityException) { }

        return Result.success()
    }

    private fun isQuietHours(hour: Int, start: Int, end: Int): Boolean =
        if (start <= end) hour in start until end
        else hour >= start || hour < end

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        const val WORK_NAME = "hydration_reminders"
        private const val CHANNEL_ID = "fitlife_hydration"
        private const val REMINDER_ID = 1002

        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminders to drink water throughout the day" }
            manager.createNotificationChannel(channel)
        }
    }
}
