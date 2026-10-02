package com.samuelbaldasso.financeflow.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.category.Category
import com.samuelbaldasso.financeflow.core.model.category.CategoryType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.settings.SecuritySettings
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.data.datastore.SecurityPreferencesDataSource
import com.samuelbaldasso.financeflow.data.datastore.securityDataStore
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.BudgetRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.GoalRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.SecurityRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.security.AppLockManager
import com.samuelbaldasso.financeflow.domain.usecase.security.ExportAllUserDataUseCase
import com.samuelbaldasso.financeflow.domain.usecase.security.WipeAllUserDataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository
import com.samuelbaldasso.financeflow.core.model.audit.AuditEvent
import com.samuelbaldasso.financeflow.core.model.audit.AuditAction
import com.samuelbaldasso.financeflow.core.database.entity.CreditCardInvoiceEntity
import com.samuelbaldasso.financeflow.core.database.entity.AuditLogEntity
import kotlinx.coroutines.runBlocking
import androidx.room.withTransaction
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SecurityAndLgpdTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var budgetRepo: BudgetRepositoryImpl
    private lateinit var goalRepo: GoalRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl
    private lateinit var securityRepo: SecurityRepositoryImpl
    private lateinit var appLockManager: AppLockManager

    private lateinit var exportAllUserDataUseCase: ExportAllUserDataUseCase
    private lateinit var wipeAllUserDataUseCase: WipeAllUserDataUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        auditRepo = AuditRepositoryImpl(db.auditLogDao())
        accountRepo = AccountRepositoryImpl(db.accountDao(), db.transactionDao(), auditRepo, db)
        categoryRepo = CategoryRepositoryImpl(db.categoryDao(), auditRepo, db)
        transactionRepo = TransactionRepositoryImpl(db.transactionDao(), auditRepo, db)
        budgetRepo = BudgetRepositoryImpl(db.budgetDao(), db.categoryDao(), db.transactionDao())
        goalRepo = GoalRepositoryImpl(db.goalDao(), db.transactionDao())

        val ds = SecurityPreferencesDataSource(context.securityDataStore)
        securityRepo = SecurityRepositoryImpl(ds)
        appLockManager = AppLockManager()

        exportAllUserDataUseCase = ExportAllUserDataUseCase(
            accountRepo, transactionRepo, categoryRepo, budgetRepo, goalRepo, auditRepo, db, securityRepo
        )
        wipeAllUserDataUseCase = WipeAllUserDataUseCase(db, securityRepo, categoryRepo)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `PIN verification and password hashing works correctly`() = runTest(testDispatcher) {
        securityRepo.clearSecuritySettings()

        assertFalse(securityRepo.verifyPin("1234"))

        securityRepo.setPin("4321")
        advanceUntilIdle()

        assertTrue(securityRepo.verifyPin("4321"))
        assertFalse(securityRepo.verifyPin("1234"))
        assertFalse(securityRepo.verifyPin("4322"))

        val settings = securityRepo.securitySettingsFlow.first()
        assertTrue(settings.isPinSet)
        assertTrue(settings.isAppLockEnabled)

        securityRepo.clearPin()
        advanceUntilIdle()

        assertFalse(securityRepo.verifyPin("4321"))
        val clearedSettings = securityRepo.securitySettingsFlow.first()
        assertFalse(clearedSettings.isPinSet)
    }

    @Test
    fun `Security settings toggles update properly`() = runTest(testDispatcher) {
        securityRepo.setBiometricEnabled(true)
        securityRepo.setLockTimeoutMinutes(5)
        securityRepo.setScreenshotProtection(false)
        securityRepo.setTelemetryConsent(true)
        advanceUntilIdle()

        val settings = securityRepo.securitySettingsFlow.first()
        assertTrue(settings.isBiometricEnabled)
        assertEquals(5, settings.lockTimeoutMinutes)
        assertFalse(settings.isScreenshotProtectionEnabled)
        assertTrue(settings.telemetryConsent)
    }

    @Test
    fun `AppLockManager engages lock based on background timeout`() {
        val settings = SecuritySettings(
            isPinSet = true,
            isBiometricEnabled = false,
            lockTimeoutMinutes = 1
        )

        // Initial state
        assertFalse(appLockManager.isLocked.value)

        // Lock immediately
        appLockManager.lockImmediately()
        assertTrue(appLockManager.isLocked.value)

        // Unlock
        appLockManager.unlock()
        assertFalse(appLockManager.isLocked.value)

        // App backgrounded
        appLockManager.onAppBackgrounded()

        // App foregrounded without security enabled
        val disabledSettings = SecuritySettings(isPinSet = false, isBiometricEnabled = false)
        appLockManager.onAppForegrounded(disabledSettings)
        assertFalse(appLockManager.isLocked.value)

        // App foregrounded with immediate lock (timeout 0)
        val immediateSettings = SecuritySettings(isPinSet = true, lockTimeoutMinutes = 0)
        appLockManager.onAppForegrounded(immediateSettings)
        assertTrue(appLockManager.isLocked.value)
    }

    @Test
    fun `ExportAllUserDataUseCase exports complete JSON with LGPD metadata`() = runTest(testDispatcher) {
        val accId = UUID.randomUUID()
        accountRepo.createAccount(
            Account(id = accId, name = "Nubank", type = AccountType.CHECKING, currency = CurrencyCode.BRL, initialBalance = Money(1000_00L))
        )
        categoryRepo.createCategory(
            Category(name = "Alimentação", type = CategoryType.EXPENSE)
        )
        transactionRepo.createTransaction(
            Transaction(
                accountId = accId,
                type = TransactionType.EXPENSE,
                amount = Money(45_00L),
                competenceDate = Instant.now(),
                effectiveDate = Instant.now(),
                description = "Lanche"
            )
        )
        advanceUntilIdle()

        val jsonString = exportAllUserDataUseCase()
        assertNotNull(jsonString)

        val json = JSONObject(jsonString)
        assertEquals("FinanceFlow", json.getString("app"))
        assertTrue(json.getString("compliance").contains("LGPD"))
        assertTrue(json.getJSONArray("accounts").length() >= 1)
        assertTrue(json.getJSONArray("categories").length() >= 1)
        assertTrue(json.getJSONArray("transactions").length() >= 1)
        assertTrue(json.getJSONArray("auditLogs").length() >= 1)
    }

    @Test
    fun `WipeAllUserDataUseCase deletes all data and re-seeds default categories`() = runTest(testDispatcher) {
        val accId = UUID.randomUUID()
        accountRepo.createAccount(
            Account(id = accId, name = "Conta Salário", type = AccountType.CHECKING, currency = CurrencyCode.BRL)
        )
        transactionRepo.createTransaction(
            Transaction(
                accountId = accId,
                type = TransactionType.INCOME,
                amount = Money(5000_00L),
                competenceDate = Instant.now(),
                effectiveDate = Instant.now(),
                description = "Salário"
            )
        )
        securityRepo.setPin("9999")
        advanceUntilIdle()

        assertEquals(1, accountRepo.getAllAccountsFlow().first().size)
        assertEquals(1, transactionRepo.getAllTransactionsFlow().first().size)
        assertTrue(securityRepo.verifyPin("9999"))

        // Wipe all data
        wipeAllUserDataUseCase()
        advanceUntilIdle()

        // Verify accounts and transactions are empty
        assertEquals(0, accountRepo.getAllAccountsFlow().first().size)
        assertEquals(0, transactionRepo.getAllTransactionsFlow().first().size)

        // Verify security preferences are reset
        assertFalse(securityRepo.verifyPin("9999"))

        // Verify default categories were re-seeded
        val categories = categoryRepo.getAllCategoriesFlow().first()
        assertTrue(categories.isNotEmpty())
        assertTrue(categories.any { it.isSystem })
    }
    @Test
    fun `export includes all audit entries transaction metadata and invoices`() = runTest(testDispatcher) {
        val account = accountRepo.createAccount(Account(name = "Cartao", type = AccountType.CREDIT_CARD,
            currency = CurrencyCode.BRL, closingDay = 10, dueDay = 20))
        val invoice = CreditCardInvoiceEntity(id = UUID.randomUUID(), cardAccountId = account.id,
            closingDate = Instant.now(), dueDate = Instant.now().plusSeconds(86400), totalMinor = 500L)
        db.creditCardInvoiceDao().insert(invoice)
        val tx = Transaction(accountId = account.id, type = TransactionType.EXPENSE, amount = Money(500L),
            competenceDate = Instant.now(), effectiveDate = Instant.now(), description = "Compra",
            tags = listOf("viagem", "trabalho"), recurrenceRuleId = UUID.randomUUID(), invoiceId = invoice.id,
            attachmentPath = "/private/receipt.pdf")
        transactionRepo.createTransaction(tx)
        db.withTransaction {
            repeat(1005) {
                db.auditLogDao().insert(AuditLogEntity.fromDomain(AuditEvent(entityType = "TEST",
                    entityId = tx.id, action = AuditAction.UPDATE, actor = "regression")))
            }
        }
        val expectedLogs = auditRepo.getAllLogsFlow().first().size
        val json = JSONObject(exportAllUserDataUseCase())
        assertEquals(expectedLogs, json.getJSONArray("auditLogs").length())
        assertTrue(expectedLogs > 1000)
        val exported = json.getJSONArray("transactions").getJSONObject(0)
        assertEquals(tx.recurrenceRuleId.toString(), exported.getString("recurrenceRuleId"))
        assertEquals(tx.invoiceId.toString(), exported.getString("invoiceId"))
        assertEquals("viagem", exported.getJSONArray("tags").getString(0))
        assertEquals(Instant.ofEpochMilli(tx.createdAt.toEpochMilli()).toString(), exported.getString("createdAt"))
        assertEquals(1, json.getJSONArray("creditCardInvoices").length())
        assertFalse(json.getBoolean("attachmentsIncluded"))
        assertFalse(json.getJSONObject("securitySettings").has("pinHash"))
    }

    @Test
    fun `failed preference cleanup leaves durable wipe intent and can be retried`() = runTest(testDispatcher) {
        securityRepo.clearSecuritySettings()
        accountRepo.createAccount(Account(name = "Apagar", type = AccountType.CHECKING, currency = CurrencyCode.BRL))
        securityRepo.setPin("1234")
        val failingSecurity = object : SecurityRepository by securityRepo {
            override suspend fun clearSecuritySettings() { error("Simulated disk write failure") }
        }
        val interruptedWipe = WipeAllUserDataUseCase(db, failingSecurity, categoryRepo)
        val failure = runCatching { interruptedWipe() }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertTrue(securityRepo.isWipePending())
        assertTrue(accountRepo.getAllAccountsFlow().first().isEmpty())
        wipeAllUserDataUseCase()
        assertFalse(securityRepo.isWipePending())
        assertFalse(securityRepo.verifyPin("1234"))
        assertTrue(categoryRepo.getAllCategoriesFlow().first().all { it.isSystem })
    }

}
