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
        cycleEntryDao.upsert(entry)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("cycle_entries").upsert(entry)
                cycleEntryDao.markSynced(entry.id)
            }
        } catch (_: Exception) { }
    }

    suspend fun upsertSymptomLog(log: SymptomLogEntity) {
        symptomLogDao.upsert(log)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("symptom_logs").upsert(log)
                symptomLogDao.markSynced(log.id)
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
            val localEntries = cycleEntryDao.getUnsynced().map { it.startDate }.toSet()
            val toInsert = remoteEntries.filter { it.startDate !in localEntries }
            if (toInsert.isNotEmpty()) cycleEntryDao.upsertAll(toInsert)

            val remoteLogs = withContext(Dispatchers.IO) {
                supabase.from("symptom_logs")
                    .select { filter { eq("userId", userId) } }
                    .decodeList<SymptomLogEntity>()
            }
            val localLogs = symptomLogDao.getUnsynced().map { it.date }.toSet()
            val logsToInsert = remoteLogs.filter { it.date !in localLogs }
            if (logsToInsert.isNotEmpty()) symptomLogDao.upsertAll(logsToInsert)
        } catch (_: Exception) { }
    }
}
