package com.samuelbaldasso.financeflow.domain.usecase.account

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account): Account {
        return accountRepository.createAccount(account)
    }
}
