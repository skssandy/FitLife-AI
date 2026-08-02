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
}
