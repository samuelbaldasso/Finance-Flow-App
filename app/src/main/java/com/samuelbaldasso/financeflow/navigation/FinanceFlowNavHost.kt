package com.samuelbaldasso.financeflow.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.paging.compose.collectAsLazyPagingItems
import com.samuelbaldasso.financeflow.di.AppContainer
import com.samuelbaldasso.financeflow.feature.accounts.AccountsScreen
import com.samuelbaldasso.financeflow.feature.accounts.AccountsViewModel
import com.samuelbaldasso.financeflow.feature.budgets.BudgetsScreen
import com.samuelbaldasso.financeflow.feature.budgets.BudgetsViewModel
import com.samuelbaldasso.financeflow.feature.cards.CardsScreen
import com.samuelbaldasso.financeflow.feature.cards.CardsViewModel
import com.samuelbaldasso.financeflow.feature.goals.GoalsScreen
import com.samuelbaldasso.financeflow.feature.goals.GoalsViewModel
import com.samuelbaldasso.financeflow.feature.reports.ReportsScreen
import com.samuelbaldasso.financeflow.feature.reports.ReportsViewModel
import com.samuelbaldasso.financeflow.feature.settings.SettingsScreen
import com.samuelbaldasso.financeflow.feature.settings.SettingsViewModel
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryScreen
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryUiEffect
import com.samuelbaldasso.financeflow.feature.transactions.TransactionEntryViewModel
import com.samuelbaldasso.financeflow.feature.transactions.TransactionsListScreen
import com.samuelbaldasso.financeflow.feature.transactions.TransactionsViewModel

@Composable
fun FinanceFlowNavHost(
    navController: NavHostController,
    container: AppContainer,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AccountsRoute,
        modifier = modifier.padding(innerPadding)
    ) {
        composable<AccountsRoute> {
            val accountsViewModel: AccountsViewModel = viewModel {
                AccountsViewModel(
                    container.getAccountsWithBalanceUseCase,
                    container.createAccountUseCase,
                    container.archiveAccountUseCase
                )
            }
            val state by accountsViewModel.uiState.collectAsState()
            AccountsScreen(
                state = state,
                onEvent = accountsViewModel::onEvent
            )
        }

        composable<TransactionsRoute> {
            val transactionsViewModel: TransactionsViewModel = viewModel {
                TransactionsViewModel(container.transactionRepository)
            }
            val allTxs by transactionsViewModel.allTransactions.collectAsState()
            val pagedTxs = transactionsViewModel.pagedTransactions.collectAsLazyPagingItems()

            TransactionsListScreen(
                transactions = allTxs,
                pagedTransactions = pagedTxs,
                onAddTransactionClick = { navController.navigate(NewTransactionRoute()) },
                onOpenReportsClick = { navController.navigate(ReportsRoute) }
            )
        }

        composable<BudgetsRoute> {
            val budgetsViewModel: BudgetsViewModel = viewModel {
                BudgetsViewModel(container.budgetRepository, container.categoryRepository)
            }
            val state by budgetsViewModel.uiState.collectAsState()
            BudgetsScreen(
                state = state,
                onEvent = budgetsViewModel::onEvent
            )
        }

        composable<GoalsRoute> {
            val goalsViewModel: GoalsViewModel = viewModel {
                GoalsViewModel(container.goalRepository, container.accountRepository, container.createTransactionUseCase)
            }
            val state by goalsViewModel.uiState.collectAsState()
            GoalsScreen(
                state = state,
                onEvent = goalsViewModel::onEvent
            )
        }

        composable<CardsRoute> {
            val cardsViewModel: CardsViewModel = viewModel {
                CardsViewModel(
                    container.accountRepository,
                    container.transactionRepository,
                    container.categoryRepository,
                    container.calculateAvailableLimitUseCase,
                    container.createInstallmentPurchaseUseCase,
                    container.createTransactionUseCase
                )
            }
            val state by cardsViewModel.uiState.collectAsState()
            CardsScreen(
                state = state,
                onEvent = cardsViewModel::onEvent,
                onNavigateToAccounts = { navController.navigate(AccountsRoute) }
            )
        }

        composable<ReportsRoute> {
            val reportsViewModel: ReportsViewModel = viewModel {
                ReportsViewModel(
                    container.getCashFlowReportUseCase,
                    container.transactionRepository,
                    container.accountRepository,
                    container.createTransactionUseCase
                )
            }
            val state by reportsViewModel.uiState.collectAsState()
            ReportsScreen(
                state = state,
                onEvent = reportsViewModel::onEvent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            val settingsViewModel: SettingsViewModel = viewModel {
                SettingsViewModel(
                    container.securityRepository,
                    container.exportAllUserDataUseCase,
                    container.wipeAllUserDataUseCase,
                    container.appLockManager
                )
            }
            val state by settingsViewModel.uiState.collectAsState()
            SettingsScreen(
                state = state,
                onEvent = settingsViewModel::onEvent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NewTransactionRoute> {
            val transactionEntryViewModel: TransactionEntryViewModel = viewModel {
                TransactionEntryViewModel(
                    container.accountRepository,
                    container.categoryRepository,
                    container.createTransactionUseCase
                )
            }
            val state by transactionEntryViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                transactionEntryViewModel.effect.collect { effect ->
                    when (effect) {
                        TransactionEntryUiEffect.TransactionSaved -> {
                            navController.popBackStack()
                        }
                        is TransactionEntryUiEffect.ShowError -> {}
                    }
                }
            }

            TransactionEntryScreen(
                state = state,
                onEvent = transactionEntryViewModel::onEvent,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
