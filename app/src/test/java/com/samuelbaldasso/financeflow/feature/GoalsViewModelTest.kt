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
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.GoalRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import com.samuelbaldasso.financeflow.feature.goals.GoalsUiEvent
import com.samuelbaldasso.financeflow.feature.goals.GoalsViewModel
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GoalsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl
    private lateinit var goalRepo: GoalRepositoryImpl
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var viewModel: GoalsViewModel

    private val testAccountId = UUID.randomUUID()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        auditRepo = AuditRepositoryImpl(db.auditLogDao())
        accountRepo = AccountRepositoryImpl(db.accountDao(), db.transactionDao(), auditRepo, db)
        transactionRepo = TransactionRepositoryImpl(db.transactionDao(), auditRepo, db)
        goalRepo = GoalRepositoryImpl(db.goalDao(), db.transactionDao())

        createTransactionUseCase = CreateTransactionUseCase(transactionRepo, accountRepo)

        viewModel = GoalsViewModel(
            goalRepository = goalRepo,
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
    fun `creating a goal and submitting contribution updates progress`() = runTest(testDispatcher) {
        accountRepo.createAccount(
            Account(
                id = testAccountId,
                name = "Conta Corrente",
                type = AccountType.CHECKING,
                currency = CurrencyCode.BRL,
                initialBalance = Money(1000_00L)
            )
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // initial loading
            val emptyState = awaitItem() // initial empty db
            assertEquals(0, emptyState.goals.size)

            // 1. Create Goal
            viewModel.onEvent(
                GoalsUiEvent.CreateGoal(
                    name = "Viagem",
                    targetAmountMinor = 1000_00L,
                    targetDate = null,
                    linkedAccountId = null
                )
            )
            advanceUntilIdle()

            val stateWithGoal = awaitItem()
            assertEquals(1, stateWithGoal.goals.size)
            val createdGoal = stateWithGoal.goals.first()
            assertEquals("Viagem", createdGoal.goal.name)
            assertEquals(0L, createdGoal.currentSavedAmount.amountMinor)
            assertFalse(createdGoal.isCompleted)

            // 2. Submit Contribution of R$ 1.000,00
            viewModel.onEvent(
                GoalsUiEvent.SubmitContribution(
                    goalId = createdGoal.goal.id,
                    accountId = testAccountId,
                    amountMinor = 1000_00L
                )
            )
            advanceUntilIdle()

            val stateAfterContribution = awaitItem()
            val completedGoal = stateAfterContribution.goals.first()
            assertEquals(1000_00L, completedGoal.currentSavedAmount.amountMinor)
            assertTrue(completedGoal.isCompleted)
            assertEquals(1, stateAfterContribution.completedGoalsCount)
        }
    }
}
