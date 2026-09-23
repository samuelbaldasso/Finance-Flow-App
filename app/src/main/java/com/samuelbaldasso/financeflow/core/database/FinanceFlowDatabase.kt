package com.samuelbaldasso.financeflow.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.samuelbaldasso.financeflow.core.database.converter.DatabaseConverters
import com.samuelbaldasso.financeflow.core.database.dao.AccountDao
import com.samuelbaldasso.financeflow.core.database.dao.AuditLogDao
import com.samuelbaldasso.financeflow.core.database.dao.BudgetDao
import com.samuelbaldasso.financeflow.core.database.dao.CategoryDao
import com.samuelbaldasso.financeflow.core.database.dao.CreditCardInvoiceDao
import com.samuelbaldasso.financeflow.core.database.dao.GoalDao
import com.samuelbaldasso.financeflow.core.database.dao.TransactionDao
import com.samuelbaldasso.financeflow.core.database.entity.AccountEntity
import com.samuelbaldasso.financeflow.core.database.entity.AuditLogEntity
import com.samuelbaldasso.financeflow.core.database.entity.BudgetEntity
import com.samuelbaldasso.financeflow.core.database.entity.CategoryEntity
import com.samuelbaldasso.financeflow.core.database.entity.CreditCardInvoiceEntity
import com.samuelbaldasso.financeflow.core.database.entity.GoalEntity
import com.samuelbaldasso.financeflow.core.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        CreditCardInvoiceEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class FinanceFlowDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun creditCardInvoiceDao(): CreditCardInvoiceDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        const val DATABASE_NAME = "finance_flow.db"

        fun build(context: Context, inMemory: Boolean = false): FinanceFlowDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            } else {
                Room.databaseBuilder(context, FinanceFlowDatabase::class.java, DATABASE_NAME)
            }
            return builder
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
