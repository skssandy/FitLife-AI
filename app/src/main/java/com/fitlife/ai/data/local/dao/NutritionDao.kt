package com.fitlife.ai.data.local.dao

import androidx.room.*
import com.fitlife.ai.data.local.entity.NutritionLogEntity
import com.fitlife.ai.data.local.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId AND date = :date ORDER BY createdAt")
    fun getLogsForDate(userId: String, date: String): Flow<List<NutritionLogEntity>>

    @Query("SELECT SUM(calories) FROM nutrition_logs WHERE userId = :userId AND date = :date")
    fun getTotalCalories(userId: String, date: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: NutritionLogEntity)

    @Delete
    suspend fun delete(log: NutritionLogEntity)

    @Query("DELETE FROM nutrition_logs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs WHERE userId = :userId AND date = :date ORDER BY timestamp")
    fun getLogsForDate(userId: String, date: String): Flow<List<WaterLogEntity>>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE userId = :userId AND date = :date")
    fun getTotalMl(userId: String, date: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: WaterLogEntity)

    @Delete
    suspend fun delete(log: WaterLogEntity)
}
