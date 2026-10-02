package com.samuelbaldasso.financeflow.data.repository

import com.samuelbaldasso.financeflow.core.database.dao.CategoryDao
import com.samuelbaldasso.financeflow.core.database.entity.CategoryEntity
import com.samuelbaldasso.financeflow.core.model.audit.AuditAction
import com.samuelbaldasso.financeflow.core.model.audit.AuditEvent
import com.samuelbaldasso.financeflow.core.model.category.Category
import com.samuelbaldasso.financeflow.core.model.category.CategoryType
import com.samuelbaldasso.financeflow.domain.repository.AuditRepository
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import androidx.room.withTransaction
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import java.util.UUID

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val auditRepository: AuditRepository,
    private val database: FinanceFlowDatabase
) : CategoryRepository {

    override fun getAllCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoryById(id: UUID): Category? {
        return categoryDao.getById(id)?.toDomain()
    }

    override suspend fun createCategory(category: Category): Category = database.withTransaction {
        categoryDao.insert(CategoryEntity.fromDomain(category))
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "CATEGORY",
                entityId = category.id,
                action = AuditAction.CREATE,
                afterState = "name=${category.name}, type=${category.type}, parent=${category.parentCategoryId}"
            )
        )
        return@withTransaction category
    }

    override suspend fun updateCategory(category: Category) = database.withTransaction {
        val before = categoryDao.getById(category.id)
        categoryDao.update(CategoryEntity.fromDomain(category))
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "CATEGORY",
                entityId = category.id,
                action = AuditAction.UPDATE,
                beforeState = before?.let { "name=${it.name}" },
                afterState = "name=${category.name}"
            )
        )
    }

    override suspend fun deleteCategory(id: UUID, reassignToCategoryId: UUID) = database.withTransaction {
        val category = categoryDao.getById(id) ?: return@withTransaction
        category.toDomain().validateDeletionAllowed()

        categoryDao.deleteWithReassignment(category, reassignToCategoryId)
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "CATEGORY",
                entityId = id,
                action = AuditAction.DELETE,
                beforeState = "name=${category.name}",
                afterState = "reassignedTo=$reassignToCategoryId"
            )
        )
    }

    override suspend fun getTransactionUsageCount(categoryId: UUID): Int = database.withTransaction {
        return@withTransaction categoryDao.getTransactionUsageCount(categoryId)
    }

    override suspend fun seedDefaultCategoriesIfNeeded() = database.withTransaction {
        val existing = categoryDao.getAllFlow().first()
        if (existing.isEmpty()) {
            val defaults = listOf(
                Category(name = "Salário", type = CategoryType.INCOME, isSystem = true, iconKey = "payments"),
                Category(name = "Rendimentos", type = CategoryType.INCOME, isSystem = true, iconKey = "trending_up"),
                Category(name = "Alimentação", type = CategoryType.EXPENSE, isSystem = true, iconKey = "restaurant"),
                Category(name = "Moradia", type = CategoryType.EXPENSE, isSystem = true, iconKey = "home"),
                Category(name = "Transporte", type = CategoryType.EXPENSE, isSystem = true, iconKey = "directions_car"),
                Category(name = "Saúde", type = CategoryType.EXPENSE, isSystem = true, iconKey = "local_hospital"),
                Category(name = "Lazer", type = CategoryType.EXPENSE, isSystem = true, iconKey = "sports_esports"),
                Category(name = "Educação", type = CategoryType.EXPENSE, isSystem = true, iconKey = "school"),
                Category(name = "Outros", type = CategoryType.EXPENSE, isSystem = true, iconKey = "category")
            )
            categoryDao.insertAll(defaults.map { CategoryEntity.fromDomain(it) })
        }
    }
}
