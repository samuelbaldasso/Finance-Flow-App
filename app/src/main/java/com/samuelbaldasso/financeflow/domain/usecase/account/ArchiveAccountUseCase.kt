package com.samuelbaldasso.financeflow.domain.usecase.account

import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import java.util.UUID

class ArchiveAccountUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(accountId: UUID) {
        accountRepository.archiveAccount(accountId)
    }
}
