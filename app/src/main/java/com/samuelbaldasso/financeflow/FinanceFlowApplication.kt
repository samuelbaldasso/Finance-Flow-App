package com.samuelbaldasso.financeflow

import android.app.Application
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FinanceFlowApplication : Application() {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Seed default system categories if first run
        applicationScope.launch {
            categoryRepository.seedDefaultCategoriesIfNeeded()
        }
    }
}
