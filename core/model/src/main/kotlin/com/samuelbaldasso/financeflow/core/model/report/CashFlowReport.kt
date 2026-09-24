package com.samuelbaldasso.financeflow.core.model.report

import com.samuelbaldasso.financeflow.core.model.money.Money
import java.util.UUID

data class CategoryExpenseBreakdown(
    val categoryId: UUID?,
    val categoryName: String,
    val totalExpense: Money,
    val percentageOfTotal: Float,
    val colorHex: String? = null
)

data class CashFlowReport(
    val month: Int,
    val year: Int,
    val totalIncome: Money,
    val totalExpense: Money,
    val netSavings: Money,
    val savingsRatePercentage: Float,
    val categoryBreakdowns: List<CategoryExpenseBreakdown>
) {
    val isPositiveCashFlow: Boolean get() = totalIncome >= totalExpense
}
