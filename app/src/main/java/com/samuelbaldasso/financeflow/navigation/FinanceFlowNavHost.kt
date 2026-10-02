package com.samuelbaldasso.financeflow.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.paging.compose.collectAsLazyPagingItems
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
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AccountsRoute,
        modifier = modifier.padding(innerPadding)
    ) {
        composable<AccountsRoute> {
            val accountsViewModel: AccountsViewModel = hiltViewModel()
            val state by accountsViewModel.uiState.collectAsStateWithLifecycle()
            AccountsScreen(
                state = state,
                onEvent = accountsViewModel::onEvent
            )
        }

        composable<TransactionsRoute> {
            val transactionsViewModel: TransactionsViewModel = hiltViewModel()
            val allTxs by transactionsViewModel.allTransactions.collectAsStateWithLifecycle()
            val pagedTxs = transactionsViewModel.pagedTransactions.collectAsLazyPagingItems()

            TransactionsListScreen(
                transactions = allTxs,
                pagedTransactions = pagedTxs,
                onAddTransactionClick = { navController.navigate(NewTransactionRoute()) },
                onOpenReportsClick = { navController.navigate(ReportsRoute) }
            )
        }

        composable<BudgetsRoute> {
            val budgetsViewModel: BudgetsViewModel = hiltViewModel()
            val state by budgetsViewModel.uiState.collectAsStateWithLifecycle()
            BudgetsScreen(
                state = state,
                onEvent = budgetsViewModel::onEvent
            )
        }

        composable<GoalsRoute> {
            val goalsViewModel: GoalsViewModel = hiltViewModel()
            val state by goalsViewModel.uiState.collectAsStateWithLifecycle()
            GoalsScreen(
                state = state,
                onEvent = goalsViewModel::onEvent
            )
        }

        composable<CardsRoute> {
            val cardsViewModel: CardsViewModel = hiltViewModel()
            val state by cardsViewModel.uiState.collectAsStateWithLifecycle()
            CardsScreen(
                state = state,
                onEvent = cardsViewModel::onEvent,
                onNavigateToAccounts = { navController.navigate(AccountsRoute) }
            )
        }

        composable<ReportsRoute> {
            val reportsViewModel: ReportsViewModel = hiltViewModel()
            val state by reportsViewModel.uiState.collectAsStateWithLifecycle()
            ReportsScreen(
                state = state,
                onEvent = reportsViewModel::onEvent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state,
                onEvent = settingsViewModel::onEvent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NewTransactionRoute> {
            val transactionEntryViewModel: TransactionEntryViewModel = hiltViewModel()
            val state by transactionEntryViewModel.uiState.collectAsStateWithLifecycle()

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
