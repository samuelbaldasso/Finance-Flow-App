package com.samuelbaldasso.financeflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samuelbaldasso.financeflow.core.model.goal.Goal
import com.samuelbaldasso.financeflow.core.model.goal.GoalStatus
import com.samuelbaldasso.financeflow.core.model.money.Money
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linked_account_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["linked_account_id"])]
)
data class GoalEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    @ColumnInfo(name = "target_minor") val targetMinor: Long,
    @ColumnInfo(name = "target_date") val targetDate: Instant? = null,
    @ColumnInfo(name = "linked_account_id") val linkedAccountId: UUID? = null,
    val status: GoalStatus = GoalStatus.IN_PROGRESS,
    @ColumnInfo(name = "created_at") val createdAt: Instant
) {
    fun toDomain(): Goal = Goal(
        id = id,
        name = name,
        targetAmount = Money(targetMinor),
        targetDate = targetDate,
        linkedAccountId = linkedAccountId,
        status = status,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(goal: Goal): GoalEntity = GoalEntity(
            id = goal.id,
            name = goal.name,
            targetMinor = goal.targetAmount.amountMinor,
            targetDate = goal.targetDate,
            linkedAccountId = goal.linkedAccountId,
            status = goal.status,
            createdAt = goal.createdAt
        )
    }
}
