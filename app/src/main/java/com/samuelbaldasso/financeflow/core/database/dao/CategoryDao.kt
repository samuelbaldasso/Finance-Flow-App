package com.samuelbaldasso.financeflow.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.samuelbaldasso.financeflow.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    abstract suspend fun update(category: CategoryEntity)

    @Delete
    abstract suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    abstract suspend fun getById(id: UUID): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY name ASC")
    abstract fun getAllFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    abstract suspend fun getTransactionUsageCount(categoryId: UUID): Int

    @Query("UPDATE transactions SET category_id = :newCategoryId WHERE category_id = :oldCategoryId")
    abstract suspend fun reassignTransactionsCategory(oldCategoryId: UUID, newCategoryId: UUID): Int

    @Transaction
    open suspend fun deleteWithReassignment(category: CategoryEntity, newCategoryId: UUID) {
        reassignTransactionsCategory(category.id, newCategoryId)
        delete(category)
    }
}
