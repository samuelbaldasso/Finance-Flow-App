package com.samuelbaldasso.financeflow.domain.usecase.security

import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class WipeAllUserDataUseCase @Inject constructor(
    private val database: FinanceFlowDatabase,
    private val securityRepository: SecurityRepository,
    private val categoryRepository: CategoryRepository
) {
    private val mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        withContext(Dispatchers.IO) {
            // The marker survives process death or a failed database/preferences write.
            // Startup retries before exposing the main app. Room and DataStore cannot
            // participate in a single transaction.
            securityRepository.markWipePending()
            database.clearAllTables()
            categoryRepository.seedDefaultCategoriesIfNeeded()
            securityRepository.clearSecuritySettings()
        }
    }
}
