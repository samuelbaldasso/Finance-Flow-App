package com.samuelbaldasso.financeflow.domain.usecase.security

import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.AuditRepository
import com.samuelbaldasso.financeflow.domain.repository.BudgetRepository
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import com.samuelbaldasso.financeflow.domain.repository.GoalRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

class ExportAllUserDataUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(): String {
        val root = JSONObject()
        root.put("app", "FinanceFlow")
        root.put("version", "1.0.0-MVP")
        root.put("exportTimestamp", Instant.now().toString())
        root.put("compliance", "LGPD Art. 18 - Direito à portabilidade de dados")

        // 1. Accounts
        val accounts = accountRepository.getAllAccountsFlow().first()
        val accountsArray = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id.toString())
                put("name", acc.name)
                put("type", acc.type.name)
                put("currency", acc.currency.name)
                put("initialBalanceMinor", acc.initialBalance.amountMinor)
                put("isArchived", acc.isArchived)
                put("closingDay", acc.closingDay)
                put("dueDay", acc.dueDay)
                put("creditLimitMinor", acc.creditLimit?.amountMinor)
            }
            accountsArray.put(obj)
        }
        root.put("accounts", accountsArray)

        // 2. Categories
        val categories = categoryRepository.getAllCategoriesFlow().first()
        val categoriesArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject().apply {
                put("id", cat.id.toString())
                put("name", cat.name)
                put("type", cat.type.name)
                put("parentCategoryId", cat.parentCategoryId?.toString())
                put("isSystem", cat.isSystem)
            }
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        // 3. Transactions
        val transactions = transactionRepository.getAllTransactionsFlow().first()
        val transactionsArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id.toString())
                put("accountId", tx.accountId.toString())
                put("destinationAccountId", tx.destinationAccountId?.toString())
                put("type", tx.type.name)
                put("amountMinor", tx.amount.amountMinor)
                put("competenceDate", tx.competenceDate.toString())
                put("effectiveDate", tx.effectiveDate.toString())
                put("categoryId", tx.categoryId?.toString())
                put("description", tx.description)
                put("status", tx.status.name)
                put("transferId", tx.transferId?.toString())
                put("installmentGroupId", tx.installmentGroupId?.toString())
                put("installmentNumber", tx.installmentNumber)
                put("totalInstallments", tx.totalInstallments)
            }
            transactionsArray.put(obj)
        }
        root.put("transactions", transactionsArray)

        // 4. Budgets
        val budgets = budgetRepository.getAllBudgetsFlow().first()
        val budgetsArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject().apply {
                put("id", b.id.toString())
                put("categoryId", b.categoryId.toString())
                put("limitAmountMinor", b.limitAmount.amountMinor)
                put("periodMonth", b.periodMonth)
                put("periodYear", b.periodYear)
                put("rolloverEnabled", b.rolloverEnabled)
            }
            budgetsArray.put(obj)
        }
        root.put("budgets", budgetsArray)

        // 5. Goals
        val goals = goalRepository.getAllGoalsFlow().first()
        val goalsArray = JSONArray()
        for (g in goals) {
            val obj = JSONObject().apply {
                put("id", g.id.toString())
                put("name", g.name)
                put("targetAmountMinor", g.targetAmount.amountMinor)
                put("linkedAccountId", g.linkedAccountId?.toString())
                put("targetDate", g.targetDate?.toString())
                put("status", g.status.name)
            }
            goalsArray.put(obj)
        }
        root.put("goals", goalsArray)

        // 6. Audit Logs
        val auditLogs = auditRepository.getRecentLogsFlow(limit = 1000).first()
        val auditArray = JSONArray()
        for (a in auditLogs) {
            val obj = JSONObject().apply {
                put("id", a.id.toString())
                put("entityType", a.entityType)
                put("entityId", a.entityId.toString())
                put("action", a.action.name)
                put("timestamp", a.timestamp.toString())
                put("beforeState", a.beforeState)
                put("afterState", a.afterState)
            }
            auditArray.put(obj)
        }
        root.put("auditLogs", auditArray)

        return root.toString(2)
    }
}
