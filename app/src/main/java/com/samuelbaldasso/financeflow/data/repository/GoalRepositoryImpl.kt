package com.samuelbaldasso.financeflow.data.repository

import com.samuelbaldasso.financeflow.core.database.dao.GoalDao
import com.samuelbaldasso.financeflow.core.database.dao.TransactionDao
import com.samuelbaldasso.financeflow.core.database.entity.GoalEntity
import com.samuelbaldasso.financeflow.core.model.goal.Goal
import com.samuelbaldasso.financeflow.core.model.goal.GoalStatus
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.repository.GoalRepository
import com.samuelbaldasso.financeflow.domain.repository.GoalWithProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val transactionDao: TransactionDao
) : GoalRepository {

    override fun getAllGoalsFlow(): Flow<List<Goal>> {
        return goalDao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    override fun getGoalsWithProgressFlow(): Flow<List<GoalWithProgress>> {
        return goalDao.getAllFlow().flatMapLatest { goalEntities ->
            if (goalEntities.isEmpty()) {
                flowOf(emptyList())
            } else {
                val goalFlows = goalEntities.map { entity ->
                    val goal = entity.toDomain()
                    transactionDao.getGoalContributionsSumMinorFlow(goal.id).map { savedMinor ->
                        val saved = Money(savedMinor)
                        val isReached = goal.isTargetReached(saved)
                        val effectiveGoal = if (isReached && goal.status == GoalStatus.IN_PROGRESS) {
                            goal.copy(status = GoalStatus.COMPLETED)
                        } else goal

                        GoalWithProgress(
                            goal = effectiveGoal,
                            currentSavedAmount = saved,
                            progressPercentage = goal.progressPercentage(saved),
                            isCompleted = isReached
                        )
                    }
                }
                combine(goalFlows) { it.toList() }
            }
        }
    }

    override suspend fun getGoalById(id: UUID): Goal? {
        return goalDao.getById(id)?.toDomain()
    }

    override suspend fun createGoal(goal: Goal): Goal {
        goalDao.insert(GoalEntity.fromDomain(goal))
        return goal
    }

    override suspend fun updateGoal(goal: Goal) {
        goalDao.update(GoalEntity.fromDomain(goal))
    }

    override suspend fun deleteGoal(id: UUID) {
        val existing = goalDao.getById(id) ?: return
        goalDao.delete(existing)
    }
}
