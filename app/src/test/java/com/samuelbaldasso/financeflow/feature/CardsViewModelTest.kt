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
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.usecase.card.CalculateAvailableLimitUseCase
import com.samuelbaldasso.financeflow.domain.usecase.card.CreateInstallmentPurchaseUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import com.samuelbaldasso.financeflow.feature.cards.CardsUiEvent
import com.samuelbaldasso.financeflow.feature.cards.CardsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class CardsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl

    private lateinit var calculateAvailableLimitUseCase: CalculateAvailableLimitUseCase
    private lateinit var createInstallmentPurchaseUseCase: CreateInstallmentPurchaseUseCase
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var viewModel: CardsViewModel

    private val cardAccountId = UUID.randomUUID()
    private val checkingAccountId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()

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

        calculateAvailableLimitUseCase = CalculateAvailableLimitUseCase(transactionRepo)
        createInstallmentPurchaseUseCase = CreateInstallmentPurchaseUseCase(transactionRepo)
        createTransactionUseCase = CreateTransactionUseCase(transactionRepo, accountRepo)

        viewModel = CardsViewModel(
            accountRepository = accountRepo,
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            calculateAvailableLimitUseCase = calculateAvailableLimitUseCase,
            createInstallmentPurchaseUseCase = createInstallmentPurchaseUseCase,
            createTransactionUseCase = createTransactionUseCase
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `creating installment purchase updates card transactions and available limit`() = runTest(testDispatcher) {
        accountRepo.createAccount(
            Account(
                id = cardAccountId,
                name = "Nubank Ultravioleta",
                type = AccountType.CREDIT_CARD,
                currency = CurrencyCode.BRL,
                initialBalance = Money.ZERO,
                creditLimit = Money(5000_00L),
                closingDay = 10,
                dueDay = 20
            )
        )
        accountRepo.createAccount(
            Account(
                id = checkingAccountId,
                name = "Itaú Corrente",
                type = AccountType.CHECKING,
                currency = CurrencyCode.BRL,
                initialBalance = Money(2000_00L)
            )
        )
        categoryRepo.createCategory(
            Category(
                id = categoryId,
                name = "Eletrônicos",
                type = CategoryType.EXPENSE
            )
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // initial loading
            val stateWithCard = awaitItem()
            assertEquals(1, stateWithCard.cards.size)
            val card = stateWithCard.cards.first()
            assertEquals(5000_00L, card.availableLimit.amountMinor)
            assertEquals(0L, card.usedLimit.amountMinor)

            // Purchase of R$ 1.200 in 3x
            viewModel.onEvent(
                CardsUiEvent.CreateInstallmentPurchase(
                    description = "Notebook Dell",
                    totalAmountMinor = 1200_00L,
                    installmentsCount = 3,
                    categoryId = categoryId
                )
            )
            advanceUntilIdle()

            var updatedCard = awaitItem().cards.first()
            while (updatedCard.installments.size < 3) {
                updatedCard = awaitItem().cards.first()
            }
            assertEquals(3, updatedCard.installments.size)
            assertEquals(1200_00L, updatedCard.usedLimit.amountMinor)
            assertEquals(3800_00L, updatedCard.availableLimit.amountMinor)
        }
    }
}
