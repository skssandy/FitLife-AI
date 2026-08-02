package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.DailyMetricDao
import com.fitlife.ai.data.local.entity.DailyMetricEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyMetricRepository @Inject constructor(
    private val dailyMetricDao: DailyMetricDao,
    private val supabase: SupabaseClient
) {
    fun getMetrics(userId: String): Flow<List<DailyMetricEntity>> = dailyMetricDao.getMetrics(userId)

    suspend fun getForDay(userId: String, date: Long): DailyMetricEntity? =
        dailyMetricDao.getForDay(userId, date)

    suspend fun upsert(metric: DailyMetricEntity) {
        dailyMetricDao.upsert(metric)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("daily_metrics").upsert(metric)
                dailyMetricDao.markSynced(metric.id)
            }
        } catch (_: Exception) { }
    }

    suspend fun syncUnsynced() {
        for (metric in dailyMetricDao.getUnsynced()) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("daily_metrics").upsert(metric)
                    dailyMetricDao.markSynced(metric.id)
                }
            } catch (_: Exception) { }
        }
    }

    suspend fun pullFromServer(userId: String) {
        try {
            val remote = withContext(Dispatchers.IO) {
                supabase.from("daily_metrics")
                    .select { filter { eq("userId", userId) } }
                    .decodeList<DailyMetricEntity>()
            }
            val local = dailyMetricDao.getUnsynced().map { it.date }.toSet()
            val toInsert = remote.filter { it.date !in local }
            if (toInsert.isNotEmpty()) dailyMetricDao.upsertAll(toInsert)
        } catch (_: Exception) { }
    }
}
