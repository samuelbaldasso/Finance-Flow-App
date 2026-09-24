package com.samuelbaldasso.financeflow.domain.usecase.report

import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.report.CashFlowReport
import com.samuelbaldasso.financeflow.core.model.report.CategoryExpenseBreakdown
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.ZoneOffset

class GetCashFlowReportUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<CashFlowReport> {
        return combine(
            transactionRepository.getAllTransactionsFlow(),
            categoryRepository.getAllCategoriesFlow()
        ) { transactions, categories ->
            val categoriesMap = categories.associateBy { it.id }

            // Filter transactions for specified month and year (by competence date)
            val periodTransactions = transactions.filter { tx ->
                val date = tx.competenceDate.atZone(ZoneOffset.UTC)
                date.monthValue == month && date.year == year
            }

            // §3.9: Transferências entre contas próprias NÃO contam como receita/despesa
            val incomeTransactions = periodTransactions.filter { it.type == TransactionType.INCOME }
            val expenseTransactions = periodTransactions.filter { it.type == TransactionType.EXPENSE }

            val totalIncomeMinor = incomeTransactions.sumOf { it.amount.amountMinor }
            val totalExpenseMinor = expenseTransactions.sumOf { it.amount.amountMinor }
            val netSavingsMinor = totalIncomeMinor - totalExpenseMinor

            val savingsRate = if (totalIncomeMinor > 0L) {
                ((netSavingsMinor.toFloat() / totalIncomeMinor.toFloat()) * 100f).coerceIn(-100f, 100f)
            } else 0f

            // Group expenses by category
            val expensesByCategory = expenseTransactions.groupBy { it.categoryId }
            val categoryBreakdowns = expensesByCategory.map { (catId, txList) ->
                val catExpenseMinor = txList.sumOf { it.amount.amountMinor }
                val cat = catId?.let { categoriesMap[it] }
                val percentage = if (totalExpenseMinor > 0L) {
                    (catExpenseMinor.toFloat() / totalExpenseMinor.toFloat()) * 100f
                } else 0f

                CategoryExpenseBreakdown(
                    categoryId = catId,
                    categoryName = cat?.name ?: "Sem Categoria",
                    totalExpense = Money(catExpenseMinor),
                    percentageOfTotal = percentage,
                    colorHex = cat?.colorHex
                )
            }.sortedByDescending { it.totalExpense.amountMinor }

            CashFlowReport(
                month = month,
                year = year,
                totalIncome = Money(totalIncomeMinor),
                totalExpense = Money(totalExpenseMinor),
                netSavings = Money(netSavingsMinor),
                savingsRatePercentage = savingsRate,
                categoryBreakdowns = categoryBreakdowns
            )
        }
    }
}
