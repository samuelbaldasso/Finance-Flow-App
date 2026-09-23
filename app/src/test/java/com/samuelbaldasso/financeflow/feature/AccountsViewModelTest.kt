package com.samuelbaldasso.financeflow.feature

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.usecase.account.ArchiveAccountUseCase
import com.samuelbaldasso.financeflow.domain.usecase.account.CreateAccountUseCase
import com.samuelbaldasso.financeflow.domain.usecase.account.GetAccountsWithBalanceUseCase
import com.samuelbaldasso.financeflow.feature.accounts.AccountsUiEvent
import com.samuelbaldasso.financeflow.feature.accounts.AccountsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl

    private lateinit var getAccountsWithBalanceUseCase: GetAccountsWithBalanceUseCase
    private lateinit var createAccountUseCase: CreateAccountUseCase
    private lateinit var archiveAccountUseCase: ArchiveAccountUseCase
    private lateinit var viewModel: AccountsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        auditRepo = AuditRepositoryImpl(db.auditLogDao())
        accountRepo = AccountRepositoryImpl(db.accountDao(), db.transactionDao(), auditRepo)
        transactionRepo = TransactionRepositoryImpl(db.transactionDao(), auditRepo)

        getAccountsWithBalanceUseCase = GetAccountsWithBalanceUseCase(accountRepo)
        createAccountUseCase = CreateAccountUseCase(accountRepo)
        archiveAccountUseCase = ArchiveAccountUseCase(accountRepo)

        viewModel = AccountsViewModel(
            getAccountsWithBalanceUseCase = getAccountsWithBalanceUseCase,
            createAccountUseCase = createAccountUseCase,
            archiveAccountUseCase = archiveAccountUseCase
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `creating an account updates uiState with balance and totals`() = runTest(testDispatcher) {
        viewModel.uiState.test {
            // initial state
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            // initial empty db emission
            val emptyState = awaitItem()
            assertFalse(emptyState.isLoading)
            assertEquals(0, emptyState.accounts.size)

            // Create account
            viewModel.onEvent(
                AccountsUiEvent.CreateAccount(
                    name = "Nubank",
                    type = AccountType.CHECKING,
                    currency = CurrencyCode.BRL,
                    initialBalanceMinor = 150_00L
                )
            )
            advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.accounts.size)
            assertEquals("Nubank", state.accounts.first().account.name)
            assertEquals(150_00L, state.totalBrlBalance.amountMinor)
        }
    }

    @Test
    fun `toggle show archived updates uiState`() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem() // initial loading
            awaitItem() // initial empty db

            viewModel.onEvent(AccountsUiEvent.ToggleShowArchived)
            advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.showArchived)
        }
    }
}
