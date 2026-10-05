package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategoryByName(name: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("UPDATE categories SET useForPersonalBudget = 0")
    suspend fun resetPersonalBudgetCategory()

    @Query("UPDATE categories SET useForPersonalBudget = 1 WHERE name = :name")
    suspend fun setPersonalBudgetCategory(name: String)
}
