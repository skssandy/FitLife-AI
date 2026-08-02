package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.BloodReportDao
import com.fitlife.ai.data.local.entity.BloodReportEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloodReportRepository @Inject constructor(
    private val bloodReportDao: BloodReportDao,
    private val supabase: SupabaseClient
) {
    fun getReports(userId: String): Flow<List<BloodReportEntity>> = bloodReportDao.getReports(userId)

    suspend fun getReportOnce(id: Long): BloodReportEntity? = bloodReportDao.getById(id)

    suspend fun addReport(report: BloodReportEntity): Long {
        val id = bloodReportDao.insert(report)
        val toSync = report.copy(id = id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("blood_reports").upsert(toSync)
                bloodReportDao.markSynced(id)
            }
        } catch (_: Exception) { }
        return id
    }

    suspend fun updateAnalysis(id: Long, analysisText: String) {
        val report = bloodReportDao.getById(id) ?: return
        val updated = report.copy(analysisText = analysisText, synced = false)
        bloodReportDao.update(updated)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("blood_reports").upsert(updated)
                bloodReportDao.markSynced(id)
            }
        } catch (_: Exception) { }
    }

    suspend fun deleteReport(id: Long) {
        bloodReportDao.delete(id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("blood_reports").delete { filter { eq("id", id) } }
            }
        } catch (_: Exception) { }
    }

    suspend fun syncUnsynced() {
        val unsynced = bloodReportDao.getUnsynced()
        for (r in unsynced) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("blood_reports").upsert(r)
                    bloodReportDao.markSynced(r.id)
                }
            } catch (_: Exception) { }
        }
    }

    suspend fun pullFromServer(userId: String) {
        try {
            val remote = withContext(Dispatchers.IO) {
                supabase.from("blood_reports")
                    .select { filter { eq("userId", userId) } }
                    .decodeList<BloodReportEntity>()
            }
            val toInsert = remote.filter { r ->
                val local = bloodReportDao.getById(r.id)
                local == null || local.synced
            }.map { it.copy(synced = true) }
            if (toInsert.isNotEmpty()) {
                bloodReportDao.upsertAll(toInsert)
            }
        } catch (_: Exception) { }
    }
}
