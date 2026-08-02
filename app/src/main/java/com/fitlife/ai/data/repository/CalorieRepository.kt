package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.CalorieEntryDao
import com.fitlife.ai.data.local.entity.CalorieEntryEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalorieRepository @Inject constructor(
    private val calorieEntryDao: CalorieEntryDao,
    private val supabase: SupabaseClient
) {
    fun getEntries(userId: String): Flow<List<CalorieEntryEntity>> =
        calorieEntryDao.getEntries(userId)

    fun getEntriesInRange(userId: String, start: Long, end: Long): Flow<List<CalorieEntryEntity>> =
        calorieEntryDao.getEntriesInRange(userId, start, end)

    suspend fun addEntry(entry: CalorieEntryEntity) {
        val id = calorieEntryDao.insert(entry)
        val toSync = entry.copy(id = id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("calorie_entries").upsert(toSync)
                calorieEntryDao.markSynced(id)
            }
        } catch (_: Exception) { }
    }

    suspend fun deleteEntry(id: Long) {
        calorieEntryDao.delete(id)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("calorie_entries").delete { filter { eq("id", id) } }
            }
        } catch (_: Exception) { }
    }

    suspend fun updateEntry(entry: CalorieEntryEntity) {
        val updated = entry.copy(synced = false)
        calorieEntryDao.upsertAll(listOf(updated))
        try {
            withContext(Dispatchers.IO) {
                supabase.from("calorie_entries").upsert(updated)
                calorieEntryDao.markSynced(updated.id)
            }
        } catch (_: Exception) { }
    }

    suspend fun syncUnsynced() {
        val unsynced = calorieEntryDao.getUnsyncedEntries()
        for (e in unsynced) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("calorie_entries").upsert(e)
                    calorieEntryDao.markSynced(e.id)
                }
            } catch (_: Exception) { }
        }
    }

    suspend fun pullFromServer(userId: String) {
        try {
            val remote = withContext(Dispatchers.IO) {
                supabase.from("calorie_entries")
                    .select { filter { eq("userId", userId) } }
                    .decodeList<CalorieEntryEntity>()
            }
            val toInsert = remote.filter { e ->
                val local = calorieEntryDao.getById(e.id)
                local == null || local.synced
            }.map { it.copy(synced = true) }
            if (toInsert.isNotEmpty()) calorieEntryDao.upsertAll(toInsert)
        } catch (_: Exception) { }
    }
}
