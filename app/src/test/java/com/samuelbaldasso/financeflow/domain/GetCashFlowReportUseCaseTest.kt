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
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.usecase.report.GetCashFlowReportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GetCashFlowReportUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl
    private lateinit var useCase: GetCashFlowReportUseCase

    private val accountId = UUID.randomUUID()
    private val destAccountId = UUID.randomUUID()
    private val catFoodId = UUID.randomUUID()
    private val catTransportId = UUID.randomUUID()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        auditRepo = AuditRepositoryImpl(db.auditLogDao())
        accountRepo = AccountRepositoryImpl(db.accountDao(), db.transactionDao(), auditRepo)
        categoryRepo = CategoryRepositoryImpl(db.categoryDao(), auditRepo)
        transactionRepo = TransactionRepositoryImpl(db.transactionDao(), auditRepo)

        useCase = GetCashFlowReportUseCase(transactionRepo, categoryRepo)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `transfers between own accounts are strictly excluded from income and expenses`() = runTest(testDispatcher) {
        accountRepo.createAccount(
            Account(id = accountId, name = "Corrente", type = AccountType.CHECKING, currency = CurrencyCode.BRL, initialBalance = Money(5000_00L))
        )
        accountRepo.createAccount(
            Account(id = destAccountId, name = "Poupança", type = AccountType.SAVINGS, currency = CurrencyCode.BRL, initialBalance = Money(1000_00L))
        )
        categoryRepo.createCategory(Category(id = catFoodId, name = "Alimentação", type = CategoryType.EXPENSE))
        categoryRepo.createCategory(Category(id = catTransportId, name = "Transporte", type = CategoryType.EXPENSE))

        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year
        val dateInstant = LocalDate.of(currentYear, currentMonth, 15).atStartOfDay(ZoneOffset.UTC).toInstant()

        // 1. Regular Income: R$ 3.000,00
        transactionRepo.createTransaction(
            Transaction(
                accountId = accountId,
                type = TransactionType.INCOME,
                amount = Money(3000_00L),
                competenceDate = dateInstant,
                effectiveDate = dateInstant,
                description = "Salário"
            )
        )

        // 2. Regular Expenses: Food R$ 600,00 and Transport R$ 400,00 (Total Expense R$ 1.000,00)
        transactionRepo.createTransaction(
            Transaction(
                accountId = accountId,
                type = TransactionType.EXPENSE,
                amount = Money(600_00L),
                competenceDate = dateInstant,
                effectiveDate = dateInstant,
                categoryId = catFoodId,
                description = "Restaurante"
            )
        )
        transactionRepo.createTransaction(
            Transaction(
                accountId = accountId,
                type = TransactionType.EXPENSE,
                amount = Money(400_00L),
                competenceDate = dateInstant,
                effectiveDate = dateInstant,
                categoryId = catTransportId,
                description = "Metrô"
            )
        )

        // 3. Transfer between accounts: R$ 1.500,00 (Must NOT count as income or expense!)
        val transferId = UUID.randomUUID()
        transactionRepo.createTransaction(
            Transaction(
                accountId = accountId,
                destinationAccountId = destAccountId,
                type = TransactionType.TRANSFER,
                amount = Money(1500_00L),
                competenceDate = dateInstant,
                effectiveDate = dateInstant,
                description = "Transferência para Poupança",
                transferId = transferId
            )
        )
        advanceUntilIdle()

        useCase(currentMonth, currentYear).test {
            val report = awaitItem()

            // Verify: Income is exactly R$ 3.000,00 (transfer R$ 1.500 is ignored)
            assertEquals(3000_00L, report.totalIncome.amountMinor)
            // Verify: Expense is exactly R$ 1.000,00 (transfer R$ 1.500 is ignored)
            assertEquals(1000_00L, report.totalExpense.amountMinor)
            // Net savings = 3.000 - 1.000 = 2.000
            assertEquals(2000_00L, report.netSavings.amountMinor)
            assertTrue(report.isPositiveCashFlow)

            // Category breakdowns: Food (60%), Transport (40%)
            assertEquals(2, report.categoryBreakdowns.size)
            val food = report.categoryBreakdowns.first { it.categoryId == catFoodId }
            assertEquals(600_00L, food.totalExpense.amountMinor)
            assertEquals(60f, food.percentageOfTotal, 0.01f)

            val transport = report.categoryBreakdowns.first { it.categoryId == catTransportId }
            assertEquals(400_00L, transport.totalExpense.amountMinor)
            assertEquals(40f, transport.percentageOfTotal, 0.01f)
        }
    }
}
