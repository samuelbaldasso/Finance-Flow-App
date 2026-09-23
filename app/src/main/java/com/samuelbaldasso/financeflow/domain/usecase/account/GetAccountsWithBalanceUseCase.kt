package com.samuelbaldasso.financeflow.domain.usecase.account

import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.AccountWithBalance
import kotlinx.coroutines.flow.Flow

class GetAccountsWithBalanceUseCase(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): Flow<List<AccountWithBalance>> {
        return accountRepository.getAccountsWithDerivedBalanceFlow()
    }
}
