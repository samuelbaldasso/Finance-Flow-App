package com.samuelbaldasso.financeflow.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.samuelbaldasso.financeflow.core.database.entity.AccountEntity
import com.samuelbaldasso.financeflow.core.database.entity.AuditLogEntity
import com.samuelbaldasso.financeflow.core.database.entity.CategoryEntity
import com.samuelbaldasso.financeflow.core.database.entity.TransactionEntity
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.audit.AuditAction
import com.samuelbaldasso.financeflow.core.model.category.CategoryType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class FinanceFlowDatabaseTest {

    private lateinit var db: FinanceFlowDatabase
    private val now = Instant.now()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAccountInsertionAndSoftDelete() = runBlocking {
        val account = AccountEntity(
            id = UUID.randomUUID(),
            name = "Itaú Corrente",
            type = AccountType.CHECKING,
            currency = CurrencyCode.BRL,
            initialBalanceMinor = 100000L,
            createdAt = now,
            updatedAt = now
        )
        db.accountDao().insert(account)

        val retrieved = db.accountDao().getById(account.id)
        assertNotNull(retrieved)
        assertEquals("Itaú Corrente", retrieved?.name)

        // Soft delete
        db.accountDao().softDelete(account.id, System.currentTimeMillis())
        val softDeleted = db.accountDao().getById(account.id)
        assertTrue(softDeleted?.isArchived == true)

        // Active accounts flow should be empty
        val active = db.accountDao().getActiveAccountsFlow().first()
        assertTrue(active.isEmpty())
    }

    @Test
    fun testCategoryHierarchyAndReassignmentOnDelete() = runBlocking {
        val catAlimentacao = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Alimentação",
            type = CategoryType.EXPENSE,
            isSystem = true
        )
        val catOutros = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Outros",
            type = CategoryType.EXPENSE,
            isSystem = true
        )
        db.categoryDao().insert(catAlimentacao)
        db.categoryDao().insert(catOutros)

        val subRestaurante = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Restaurante",
            type = CategoryType.EXPENSE,
            parentCategoryId = catAlimentacao.id
        )
        db.categoryDao().insert(subRestaurante)

        val account = AccountEntity(
            id = UUID.randomUUID(),
            name = "Carteira",
            type = AccountType.CASH,
            currency = CurrencyCode.BRL,
            initialBalanceMinor = 5000L,
            createdAt = now,
            updatedAt = now
        )
        db.accountDao().insert(account)

        // Transaction under subRestaurante
        val tx = TransactionEntity(
            id = UUID.randomUUID(),
            accountId = account.id,
            type = TransactionType.EXPENSE,
            amountMinor = 2500L,
            competenceDate = now,
            effectiveDate = now,
            categoryId = subRestaurante.id,
            description = "Almoço",
            status = TransactionStatus.CLEARED,
            createdAt = now,
            updatedAt = now
        )
        db.transactionDao().insert(tx)

        assertEquals(1, db.categoryDao().getTransactionUsageCount(subRestaurante.id))

        // Delete subRestaurante with reassignment to catOutros
        db.categoryDao().deleteWithReassignment(subRestaurante, catOutros.id)

        assertNull(db.categoryDao().getById(subRestaurante.id))
        val updatedTx = db.transactionDao().getById(tx.id)
        assertEquals(catOutros.id, updatedTx?.categoryId)
    }

    @Test
    fun testAtomicTransferPair() = runBlocking {
        val accOrigin = AccountEntity(
            id = UUID.randomUUID(),
            name = "Conta Origem",
            type = AccountType.CHECKING,
            currency = CurrencyCode.BRL,
            initialBalanceMinor = 10000L,
            createdAt = now,
            updatedAt = now
        )
        val accDest = AccountEntity(
            id = UUID.randomUUID(),
            name = "Conta Destino",
            type = AccountType.CHECKING,
            currency = CurrencyCode.BRL,
            initialBalanceMinor = 0L,
            createdAt = now,
            updatedAt = now
        )
        db.accountDao().insert(accOrigin)
        db.accountDao().insert(accDest)

        val transferId = UUID.randomUUID()
        val debitLeg = TransactionEntity(
            id = UUID.randomUUID(),
            accountId = accOrigin.id,
            type = TransactionType.TRANSFER,
            amountMinor = 3000L,
            competenceDate = now,
            effectiveDate = now,
            description = "Transferência Pix",
            status = TransactionStatus.CLEARED,
            transferId = transferId,
            destinationAccountId = accDest.id,
            createdAt = now,
            updatedAt = now
        )
        val creditLeg = TransactionEntity(
            id = UUID.randomUUID(),
            accountId = accDest.id,
            type = TransactionType.TRANSFER,
            amountMinor = 3000L,
            competenceDate = now,
            effectiveDate = now,
            description = "Transferência Pix recebida",
            status = TransactionStatus.CLEARED,
            transferId = transferId,
            destinationAccountId = accOrigin.id,
            createdAt = now,
            updatedAt = now
        )

        db.transactionDao().executeAtomicTransfer(debitLeg, creditLeg)

        val legs = db.transactionDao().getByTransferId(transferId)
        assertEquals(2, legs.size)

        // Delete transfer removes both atomically
        db.transactionDao().deleteByTransferId(transferId)
        val remainingLegs = db.transactionDao().getByTransferId(transferId)
        assertTrue(remainingLegs.isEmpty())
    }

    @Test
    fun testAuditLogging() = runBlocking {
        val entityId = UUID.randomUUID()
        val audit = AuditLogEntity(
            id = UUID.randomUUID(),
            entityType = "ACCOUNT",
            entityId = entityId,
            action = AuditAction.CREATE,
            actor = "user",
            beforeState = null,
            afterState = "{\"name\":\"Nubank\"}",
            timestamp = now
        )
        db.auditLogDao().insert(audit)

        val logs = db.auditLogDao().getLogsForEntityFlow("ACCOUNT", entityId).first()
        assertEquals(1, logs.size)
        assertEquals(AuditAction.CREATE, logs.first().action)
    }
}
