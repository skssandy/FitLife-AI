package com.fitlife.ai.data.repository

import com.fitlife.ai.data.FoodSeedData
import com.fitlife.ai.data.local.dao.FoodItemDao
import com.fitlife.ai.data.local.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodItemDao: FoodItemDao
) {
    suspend fun seedIfEmpty() {
        if (foodItemDao.count() == 0) {
            foodItemDao.insertAll(FoodSeedData.foods)
        }
    }

    fun search(query: String): Flow<List<FoodItemEntity>> = foodItemDao.search(query.trim())

    suspend fun allOnce(): List<FoodItemEntity> = foodItemDao.getAllOnce()

    suspend fun findByBarcode(barcode: String): FoodItemEntity? = foodItemDao.findByBarcode(barcode)
}
