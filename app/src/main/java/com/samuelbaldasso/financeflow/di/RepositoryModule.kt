package com.samuelbaldasso.financeflow.di

import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.BudgetRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.GoalRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.SecurityRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.AuditRepository
import com.samuelbaldasso.financeflow.domain.repository.BudgetRepository
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import com.samuelbaldasso.financeflow.domain.repository.GoalRepository
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(impl: AuditRepositoryImpl): AuditRepository

    @Binds
    @Singleton
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository
}
