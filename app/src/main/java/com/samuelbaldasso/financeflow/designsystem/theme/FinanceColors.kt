package com.samuelbaldasso.financeflow.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val FinanceGreenPrimary = Color(0xFF00875A)
val FinanceGreenDark = Color(0xFF36B37E)

val FinanceRedExpense = Color(0xFFDE350B)
val FinanceRedExpenseDark = Color(0xFFFF5630)

val FinanceWarningAmber = Color(0xFFFF8B00)
val FinanceWarningAmberDark = Color(0xFFFFAB00)

val FinanceTransferBlue = Color(0xFF0052CC)
val FinanceTransferBlueDark = Color(0xFF2684FF)

val NeutralDark = Color(0xFF172B4D)
val NeutralLight = Color(0xFFF4F5F7)

@Immutable
data class FinancialSemanticColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val transfer: Color,
    val transferContainer: Color
)

val LightFinancialSemanticColors = FinancialSemanticColors(
    income = Color(0xFF00875A),
    incomeContainer = Color(0xFFE3FCEF),
    expense = Color(0xFFDE350B),
    expenseContainer = Color(0xFFFFEBE6),
    warning = Color(0xFFFF8B00),
    warningContainer = Color(0xFFFFF0B3),
    transfer = Color(0xFF0052CC),
    transferContainer = Color(0xFFDEEBFF)
)

val DarkFinancialSemanticColors = FinancialSemanticColors(
    income = Color(0xFF36B37E),
    incomeContainer = Color(0xFF004D40),
    expense = Color(0xFFFF5630),
    expenseContainer = Color(0xFF4A1009),
    warning = Color(0xFFFFAB00),
    warningContainer = Color(0xFF4D3800),
    transfer = Color(0xFF2684FF),
    transferContainer = Color(0xFF072B61)
)

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialSemanticColors }
