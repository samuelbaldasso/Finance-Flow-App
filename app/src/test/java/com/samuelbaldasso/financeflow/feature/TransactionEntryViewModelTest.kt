package com.samuelbaldasso.financeflow.feature

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
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryUiEffect
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryUiEvent
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TransactionEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var viewModel: TransactionEntryViewModel

    private val testAccountId = UUID.randomUUID()
    private val testCategoryId = UUID.randomUUID()

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

        createTransactionUseCase = CreateTransactionUseCase(
            transactionRepository = transactionRepo,
            accountRepository = accountRepo
        )

        viewModel = TransactionEntryViewModel(
            accountRepository = accountRepo,
            categoryRepository = categoryRepo,
            createTransactionUseCase = createTransactionUseCase
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `entering amount and submitting transaction emits TransactionSaved effect`() = runTest(testDispatcher) {
        accountRepo.createAccount(
            Account(
                id = testAccountId,
                name = "Carteira",
                type = AccountType.CASH,
                currency = CurrencyCode.BRL,
                initialBalance = Money(50_00L)
            )
        )
        categoryRepo.createCategory(
            Category(
                id = testCategoryId,
                name = "Alimentação",
                iconKey = "fastfood",
                colorHex = "#FF9800",
                type = CategoryType.EXPENSE
            )
        )
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(TransactionEntryUiEvent.TypeChanged(TransactionType.EXPENSE))
            viewModel.onEvent(TransactionEntryUiEvent.AccountSelected(testAccountId))
            viewModel.onEvent(TransactionEntryUiEvent.CategorySelected(testCategoryId))
            viewModel.onEvent(TransactionEntryUiEvent.AmountChanged(25_50L))
            viewModel.onEvent(TransactionEntryUiEvent.DescriptionChanged("Almoço"))

            viewModel.onEvent(TransactionEntryUiEvent.Submit)
            advanceUntilIdle()

            val effect = awaitItem()
            assertEquals(TransactionEntryUiEffect.TransactionSaved, effect)

            val txs = transactionRepo.getAllTransactionsFlow().first()
            assertEquals(1, txs.size)
            assertEquals(25_50L, txs.first().amount.amountMinor)
            assertEquals("Almoço", txs.first().description)
        }
    }
}
