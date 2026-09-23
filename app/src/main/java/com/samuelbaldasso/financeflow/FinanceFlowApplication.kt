package com.samuelbaldasso.financeflow

import android.app.Application
import com.samuelbaldasso.financeflow.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinanceFlowApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Seed default system categories if first run
        applicationScope.launch {
            container.categoryRepository.seedDefaultCategoriesIfNeeded()
        }
    }
}
