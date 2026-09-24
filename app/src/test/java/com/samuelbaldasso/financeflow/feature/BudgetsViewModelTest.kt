package com.samuelbaldasso.financeflow.feature

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.core.model.category.Category
import com.samuelbaldasso.financeflow.core.model.category.CategoryType
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.BudgetRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.feature.budgets.BudgetsUiEvent
import com.samuelbaldasso.financeflow.feature.budgets.BudgetsViewModel
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
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BudgetsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FinanceFlowDatabase
    private lateinit var budgetRepo: BudgetRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl
    private lateinit var viewModel: BudgetsViewModel

    private val testCategoryId = UUID.randomUUID()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        auditRepo = AuditRepositoryImpl(db.auditLogDao())
        categoryRepo = CategoryRepositoryImpl(db.categoryDao(), auditRepo)
        budgetRepo = BudgetRepositoryImpl(db.budgetDao(), db.categoryDao(), db.transactionDao())

        viewModel = BudgetsViewModel(
            budgetRepository = budgetRepo,
            categoryRepository = categoryRepo
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `creating budget updates uiState with new budget and limit amount`() = runTest(testDispatcher) {
        categoryRepo.createCategory(
            Category(
                id = testCategoryId,
                name = "Supermercado",
                type = CategoryType.EXPENSE
            )
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // initial loading
            val emptyState = awaitItem() // initial empty db
            assertFalse(emptyState.isLoading)
            assertEquals(0, emptyState.budgets.size)

            viewModel.onEvent(
                BudgetsUiEvent.CreateBudget(
                    categoryId = testCategoryId,
                    amountMinor = 500_00L,
                    rollover = true
                )
            )
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(1, state.budgets.size)
            assertEquals("Supermercado", state.budgets.first().categoryName)
            assertEquals(500_00L, state.totalBudgeted.amountMinor)
            assertTrue(state.budgets.first().budget.rolloverEnabled)
        }
    }

    @Test
    fun `switching month updates period in state`() = runTest(testDispatcher) {
        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year

        viewModel.uiState.test {
            awaitItem() // initial loading
            val state1 = awaitItem()
            assertEquals(currentMonth, state1.month)
            assertEquals(currentYear, state1.year)

            viewModel.onEvent(BudgetsUiEvent.NextMonth)
            advanceUntilIdle()

            val state2 = awaitItem()
            val expectedNextMonth = if (currentMonth == 12) 1 else currentMonth + 1
            assertEquals(expectedNextMonth, state2.month)
        }
    }
}
