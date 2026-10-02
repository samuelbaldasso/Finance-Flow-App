package com.samuelbaldasso.financeflow.data.repository

import com.samuelbaldasso.financeflow.core.database.dao.AccountDao
import com.samuelbaldasso.financeflow.core.database.dao.TransactionDao
import com.samuelbaldasso.financeflow.core.database.entity.AccountEntity
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.audit.AuditAction
import com.samuelbaldasso.financeflow.core.model.audit.AuditEvent
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.AccountWithBalance
import com.samuelbaldasso.financeflow.domain.repository.AuditRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import androidx.room.withTransaction
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val auditRepository: AuditRepository,
    private val database: FinanceFlowDatabase
) : AccountRepository {

    override fun getAllAccountsFlow(): Flow<List<Account>> {
        return accountDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveAccountsFlow(): Flow<List<Account>> {
        return accountDao.getActiveAccountsFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getAccountsWithDerivedBalanceFlow(): Flow<List<AccountWithBalance>> {
        return accountDao.getAllFlow().flatMapLatest { accounts ->
            if (accounts.isEmpty()) {
                flowOf(emptyList())
            } else {
                val balanceFlows = accounts.map { accountEntity ->
                    transactionDao.getDerivedBalanceSumMinorFlow(accountEntity.id).map { sumMinor ->
                        val derivedMinor = Math.addExact(accountEntity.initialBalanceMinor, sumMinor)
                        AccountWithBalance(
                            account = accountEntity.toDomain(),
                            derivedBalance = Money(derivedMinor)
                        )
                    }
                }
                combine(balanceFlows) { it.toList() }
            }
        }
    }

    override suspend fun getAccountById(id: UUID): Account? {
        return accountDao.getById(id)?.toDomain()
    }

    override suspend fun createAccount(account: Account): Account = database.withTransaction {
        accountDao.insert(AccountEntity.fromDomain(account))
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "ACCOUNT",
                entityId = account.id,
                action = AuditAction.CREATE,
                afterState = "name=${account.name}, type=${account.type}, initial=${account.initialBalance.amountMinor}"
            )
        )
        return@withTransaction account
    }

    override suspend fun updateAccount(account: Account) = database.withTransaction {
        val before = accountDao.getById(account.id)
        accountDao.update(AccountEntity.fromDomain(account.copy(updatedAt = Instant.now())))
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "ACCOUNT",
                entityId = account.id,
                action = AuditAction.UPDATE,
                beforeState = before?.let { "name=${it.name}, archived=${it.isArchived}" },
                afterState = "name=${account.name}, archived=${account.isArchived}"
            )
        )
    }

    override suspend fun archiveAccount(id: UUID) = database.withTransaction {
        val before = accountDao.getById(id)
        accountDao.softDelete(id, System.currentTimeMillis())
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "ACCOUNT",
                entityId = id,
                action = AuditAction.UPDATE,
                beforeState = before?.let { "isArchived=${it.isArchived}" },
                afterState = "isArchived=true"
            )
        )
    }

    override suspend fun unarchiveAccount(id: UUID) = database.withTransaction {
        accountDao.unarchive(id, System.currentTimeMillis())
        auditRepository.recordEvent(
            AuditEvent(
                entityType = "ACCOUNT",
                entityId = id,
                action = AuditAction.UPDATE,
                afterState = "isArchived=false"
            )
        )
    }
}
