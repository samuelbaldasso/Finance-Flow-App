package com.samuelbaldasso.financeflow.domain.repository

import com.samuelbaldasso.financeflow.core.model.goal.Goal
import com.samuelbaldasso.financeflow.core.model.money.Money
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class GoalWithProgress(
    val goal: Goal,
    val currentSavedAmount: Money,
    val progressPercentage: Float,
    val isCompleted: Boolean
)

interface GoalRepository {
    fun getAllGoalsFlow(): Flow<List<Goal>>
    fun getGoalsWithProgressFlow(): Flow<List<GoalWithProgress>>
    suspend fun getGoalById(id: UUID): Goal?
    suspend fun createGoal(goal: Goal): Goal
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(id: UUID)
}
