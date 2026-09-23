package com.samuelbaldasso.financeflow.domain.repository

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.money.Money
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class AccountWithBalance(
    val account: Account,
    val derivedBalance: Money
)

interface AccountRepository {
    fun getAllAccountsFlow(): Flow<List<Account>>
    fun getActiveAccountsFlow(): Flow<List<Account>>
    fun getAccountsWithDerivedBalanceFlow(): Flow<List<AccountWithBalance>>
    suspend fun getAccountById(id: UUID): Account?
    suspend fun createAccount(account: Account): Account
    suspend fun updateAccount(account: Account)
    suspend fun archiveAccount(id: UUID)
    suspend fun unarchiveAccount(id: UUID)
}
