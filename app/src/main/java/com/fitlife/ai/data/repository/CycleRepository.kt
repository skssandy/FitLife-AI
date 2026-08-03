package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.CycleEntryDao
import com.fitlife.ai.data.local.dao.SymptomLogDao
import com.fitlife.ai.data.local.entity.CycleEntryEntity
import com.fitlife.ai.data.local.entity.SymptomLogEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CycleRepository @Inject constructor(
    private val cycleEntryDao: CycleEntryDao,
    private val symptomLogDao: SymptomLogDao,
    private val supabase: SupabaseClient
) {
    fun getEntries(userId: String): Flow<List<CycleEntryEntity>> = cycleEntryDao.getEntries(userId)

    suspend fun getEntriesOnce(userId: String): List<CycleEntryEntity> =
        cycleEntryDao.getEntriesOnce(userId)

    fun getSymptomLogs(userId: String): Flow<List<SymptomLogEntity>> = symptomLogDao.getLogs(userId)

    suspend fun getSymptomsForDay(userId: String, date: Long): SymptomLogEntity? =
        symptomLogDao.getForDay(userId, date)

    suspend fun upsertEntry(entry: CycleEntryEntity) {
        val id = cycleEntryDao.upsert(entry)
        val toSync = entry.copy(id = id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("cycle_entries").upsert(toSync)
                cycleEntryDao.markSynced(id)
            }
        } catch (_: Exception) { }
    }

    suspend fun upsertSymptomLog(log: SymptomLogEntity) {
        val id = symptomLogDao.upsert(log)
        val toSync = log.copy(id = id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("symptom_logs").upsert(toSync)
                symptomLogDao.markSynced(id)
            }
        } catch (_: Exception) { }
    }

    suspend fun syncUnsynced() {
        for (entry in cycleEntryDao.getUnsynced()) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("cycle_entries").upsert(entry)
                    cycleEntryDao.markSynced(entry.id)
                }
            } catch (_: Exception) { }
        }
        for (log in symptomLogDao.getUnsynced()) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("symptom_logs").upsert(log)
                    symptomLogDao.markSynced(log.id)
                }
            } catch (_: Exception) { }
        }
    }

    suspend fun pullFromServer(userId: String) {
        try {
            val remoteEntries = withContext(Dispatchers.IO) {
                supabase.from("cycle_entries")
                    .select { filter { eq("userId", userId) } }
                    .decodeList<CycleEntryEntity>()
            }
            val toInsert = remoteEntries.filter { e ->
                val local = cycleEntryDao.getById(e.id)
                local == null || local.synced
            }.map { it.copy(synced = true) }
            if (toInsert.isNotEmpty()) cycleEntryDao.upsertAll(toInsert)

            val remoteLogs = withContext(Dispatchers.IO) {
                supabase.from("symptom_logs")
                    .select { filter { eq("userId", userId) } }
                    .decodeList<SymptomLogEntity>()
            }
            val logsToInsert = remoteLogs.filter { l ->
                val local = symptomLogDao.getById(l.id)
                local == null || local.synced
            }.map { it.copy(synced = true) }
            if (logsToInsert.isNotEmpty()) symptomLogDao.upsertAll(logsToInsert)
        } catch (_: Exception) { }
    }
}
