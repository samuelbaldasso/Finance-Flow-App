package com.samuelbaldasso.financeflow.data.repository

import com.samuelbaldasso.financeflow.core.database.dao.TransactionDao
import com.samuelbaldasso.financeflow.core.database.entity.TransactionEntity
import com.samuelbaldasso.financeflow.core.model.audit.AuditAction
import com.samuelbaldasso.financeflow.core.model.audit.AuditEvent
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.domain.repository.AuditRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import androidx.room.withTransaction
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import java.time.Instant
import java.util.UUID

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val auditRepository: AuditRepository,
    private val database: FinanceFlowDatabase
) : TransactionRepository {

    override fun getAllTransactionsFlow(): Flow<List<Transaction>> {
        return transactionDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsPagedFlow(pageSize: Int): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false)
        ) {
            transactionDao.getAllPaged()
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getTransactionsByAccountFlow(accountId: UUID): Flow<List<Transaction>> {
        return transactionDao.getByAccountFlow(accountId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByAccountPagedFlow(accountId: UUID, pageSize: Int): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false)
        ) {
            transactionDao.getByAccountPaged(accountId)
        }.flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getGoalContributionsFlow(goalId: UUID): Flow<List<Transaction>> {
        return transactionDao.getGoalContributionsFlow(goalId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTransactionById(id: UUID): Transaction? {
        return transactionDao.getById(id)?.toDomain()
    }

    override suspend fun getTransactionsByTransferId(transferId: UUID): List<Transaction> {
        return transactionDao.getByTransferId(transferId).map { it.toDomain() }
    }

    override suspend fun createTransaction(transaction: Transaction): Transaction = database.withTransaction {
        require(!transaction.isTransfer) { "Transfers must be persisted as an atomic pair" }
        requireActiveAccount(transaction.accountId)
        transaction.validateFutureDate(Instant.now())
        transactionDao.insert(TransactionEntity.fromDomain(transaction))
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "TRANSACTION",
                entityId = transaction.id,
                action = AuditAction.CREATE,
                afterState = "type=${transaction.type}, amount=${transaction.amount.amountMinor}, desc=${transaction.description}"
            )
        )
        return@withTransaction transaction
    }

    override suspend fun createTransfer(debitTx: Transaction, creditTx: Transaction) = database.withTransaction {
        val source = requireActiveAccount(debitTx.accountId)
        val destination = requireActiveAccount(creditTx.accountId)
        require(source.id != destination.id) { "Transfer accounts must be different" }
        require(source.currency == destination.currency) { "Transfer requires accounts in the same currency" }
        require(debitTx.isTransfer && creditTx.isTransfer && debitTx.transferId != null &&
            debitTx.transferId == creditTx.transferId && debitTx.amount == creditTx.amount &&
            debitTx.destinationAccountId == creditTx.accountId && creditTx.destinationAccountId == null &&
            debitTx.status == creditTx.status) { "Invalid transfer pair" }
        debitTx.validateFutureDate(Instant.now())
        creditTx.validateFutureDate(Instant.now())

        transactionDao.executeAtomicTransfer(
            TransactionEntity.fromDomain(debitTx),
            TransactionEntity.fromDomain(creditTx)
        )

        auditRepository.recordEvent(
            AuditEvent(
                entityType = "TRANSACTION",
                entityId = debitTx.id,
                action = AuditAction.CREATE,
                afterState = "TRANSFER debit ${debitTx.amount.amountMinor} to ${debitTx.destinationAccountId}"
            )
        )
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "TRANSACTION",
                entityId = creditTx.id,
                action = AuditAction.CREATE,
                afterState = "TRANSFER credit ${creditTx.amount.amountMinor} from ${creditTx.destinationAccountId}"
            )
        )
    }

    override suspend fun updateTransaction(transaction: Transaction) = database.withTransaction {
        val existing = transactionDao.getById(transaction.id)?.toDomain()
            ?: throw IllegalArgumentException("Transaction ${transaction.id} not found")

        existing.validateModificationAllowed()
        requireActiveAccount(transaction.accountId)
        require(transaction.accountId == existing.accountId && transaction.type == existing.type &&
            transaction.transferId == existing.transferId &&
            transaction.destinationAccountId == existing.destinationAccountId) {
            "Changing transaction ownership or transfer structure requires a new transaction"
        }
        transaction.validateFutureDate(Instant.now())

        val transferId = transaction.transferId
        if (transaction.isTransfer && transferId != null) {
            val transferLegs = transactionDao.getByTransferId(transferId)
            require(transferLegs.size == 2) { "Transfer must contain exactly two legs" }
            val otherLeg = transferLegs.firstOrNull { it.id != transaction.id }
            requireNotNull(otherLeg) { "Transfer counterpart is missing" }
            otherLeg.toDomain().validateModificationAllowed()
            requireActiveAccount(otherLeg.accountId)
            val updatedOtherLeg = otherLeg.copy(
                amountMinor = transaction.amount.amountMinor,
                competenceDate = transaction.competenceDate,
                effectiveDate = transaction.effectiveDate,
                status = transaction.status,
                updatedAt = Instant.now()
            )
            transactionDao.updateAtomicTransfer(
                TransactionEntity.fromDomain(transaction),
                updatedOtherLeg
            )
            auditRepository.recordEvent(AuditEvent(
                entityType = "TRANSACTION", entityId = otherLeg.id, action = AuditAction.UPDATE,
                beforeState = "amount=${otherLeg.amountMinor}, status=${otherLeg.status}",
                afterState = "amount=${updatedOtherLeg.amountMinor}, status=${updatedOtherLeg.status}"
            ))

        } else {
            transactionDao.update(TransactionEntity.fromDomain(transaction))
        }

        auditRepository.recordEvent(
            AuditEvent(
                entityType = "TRANSACTION",
                entityId = transaction.id,
                action = AuditAction.UPDATE,
                beforeState = "amount=${existing.amount.amountMinor}, status=${existing.status}",
                afterState = "amount=${transaction.amount.amountMinor}, status=${transaction.status}"
            )
        )
    }

    override suspend fun deleteTransaction(id: UUID) = database.withTransaction {
        val existing = transactionDao.getById(id)?.toDomain() ?: return@withTransaction
        existing.validateModificationAllowed()

        val transferId = existing.transferId
        if (existing.isTransfer && transferId != null) {
            val legs = transactionDao.getByTransferId(transferId)
            legs.forEach { it.toDomain().validateModificationAllowed() }
            transactionDao.deleteByTransferId(transferId)
            legs.filter { it.id != id }.forEach { leg ->
                auditRepository.recordEvent(AuditEvent(
                    entityType = "TRANSACTION", entityId = leg.id, action = AuditAction.DELETE,
                    beforeState = "amount=${leg.amountMinor}, transferId=$transferId"
                ))
            }
            auditRepository.recordEvent(
                AuditEvent(
                    entityType = "TRANSACTION",
                    entityId = id,
                    action = AuditAction.DELETE,
                    beforeState = "Atomic transfer ${existing.transferId} deleted"
                )
            )
        } else {
            transactionDao.delete(TransactionEntity.fromDomain(existing))
            auditRepository.recordEvent(
                AuditEvent(
                    entityType = "TRANSACTION",
                    entityId = id,
                    action = AuditAction.DELETE,
                    beforeState = "amount=${existing.amount.amountMinor}, desc=${existing.description}"
                )
            )
        }
    }

    override suspend fun reconcileTransaction(id: UUID) {
        changeStatus(id, TransactionStatus.RECONCILED, AuditAction.RECONCILE)
    }

    override suspend fun unreconcileTransaction(id: UUID) {
        changeStatus(id, TransactionStatus.CLEARED, AuditAction.UNRECONCILE)
    }

    private suspend fun changeStatus(id: UUID, target: TransactionStatus, action: AuditAction) = database.withTransaction {
        val existing = transactionDao.getById(id) ?: return@withTransaction
        val legs = existing.transferId?.let { transactionDao.getByTransferId(it) } ?: listOf(existing)
        require(legs.all { it.status != TransactionStatus.PENDING }) { "Pending transactions cannot be reconciled" }
        legs.forEach { leg ->
            transactionDao.update(leg.copy(status = target, updatedAt = Instant.now()))
            auditRepository.recordEvent(AuditEvent(
                entityType = "TRANSACTION", entityId = leg.id, action = action,
                beforeState = "status=${leg.status}", afterState = "status=$target"
            ))
        }
    }

    private suspend fun requireActiveAccount(id: UUID): com.samuelbaldasso.financeflow.core.database.entity.AccountEntity {
        val account = requireNotNull(database.accountDao().getById(id)) { "Account $id not found" }
        if (account.isArchived) throw com.samuelbaldasso.financeflow.core.model.error.DomainException.AccountArchivedException(
            "Cannot add or update transactions in archived account ${account.name}"
        )
        return account
    }
}
