package com.samuelbaldasso.finance_flow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samuelbaldasso.financeflow.FinanceFlowApplication
import com.samuelbaldasso.financeflow.designsystem.theme.FinanceFlowTheme
import com.samuelbaldasso.financeflow.feature.accounts.AccountsScreen
import com.samuelbaldasso.financeflow.feature.accounts.AccountsViewModel
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryScreen
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryUiEffect
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryViewModel
import com.samuelbaldasso.financeflow.feature.transactions.TransactionsListScreen

enum class AppDestination {
    ACCOUNTS,
    TRANSACTIONS,
    NEW_TRANSACTION
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as FinanceFlowApplication).container

        setContent {
            FinanceFlowTheme {
                var currentDestination by remember { mutableStateOf(AppDestination.ACCOUNTS) }

                val accountsViewModel: AccountsViewModel = viewModel {
                    AccountsViewModel(
                        container.getAccountsWithBalanceUseCase,
                        container.createAccountUseCase,
                        container.archiveAccountUseCase
                    )
                }

                val transactionEntryViewModel: TransactionEntryViewModel = viewModel {
                    TransactionEntryViewModel(
                        container.accountRepository,
                        container.categoryRepository,
                        container.createTransactionUseCase
                    )
                }

                val accountsState by accountsViewModel.uiState.collectAsState()
                val transactionEntryState by transactionEntryViewModel.uiState.collectAsState()
                val allTransactions by container.transactionRepository.getAllTransactionsFlow().collectAsState(initial = emptyList())

                LaunchedEffect(Unit) {
                    transactionEntryViewModel.effect.collect { effect ->
                        when (effect) {
                            TransactionEntryUiEffect.TransactionSaved -> {
                                currentDestination = AppDestination.TRANSACTIONS
                            }
                            is TransactionEntryUiEffect.ShowError -> {
                                // Handled via state.errorMessage
                            }
                        }
                    }
                }

                if (currentDestination == AppDestination.NEW_TRANSACTION) {
                    TransactionEntryScreen(
                        state = transactionEntryState,
                        onEvent = transactionEntryViewModel::onEvent,
                        onNavigateBack = { currentDestination = AppDestination.ACCOUNTS }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentDestination == AppDestination.ACCOUNTS,
                                    onClick = { currentDestination = AppDestination.ACCOUNTS },
                                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                                    label = { Text("Contas") }
                                )
                                NavigationBarItem(
                                    selected = currentDestination == AppDestination.TRANSACTIONS,
                                    onClick = { currentDestination = AppDestination.TRANSACTIONS },
                                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                                    label = { Text("Extrato") }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        when (currentDestination) {
                            AppDestination.ACCOUNTS -> {
                                AccountsScreen(
                                    state = accountsState,
                                    onEvent = accountsViewModel::onEvent,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            AppDestination.TRANSACTIONS -> {
                                TransactionsListScreen(
                                    transactions = allTransactions,
                                    onAddTransactionClick = { currentDestination = AppDestination.NEW_TRANSACTION },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            AppDestination.NEW_TRANSACTION -> {}
                        }
                    }
                }
            }
        }
    }
}