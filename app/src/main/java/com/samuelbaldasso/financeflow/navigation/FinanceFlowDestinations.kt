package com.samuelbaldasso.financeflow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
data object AccountsRoute

@Serializable
data object TransactionsRoute

@Serializable
data object BudgetsRoute

@Serializable
data object GoalsRoute

@Serializable
data object CardsRoute

@Serializable
data object ReportsRoute

@Serializable
data object SettingsRoute

@Serializable
data class NewTransactionRoute(
    val preselectedAccountId: String? = null
)

enum class TopLevelDestination(
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelText: String
) {
    ACCOUNTS(
        route = AccountsRoute,
        selectedIcon = Icons.Default.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance,
        labelText = "Contas"
    ),
    TRANSACTIONS(
        route = TransactionsRoute,
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        labelText = "Extrato"
    ),
    BUDGETS(
        route = BudgetsRoute,
        selectedIcon = Icons.Default.PieChart,
        unselectedIcon = Icons.Outlined.PieChart,
        labelText = "Orçamento"
    ),
    GOALS(
        route = GoalsRoute,
        selectedIcon = Icons.Default.Flag,
        unselectedIcon = Icons.Outlined.Flag,
        labelText = "Metas"
    ),
    CARDS(
        route = CardsRoute,
        selectedIcon = Icons.Default.CreditCard,
        unselectedIcon = Icons.Outlined.CreditCard,
        labelText = "Cartões"
    ),
    SETTINGS(
        route = SettingsRoute,
        selectedIcon = Icons.Default.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelText = "Ajustes"
    )
}
