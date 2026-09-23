package com.samuelbaldasso.financeflow.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.error.DomainException
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.usecase.account.GetAccountsWithBalanceUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.DeleteTransactionUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.ReconcileTransactionUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.UnreconcileTransactionUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.UpdateTransactionUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class TransactionUseCaseTest {

    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl

    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
    private lateinit var reconcileTransactionUseCase: ReconcileTransactionUseCase
    private lateinit var unreconcileTransactionUseCase: UnreconcileTransactionUseCase
    private lateinit var getAccountsWithBalanceUseCase: GetAccountsWithBalanceUseCase

    private val now = Instant.now()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        auditRepo = AuditRepositoryImpl(db.auditLogDao())
        accountRepo = AccountRepositoryImpl(db.accountDao(), db.transactionDao(), auditRepo)
        transactionRepo = TransactionRepositoryImpl(db.transactionDao(), auditRepo)

        createTransactionUseCase = CreateTransactionUseCase(transactionRepo, accountRepo)
        updateTransactionUseCase = UpdateTransactionUseCase(transactionRepo)
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepo)
        reconcileTransactionUseCase = ReconcileTransactionUseCase(transactionRepo)
        unreconcileTransactionUseCase = UnreconcileTransactionUseCase(transactionRepo)
        getAccountsWithBalanceUseCase = GetAccountsWithBalanceUseCase(accountRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAtomicTransferCreationAndDerivedBalances() = runBlocking {
        val checking = accountRepo.createAccount(
            Account(
                name = "Itaú Corrente",
                type = AccountType.CHECKING,
                currency = CurrencyCode.BRL,
                initialBalance = Money(100000L) // R$ 1.000,00
            )
        )
        val savings = accountRepo.createAccount(
            Account(
                name = "Nubank Reserva",
                type = AccountType.SAVINGS,
                currency = CurrencyCode.BRL,
                initialBalance = Money(20000L) // R$ 200,00
            )
        )

        // Perform transfer of R$ 300,00 from checking to savings
        val transferTx = Transaction(
            accountId = checking.id,
            type = TransactionType.TRANSFER,
            amount = Money(30000L),
            competenceDate = now,
            effectiveDate = now,
            transferId = UUID.randomUUID(),
            destinationAccountId = savings.id,
            description = "Reserva de emergência mensal"
        )
        val created = createTransactionUseCase(transferTx)
        assertNotNull(created.transferId)

        // Verify both legs created
        val legs = transactionRepo.getTransactionsByTransferId(created.transferId!!)
        assertEquals(2, legs.size)

        // Verify derived balances
        getAccountsWithBalanceUseCase().test {
            val list = awaitItem()
            val checkingBal = list.find { it.account.id == checking.id }?.derivedBalance?.amountMinor
            val savingsBal = list.find { it.account.id == savings.id }?.derivedBalance?.amountMinor

            // 1.000 - 300 = 700
            assertEquals(70000L, checkingBal)
            // 200 + 300 = 500
            assertEquals(50000L, savingsBal)
        }
    }

    @Test
    fun testTransferRejectsArchivedAccount() = runBlocking {
        val active = accountRepo.createAccount(
            Account(
                name = "Ativa",
                type = AccountType.CHECKING,
                currency = CurrencyCode.BRL
            )
        )
        val archived = accountRepo.createAccount(
            Account(
                name = "Arquivada",
                type = AccountType.CHECKING,
                currency = CurrencyCode.BRL,
                isArchived = true
            )
        )

        val tx = Transaction(
            accountId = active.id,
            type = TransactionType.TRANSFER,
            amount = Money(5000L),
            competenceDate = now,
            effectiveDate = now,
            transferId = UUID.randomUUID(),
            destinationAccountId = archived.id,
            description = "Tentativa inválida"
        )

        assertThrows(DomainException.AccountArchivedException::class.java) {
            runBlocking { createTransactionUseCase(tx) }
        }
        Unit
    }

    @Test
    fun testReconciliationLockAndUnreconciliationAudit() = runBlocking {
        val account = accountRepo.createAccount(
            Account(
                name = "Carteira",
                type = AccountType.CASH,
                currency = CurrencyCode.BRL
            )
        )
        val tx = createTransactionUseCase(
            Transaction(
                accountId = account.id,
                type = TransactionType.EXPENSE,
                amount = Money(1500L),
                competenceDate = now,
                effectiveDate = now,
                description = "Lanche"
            )
        )

        // Reconcile
        reconcileTransactionUseCase(tx.id)
        val reconciled = transactionRepo.getTransactionById(tx.id)
        assertEquals(TransactionStatus.RECONCILED, reconciled?.status)

        // Direct modification is locked
        assertThrows(DomainException.ReconciledTransactionLockedException::class.java) {
            runBlocking {
                updateTransactionUseCase(reconciled!!.copy(description = "Tentativa de alteração"))
            }
        }

        // Unreconcile
        unreconcileTransactionUseCase(tx.id)
        val unreconciled = transactionRepo.getTransactionById(tx.id)
        assertEquals(TransactionStatus.CLEARED, unreconciled?.status)

        // Now modification succeeds
        updateTransactionUseCase(unreconciled!!.copy(description = "Lanche da tarde"))
        val updated = transactionRepo.getTransactionById(tx.id)
        assertEquals("Lanche da tarde", updated?.description)
    }

    @Test
    fun testAtomicTransferDeletion() = runBlocking {
        val acc1 = accountRepo.createAccount(Account(name = "C1", type = AccountType.CHECKING, currency = CurrencyCode.BRL))
        val acc2 = accountRepo.createAccount(Account(name = "C2", type = AccountType.CHECKING, currency = CurrencyCode.BRL))

        val tx = createTransactionUseCase(
            Transaction(
                accountId = acc1.id,
                type = TransactionType.TRANSFER,
                amount = Money(1000L),
                competenceDate = now,
                effectiveDate = now,
                transferId = UUID.randomUUID(),
                destinationAccountId = acc2.id,
                description = "Pix teste"
            )
        )
        val transferId = tx.transferId!!

        assertEquals(2, transactionRepo.getTransactionsByTransferId(transferId).size)

        // Delete one leg deletes both
        deleteTransactionUseCase(tx.id)
        assertTrue(transactionRepo.getTransactionsByTransferId(transferId).isEmpty())
    }
}
