package com.samuelbaldasso.financeflow.domain.usecase.account

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository

class CreateAccountUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account): Account {
        return accountRepository.createAccount(account)
    }
}
