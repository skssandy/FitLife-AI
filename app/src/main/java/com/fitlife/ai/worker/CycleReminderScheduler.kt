package com.fitlife.ai.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CycleReminderScheduler {

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<CycleReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(CycleReminderWorker.nextReminderDelayMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CycleReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CycleReminderWorker.WORK_NAME)
    }
}
