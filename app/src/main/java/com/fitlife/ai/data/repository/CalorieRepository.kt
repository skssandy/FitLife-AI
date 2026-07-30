package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.CalorieEntryDao
import com.fitlife.ai.data.local.entity.CalorieEntryEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
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
        calorieEntryDao.insert(entry)
        try {
            supabase.from("calorie_entries").insert(entry)
            calorieEntryDao.markSynced(entry.id)
        } catch (_: Exception) { }
    }

    suspend fun deleteEntry(id: Long) {
        calorieEntryDao.delete(id)
        try {
            supabase.from("calorie_entries").delete { filter { eq("id", id) } }
        } catch (_: Exception) { }
    }

    suspend fun syncUnsynced() {
        val unsynced = calorieEntryDao.getUnsyncedEntries()
        for (e in unsynced) {
            try {
                supabase.from("calorie_entries").upsert(e)
                calorieEntryDao.markSynced(e.id)
            } catch (_: Exception) { }
        }
    }
}
