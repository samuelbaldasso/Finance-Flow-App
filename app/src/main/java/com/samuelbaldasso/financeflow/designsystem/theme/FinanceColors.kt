package com.samuelbaldasso.financeflow.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val FinanceGreenPrimary = Color(0xFF059669)
val FinanceGreenDark = Color(0xFF34D399)

val FinanceRedExpense = Color(0xFFE11D48)
val FinanceRedExpenseDark = Color(0xFFFB7185)

val FinanceWarningAmber = Color(0xFFD97706)
val FinanceWarningAmberDark = Color(0xFFFBBF24)

val FinanceTransferBlue = Color(0xFF2563EB)
val FinanceTransferBlueDark = Color(0xFF60A5FA)

val NeutralDark = Color(0xFF0F172A)
val NeutralLight = Color(0xFFF8FAFC)

val BorderSubtleLight = Color(0xFFE2E8F0)
val BorderSubtleDark = Color(0xFF334155)

// Gradients for Hero Cards & Visual Credit Cards
val EmeraldHeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF065F46), Color(0xFF042F2E))
)

val MidnightCreditCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
)

val PurpleCreditCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4C1D95), Color(0xFF2E1065))
)

val SapphireCreditCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1E3A8A), Color(0xFF172554))
)

val GoldGoalGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFB45309), Color(0xFF78350F))
)

@Immutable
data class FinancialSemanticColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val cardBackground: Color,
    val cardBorder: Color
)

val LightFinancialSemanticColors = FinancialSemanticColors(
    income = Color(0xFF059669),
    incomeContainer = Color(0xFFECFDF5),
    expense = Color(0xFFE11D48),
    expenseContainer = Color(0xFFFFF1F2),
    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFFFFBEB),
    transfer = Color(0xFF2563EB),
    transferContainer = Color(0xFFEFF6FF),
    cardBackground = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE2E8F0)
)

val DarkFinancialSemanticColors = FinancialSemanticColors(
    income = Color(0xFF34D399),
    incomeContainer = Color(0xFF064E3B),
    expense = Color(0xFFFB7185),
    expenseContainer = Color(0xFF4C0519),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF451A03),
    transfer = Color(0xFF60A5FA),
    transferContainer = Color(0xFF172554),
    cardBackground = Color(0xFF1E293B),
    cardBorder = Color(0xFF334155)
)

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialSemanticColors }
