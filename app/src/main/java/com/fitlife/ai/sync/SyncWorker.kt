package com.fitlife.ai.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.fitlife.ai.data.local.dao.SyncDao
import com.fitlife.ai.data.local.entity.SyncQueueEntity
import com.fitlife.ai.data.remote.SupabaseConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncDao: SyncDao,
    private val supabase: SupabaseClient
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "fitlife_sync"
        const val TABLE_SYNC_MAP = "sync_queue"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun enqueueOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val pendingItems = syncDao.getAllPending()
            if (pendingItems.isEmpty()) return Result.success()

            var successCount = 0
            for (item in pendingItems) {
                try {
                    syncEntity(item)
                    syncDao.markSynced(item.id)
                    successCount++
                } catch (_: Exception) {
                    // Will retry on next sync
                }
            }

            syncDao.deleteSynced()

            if (successCount == pendingItems.size) Result.success()
            else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncEntity(item: SyncQueueEntity) {
        when (item.entityType) {
            "user_profile" -> supabase.from("user_profiles").upsert(item.payload)
            "workout_session" -> supabase.from("workout_sessions").upsert(item.payload)
            "nutrition_log" -> supabase.from("nutrition_logs").upsert(item.payload)
            "blood_analysis" -> supabase.from("blood_analyses").upsert(item.payload)
            "cycle_entry" -> supabase.from("cycle_entries").upsert(item.payload)
            "progress_entry" -> supabase.from("progress_entries").upsert(item.payload)
        }
    }
}
