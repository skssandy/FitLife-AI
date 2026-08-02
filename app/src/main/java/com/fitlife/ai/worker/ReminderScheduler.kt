package com.fitlife.ai.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun schedule(context: Context, intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<HydrationReminderWorker>(
            intervalHours.coerceAtLeast(1).toLong(),
            TimeUnit.HOURS
        )
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HydrationReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(HydrationReminderWorker.WORK_NAME)
    }
}
