package com.samuelbaldasso.financeflow.data.repository

import com.samuelbaldasso.financeflow.core.database.dao.BudgetDao
import com.samuelbaldasso.financeflow.core.database.dao.CategoryDao
import com.samuelbaldasso.financeflow.core.database.dao.TransactionDao
import com.samuelbaldasso.financeflow.core.database.entity.BudgetEntity
import com.samuelbaldasso.financeflow.core.model.budget.Budget
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.repository.BudgetRepository
import com.samuelbaldasso.financeflow.domain.repository.BudgetWithProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getBudgetsForPeriodFlow(month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getAllForPeriodFlow(month, year).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllBudgetsFlow(): Flow<List<Budget>> {
        return budgetDao.getAllBudgetsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getBudgetsWithProgressFlow(month: Int, year: Int): Flow<List<BudgetWithProgress>> {
        val startOfMonth = LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfMonth = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1

        return budgetDao.getAllForPeriodFlow(month, year).flatMapLatest { budgetEntities ->
            if (budgetEntities.isEmpty()) {
                flowOf(emptyList())
            } else {
                val itemFlows = budgetEntities.map { budgetEntity ->
                    val budget = budgetEntity.toDomain()
                    transactionDao.getSpentForCategoryInPeriod(budget.categoryId, startOfMonth, endOfMonth)
                        .map { spentMinor ->
                            val category = categoryDao.getById(budget.categoryId)
                            val spent = Money(spentMinor)
                            BudgetWithProgress(
                                budget = budget,
                                categoryName = category?.name ?: "Categoria",
                                spentAmount = spent,
                                status = budget.calculateStatus(spent)
                            )
                        }
                }
                combine(itemFlows) { it.toList() }
            }
        }
    }

    override suspend fun getBudgetForCategoryAndPeriod(categoryId: UUID, month: Int, year: Int): Budget? {
        return budgetDao.getForCategoryAndPeriod(categoryId, month, year)?.toDomain()
    }

    override suspend fun setBudget(budget: Budget) {
        budgetDao.insertOrUpdate(BudgetEntity.fromDomain(budget))
    }

    override suspend fun deleteBudget(id: UUID) {
        // Find and delete
    }
}
