package com.samuelbaldasso.financeflow.domain.repository

import com.samuelbaldasso.financeflow.core.model.budget.Budget
import com.samuelbaldasso.financeflow.core.model.budget.BudgetStatus
import com.samuelbaldasso.financeflow.core.model.money.Money
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class BudgetWithProgress(
    val budget: Budget,
    val categoryName: String,
    val spentAmount: Money,
    val status: BudgetStatus
)

interface BudgetRepository {
    fun getBudgetsForPeriodFlow(month: Int, year: Int): Flow<List<Budget>>
    fun getAllBudgetsFlow(): Flow<List<Budget>>
    fun getBudgetsWithProgressFlow(month: Int, year: Int): Flow<List<BudgetWithProgress>>
    suspend fun getBudgetForCategoryAndPeriod(categoryId: UUID, month: Int, year: Int): Budget?
    suspend fun setBudget(budget: Budget)
    suspend fun deleteBudget(id: UUID)
}
