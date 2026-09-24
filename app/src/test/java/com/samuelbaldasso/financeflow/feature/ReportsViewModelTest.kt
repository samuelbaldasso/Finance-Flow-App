package com.samuelbaldasso.financeflow.feature

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.usecase.importer.ImportCandidate
import com.samuelbaldasso.financeflow.domain.usecase.report.GetCashFlowReportUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import com.samuelbaldasso.financeflow.feature.reports.ReportsUiEvent
import com.samuelbaldasso.financeflow.feature.reports.ReportsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReportsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl
    private lateinit var getCashFlowReportUseCase: GetCashFlowReportUseCase
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var viewModel: ReportsViewModel

    private val accountId = UUID.randomUUID()

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

        getCashFlowReportUseCase = GetCashFlowReportUseCase(transactionRepo, categoryRepo)
        createTransactionUseCase = CreateTransactionUseCase(transactionRepo, accountRepo)

        viewModel = ReportsViewModel(
            getCashFlowReportUseCase = getCashFlowReportUseCase,
            transactionRepository = transactionRepo,
            accountRepository = accountRepo,
            createTransactionUseCase = createTransactionUseCase
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `navigation between months updates period in state`() = runTest(testDispatcher) {
        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year

        viewModel.uiState.test {
            awaitItem() // Initial loading
            val state1 = awaitItem()
            assertEquals(currentMonth, state1.month)
            assertEquals(currentYear, state1.year)

            viewModel.onEvent(ReportsUiEvent.NextMonth)
            advanceUntilIdle()

            val state2 = awaitItem()
            val expectedNextMonth = if (currentMonth == 12) 1 else currentMonth + 1
            assertEquals(expectedNextMonth, state2.month)

            viewModel.onEvent(ReportsUiEvent.PreviousMonth)
            advanceUntilIdle()

            val state3 = awaitItem()
            assertEquals(currentMonth, state3.month)
        }
    }

    @Test
    fun `export transactions generates CSV content`() = runTest(testDispatcher) {
        accountRepo.createAccount(
            Account(id = accountId, name = "Corrente", type = AccountType.CHECKING, currency = CurrencyCode.BRL)
        )
        val dateInstant = LocalDate.of(2026, 3, 10).atStartOfDay(ZoneOffset.UTC).toInstant()
        transactionRepo.createTransaction(
            Transaction(
                accountId = accountId,
                type = TransactionType.INCOME,
                amount = Money(2500_00L),
                competenceDate = dateInstant,
                effectiveDate = dateInstant,
                description = "Recebimento Freelance",
                status = TransactionStatus.CLEARED
            )
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Initial loading
            val initial = awaitItem()
            assertNull(initial.exportedCsvContent)

            viewModel.onEvent(ReportsUiEvent.ExportTransactionsCsv)
            advanceUntilIdle()

            val exportedState = awaitItem()
            assertNotNull(exportedState.exportedCsvContent)
            assertTrue(exportedState.exportedCsvContent!!.contains("Recebimento Freelance"))
            assertTrue(exportedState.exportedCsvContent!!.contains("250000"))

            viewModel.onEvent(ReportsUiEvent.ClearExportedData)
            advanceUntilIdle()

            val clearedState = awaitItem()
            assertNull(clearedState.exportedCsvContent)
        }
    }

    @Test
    fun `import CSV parse and confirmation inserts transactions into repository`() = runTest(testDispatcher) {
        accountRepo.createAccount(
            Account(id = accountId, name = "Corrente", type = AccountType.CHECKING, currency = CurrencyCode.BRL)
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Initial loading
            awaitItem() // Loaded content

            viewModel.onEvent(ReportsUiEvent.OpenImportDialog)
            advanceUntilIdle()

            val dialogOpenState = awaitItem()
            assertTrue(dialogOpenState.showImportDialog)

            val csvContent = """
                Data,Descricao,Valor
                2026-03-01,Aluguel,-1200.00
                2026-03-02,Internet,-120.00
            """.trimIndent()

            viewModel.onEvent(ReportsUiEvent.ParseCsvContent(accountId = accountId, csvText = csvContent))
            advanceUntilIdle()

            val parsedState = awaitItem()
            assertEquals(2, parsedState.importCandidates.size)
            assertFalse(parsedState.importCandidates[0].isDuplicate)
            assertEquals("Aluguel", parsedState.importCandidates[0].description)
            assertEquals(1200_00L, parsedState.importCandidates[0].amount.amountMinor)

            viewModel.onEvent(ReportsUiEvent.ConfirmImport(accountId = accountId, items = parsedState.importCandidates))
            advanceUntilIdle()

            val afterImportState = awaitItem()
            assertFalse(afterImportState.showImportDialog)
            assertEquals(0, afterImportState.importCandidates.size)
            assertNotNull(afterImportState.importSummaryMessage)
            assertTrue(afterImportState.importSummaryMessage!!.contains("2 transações"))

            val storedTransactions = transactionRepo.getAllTransactionsFlow().first()
            assertEquals(2, storedTransactions.size)
            assertTrue(storedTransactions.any { it.description == "Aluguel" && it.amount.amountMinor == 1200_00L })
            assertTrue(storedTransactions.any { it.description == "Internet" && it.amount.amountMinor == 120_00L })
        }
    }
}
