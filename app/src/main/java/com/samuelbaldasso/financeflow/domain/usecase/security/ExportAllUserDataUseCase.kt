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
import androidx.room.withTransaction
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository

class ExportAllUserDataUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val auditRepository: AuditRepository,
    private val database: FinanceFlowDatabase,
    private val securityRepository: SecurityRepository
) {
    suspend operator fun invoke(): String {
        val settings = securityRepository.securitySettingsFlow.first()
        return database.withTransaction {
            val root = JSONObject()
            root.put("app", "FinanceFlow")
            root.put("version", "1.0.0")
            root.put("exportSchemaVersion", 2)
            root.put("attachmentsIncluded", false)
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
                    put("colorHex", acc.colorHex)
                    put("iconKey", acc.iconKey)
                    put("createdAt", acc.createdAt.toString())
                    put("updatedAt", acc.updatedAt.toString())
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
                    put("iconKey", cat.iconKey)
                    put("colorHex", cat.colorHex)
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
                    put("tags", JSONArray(tx.tags))
                    put("attachmentPath", tx.attachmentPath)
                    put("goalId", tx.goalId?.toString())
                    put("recurrenceRuleId", tx.recurrenceRuleId?.toString())
                    put("invoiceId", tx.invoiceId?.toString())
                    put("createdAt", tx.createdAt.toString())
                    put("updatedAt", tx.updatedAt.toString())
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
                    put("previousRolloverAmountMinor", b.previousRolloverAmount.amountMinor)
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
                    put("createdAt", g.createdAt.toString())
                }
                goalsArray.put(obj)
            }
            root.put("goals", goalsArray)

            // 6. Audit Logs
            val auditLogs = auditRepository.getAllLogsFlow().first()
            val auditArray = JSONArray()
            for (a in auditLogs) {
                val obj = JSONObject().apply {
                    put("id", a.id.toString())
                    put("entityType", a.entityType)
                    put("entityId", a.entityId.toString())
                    put("action", a.action.name)
                    put("actor", a.actor)
                    put("timestamp", a.timestamp.toString())
                    put("beforeState", a.beforeState)
                    put("afterState", a.afterState)
                }
                auditArray.put(obj)
            }
            root.put("auditLogs", auditArray)

            val invoices = JSONArray()
            database.creditCardInvoiceDao().getAll().forEach { invoice ->
                invoices.put(JSONObject().apply {
                    put("id", invoice.id.toString())
                    put("cardAccountId", invoice.cardAccountId.toString())
                    put("closingDate", invoice.closingDate.toString())
                    put("dueDate", invoice.dueDate.toString())
                    put("totalMinor", invoice.totalMinor)
                    put("paidMinor", invoice.paidMinor)
                    put("status", invoice.status.name)
                })
            }
            root.put("creditCardInvoices", invoices)
            root.put("securitySettings", JSONObject().apply {
                put("isPinSet", settings.isPinSet)
                put("isBiometricEnabled", settings.isBiometricEnabled)
                put("lockTimeoutMinutes", settings.lockTimeoutMinutes)
                put("isScreenshotProtectionEnabled", settings.isScreenshotProtectionEnabled)
                put("telemetryConsent", settings.telemetryConsent)
            })
            root.toString(2)
        }
    }
}
