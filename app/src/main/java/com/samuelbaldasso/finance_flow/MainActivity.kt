package com.samuelbaldasso.finance_flow

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import com.samuelbaldasso.financeflow.FinanceFlowApplication
import com.samuelbaldasso.financeflow.StartupState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samuelbaldasso.financeflow.core.model.settings.SecuritySettings
import com.samuelbaldasso.financeflow.designsystem.theme.FinanceFlowTheme
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository
import com.samuelbaldasso.financeflow.domain.security.AppLockManager
import com.samuelbaldasso.financeflow.feature.security.LockScreen
import com.samuelbaldasso.financeflow.navigation.FinanceFlowNavHost
import com.samuelbaldasso.financeflow.navigation.TopLevelDestination
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var securityRepository: SecurityRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FinanceFlowTheme {
                val app = application as FinanceFlowApplication
                val startupState by app.startupState.collectAsState()
                if (startupState != StartupState.READY) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (startupState == StartupState.ERROR) {
                                Text("Não foi possível preparar seus dados.")
                                Button(onClick = app::initialize) { Text("Tentar novamente") }
                            } else {
                                CircularProgressIndicator()
                                Text("Preparando seu FinanceFlow")
                            }
                        }
                    }
                    return@FinanceFlowTheme
                }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val isLocked by appLockManager.isLocked.collectAsState()
                val securitySettings by securityRepository.securitySettingsFlow
                    .collectAsState(initial = SecuritySettings())

                // Manage FLAG_SECURE (Screenshot protection)
                LaunchedEffect(securitySettings.isScreenshotProtectionEnabled) {
                    if (securitySettings.isScreenshotProtectionEnabled) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                if (isLocked) {
                    LockScreen(
                        isBiometricAvailable = securitySettings.isBiometricEnabled,
                        onVerifyPin = { pin -> securityRepository.verifyPin(pin) },
                        onBiometricUnlockClick = {
                            showBiometricPrompt(
                                onSuccess = { appLockManager.unlock() },
                                onError = { /* fallback to pin */ }
                            )
                        },
                        onUnlockSuccess = { appLockManager.unlock() }
                    )
                } else {
                    val isTopLevelRoute = TopLevelDestination.entries.any { topDest ->
                        currentDestination?.hierarchy?.any { it.hasRoute(topDest.route::class) } == true
                    }

                    Scaffold(
                        bottomBar = {
                            if (isTopLevelRoute || currentDestination == null) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp
                                ) {
                                    TopLevelDestination.entries.forEach { topDest ->
                                        val isSelected = currentDestination?.hierarchy?.any {
                                            it.hasRoute(topDest.route::class)
                                        } == true

                                        NavigationBarItem(
                                            selected = isSelected,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            onClick = {
                                                navController.navigate(topDest.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) topDest.selectedIcon else topDest.unselectedIcon,
                                                    contentDescription = topDest.labelText,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = topDest.labelText,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        FinanceFlowNavHost(
                            navController = navController,
                            innerPadding = innerPadding
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        appLockManager.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val settings = securityRepository.securitySettingsFlow.first()
            appLockManager.onAppForegrounded(settings)
        }
    }

    private fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear FinanceFlow")
            .setSubtitle("Acesse seus dados financeiros")
            .setNegativeButtonText("Usar PIN")
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo)
    }
}