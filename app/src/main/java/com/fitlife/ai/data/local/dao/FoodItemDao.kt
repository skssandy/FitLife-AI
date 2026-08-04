package com.fitlife.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitlife.ai.data.local.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items")
    fun getAll(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items")
    suspend fun getAllOnce(): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT 30")
    fun search(query: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): FoodItemEntity?

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun count(): Int

    @Query("SELECT name FROM food_items")
    suspend fun findNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<FoodItemEntity>)
}
