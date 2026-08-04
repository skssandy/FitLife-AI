package com.fitlife.ai.data.repository

import com.fitlife.ai.data.FoodSeedData
import com.fitlife.ai.data.local.dao.FoodItemDao
import com.fitlife.ai.data.local.entity.FoodItemEntity
import com.fitlife.ai.util.FoodDiet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodItemDao: FoodItemDao
) {
    suspend fun seedIfEmpty() {
        if (foodItemDao.count() == 0) {
            foodItemDao.insertAll(FoodSeedData.foods)
            return
        }
        val existing = foodItemDao.findNames().map { it.lowercase() }.toSet()
        val missing = FoodSeedData.foods.filterNot { it.name.lowercase() in existing }
        if (missing.isNotEmpty()) {
            foodItemDao.insertAll(missing)
        }
        FoodSeedData.foods.forEach { food ->
            foodItemDao.updateDietType(food.name, food.dietType)
        }
    }

    fun search(query: String, dietType: String?): Flow<List<FoodItemEntity>> =
        foodItemDao.search(query.trim()).map { foods ->
            foods.filter { FoodDiet.allowedByDiet(it.dietType, dietType) }
        }

    suspend fun allOnce(): List<FoodItemEntity> = foodItemDao.getAllOnce()

    suspend fun findByBarcode(barcode: String): FoodItemEntity? = foodItemDao.findByBarcode(barcode)
}
