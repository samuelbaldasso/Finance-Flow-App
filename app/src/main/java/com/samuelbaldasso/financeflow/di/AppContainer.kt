package com.samuelbaldasso.financeflow.di

import android.content.Context
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.BudgetRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.GoalRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.AuditRepository
import com.samuelbaldasso.financeflow.domain.repository.BudgetRepository
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import com.samuelbaldasso.financeflow.domain.repository.GoalRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import com.samuelbaldasso.financeflow.domain.usecase.account.ArchiveAccountUseCase
import com.samuelbaldasso.financeflow.domain.usecase.account.CreateAccountUseCase
import com.samuelbaldasso.financeflow.domain.usecase.account.GetAccountsWithBalanceUseCase
import com.samuelbaldasso.financeflow.domain.usecase.card.CalculateAvailableLimitUseCase
import com.samuelbaldasso.financeflow.domain.usecase.card.CreateInstallmentPurchaseUseCase
import com.samuelbaldasso.financeflow.domain.usecase.category.DeleteCategoryUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.DeleteTransactionUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.ReconcileTransactionUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.UnreconcileTransactionUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.UpdateTransactionUseCase

class AppContainer(private val context: Context) {

    val database: FinanceFlowDatabase by lazy {
        FinanceFlowDatabase.build(context)
    }

    val auditRepository: AuditRepository by lazy {
        AuditRepositoryImpl(database.auditLogDao())
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database.accountDao(), database.transactionDao(), auditRepository)
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao(), auditRepository)
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao(), auditRepository)
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(database.budgetDao(), database.categoryDao(), database.transactionDao())
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(database.goalDao(), database.transactionDao())
    }

    // Use Cases
    val getAccountsWithBalanceUseCase by lazy { GetAccountsWithBalanceUseCase(accountRepository) }
    val createAccountUseCase by lazy { CreateAccountUseCase(accountRepository) }
    val archiveAccountUseCase by lazy { ArchiveAccountUseCase(accountRepository) }

    val createTransactionUseCase by lazy { CreateTransactionUseCase(transactionRepository, accountRepository) }
    val updateTransactionUseCase by lazy { UpdateTransactionUseCase(transactionRepository) }
    val deleteTransactionUseCase by lazy { DeleteTransactionUseCase(transactionRepository) }
    val reconcileTransactionUseCase by lazy { ReconcileTransactionUseCase(transactionRepository) }
    val unreconcileTransactionUseCase by lazy { UnreconcileTransactionUseCase(transactionRepository) }

    val deleteCategoryUseCase by lazy { DeleteCategoryUseCase(categoryRepository) }
    val createInstallmentPurchaseUseCase by lazy { CreateInstallmentPurchaseUseCase(transactionRepository) }
    val calculateAvailableLimitUseCase by lazy { CalculateAvailableLimitUseCase(transactionRepository) }
}
