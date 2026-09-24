package com.samuelbaldasso.financeflow.domain.usecase.security

import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WipeAllUserDataUseCase @Inject constructor(
    private val database: FinanceFlowDatabase,
    private val securityRepository: SecurityRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        securityRepository.clearSecuritySettings()
        categoryRepository.seedDefaultCategoriesIfNeeded()
    }
}
