package com.samuelbaldasso.financeflow.domain.repository

import com.samuelbaldasso.financeflow.core.model.category.Category
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface CategoryRepository {
    fun getAllCategoriesFlow(): Flow<List<Category>>
    suspend fun getCategoryById(id: UUID): Category?
    suspend fun createCategory(category: Category): Category
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: UUID, reassignToCategoryId: UUID)
    suspend fun getTransactionUsageCount(categoryId: UUID): Int
    suspend fun seedDefaultCategoriesIfNeeded()
}
