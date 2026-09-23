package com.samuelbaldasso.financeflow.data.repository

import com.samuelbaldasso.financeflow.core.database.dao.TransactionDao
import com.samuelbaldasso.financeflow.core.database.entity.TransactionEntity
import com.samuelbaldasso.financeflow.core.model.audit.AuditAction
import com.samuelbaldasso.financeflow.core.model.audit.AuditEvent
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.domain.repository.AuditRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val auditRepository: AuditRepository
) : TransactionRepository {

    override fun getAllTransactionsFlow(): Flow<List<Transaction>> {
        return transactionDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByAccountFlow(accountId: UUID): Flow<List<Transaction>> {
        return transactionDao.getByAccountFlow(accountId).map { list -> list.map { it.toDomain() } }
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

    override suspend fun createTransaction(transaction: Transaction): Transaction {
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
        return transaction
    }

    override suspend fun createTransfer(debitTx: Transaction, creditTx: Transaction) {
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

    override suspend fun updateTransaction(transaction: Transaction) {
        val existing = transactionDao.getById(transaction.id)?.toDomain()
            ?: throw IllegalArgumentException("Transaction ${transaction.id} not found")

        existing.validateModificationAllowed()
        transaction.validateFutureDate(Instant.now())

        val transferId = transaction.transferId
        if (transaction.isTransfer && transferId != null) {
            val transferLegs = transactionDao.getByTransferId(transferId)
            val otherLeg = transferLegs.firstOrNull { it.id != transaction.id }
            if (otherLeg != null) {
                val updatedOtherLeg = otherLeg.copy(
                    amountMinor = transaction.amount.amountMinor,
                    competenceDate = transaction.competenceDate,
                    effectiveDate = transaction.effectiveDate,
                    updatedAt = Instant.now()
                )
                transactionDao.updateAtomicTransfer(
                    TransactionEntity.fromDomain(transaction),
                    updatedOtherLeg
                )
            } else {
                transactionDao.update(TransactionEntity.fromDomain(transaction))
            }
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

    override suspend fun deleteTransaction(id: UUID) {
        val existing = transactionDao.getById(id)?.toDomain() ?: return
        existing.validateModificationAllowed()

        val transferId = existing.transferId
        if (existing.isTransfer && transferId != null) {
            transactionDao.deleteByTransferId(transferId)
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
        val existing = transactionDao.getById(id)?.toDomain() ?: return
        val reconciled = existing.copy(status = TransactionStatus.RECONCILED, updatedAt = Instant.now())
        transactionDao.update(TransactionEntity.fromDomain(reconciled))
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "TRANSACTION",
                entityId = id,
                action = AuditAction.RECONCILE,
                beforeState = "status=${existing.status}",
                afterState = "status=RECONCILED"
            )
        )
    }

    override suspend fun unreconcileTransaction(id: UUID) {
        val existing = transactionDao.getById(id)?.toDomain() ?: return
        val unreconciled = existing.copy(status = TransactionStatus.CLEARED, updatedAt = Instant.now())
        transactionDao.update(TransactionEntity.fromDomain(unreconciled))
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "TRANSACTION",
                entityId = id,
                action = AuditAction.UNRECONCILE,
                beforeState = "status=RECONCILED",
                afterState = "status=CLEARED"
            )
        )
    }
}
