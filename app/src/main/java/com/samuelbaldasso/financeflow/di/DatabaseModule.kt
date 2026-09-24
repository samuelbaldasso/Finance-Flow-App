package com.samuelbaldasso.financeflow.di

import android.content.Context
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.core.database.dao.AccountDao
import com.samuelbaldasso.financeflow.core.database.dao.AuditLogDao
import com.samuelbaldasso.financeflow.core.database.dao.BudgetDao
import com.samuelbaldasso.financeflow.core.database.dao.CategoryDao
import com.samuelbaldasso.financeflow.core.database.dao.GoalDao
import com.samuelbaldasso.financeflow.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinanceFlowDatabase {
        return FinanceFlowDatabase.build(context)
    }

    @Provides
    fun provideAccountDao(database: FinanceFlowDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideTransactionDao(database: FinanceFlowDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: FinanceFlowDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideBudgetDao(database: FinanceFlowDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    fun provideGoalDao(database: FinanceFlowDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideAuditLogDao(database: FinanceFlowDatabase): AuditLogDao {
        return database.auditLogDao()
    }
}
