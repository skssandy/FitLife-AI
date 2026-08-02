package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.WaterLogDao
import com.fitlife.ai.data.local.entity.WaterLogEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val waterLogDao: WaterLogDao,
    private val supabase: SupabaseClient
) {
    fun getLogs(userId: String): Flow<List<WaterLogEntity>> = waterLogDao.getLogs(userId)

    fun getLogsInRange(userId: String, start: Long, end: Long): Flow<List<WaterLogEntity>> =
        waterLogDao.getLogsInRange(userId, start, end)

    suspend fun addLog(userId: String, amountMl: Int) {
        val log = WaterLogEntity(userId = userId, amountMl = amountMl, date = System.currentTimeMillis())
        val id = waterLogDao.insert(log)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("water_logs").upsert(log.copy(id = id))
                waterLogDao.markSynced(id)
            }
        } catch (_: Exception) { }
    }

    suspend fun deleteLog(id: Long) {
        waterLogDao.delete(id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("water_logs").delete { filter { eq("id", id) } }
            }
        } catch (_: Exception) { }
    }

    suspend fun syncUnsynced() {
        for (log in waterLogDao.getUnsynced()) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("water_logs").upsert(log)
                    waterLogDao.markSynced(log.id)
                }
            } catch (_: Exception) { }
        }
    }

    suspend fun pullFromServer(userId: String) {
        try {
            val remote = withContext(Dispatchers.IO) {
                supabase.from("water_logs")
                    .select { filter { eq("userId", userId) } }
                    .decodeList<WaterLogEntity>()
            }
            val unsyncedIds = waterLogDao.getUnsynced().map { it.id }.toSet()
            val toInsert = remote.filter { it.id !in unsyncedIds }.map { it.copy(synced = true) }
            if (toInsert.isNotEmpty()) waterLogDao.upsertAll(toInsert)
        } catch (_: Exception) { }
    }
}
