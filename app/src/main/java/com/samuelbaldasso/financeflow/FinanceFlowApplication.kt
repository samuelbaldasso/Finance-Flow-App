package com.samuelbaldasso.financeflow

import android.app.Application
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository
import com.samuelbaldasso.financeflow.domain.usecase.security.WipeAllUserDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException

enum class StartupState { LOADING, READY, ERROR }

@HiltAndroidApp
class FinanceFlowApplication : Application() {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject lateinit var securityRepository: SecurityRepository
    @Inject lateinit var wipeAllUserDataUseCase: WipeAllUserDataUseCase

    private val _startupState = MutableStateFlow(StartupState.LOADING)
    val startupState = _startupState.asStateFlow()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        initialize()
    }
    fun initialize() {
        _startupState.value = StartupState.LOADING
        applicationScope.launch {
            try {
                if (securityRepository.isWipePending()) wipeAllUserDataUseCase()
                else categoryRepository.seedDefaultCategoriesIfNeeded()
                _startupState.value = StartupState.READY
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _startupState.value = StartupState.ERROR
            }
        }
    }
}
