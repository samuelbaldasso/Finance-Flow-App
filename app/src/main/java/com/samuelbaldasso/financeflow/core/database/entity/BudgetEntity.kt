package com.samuelbaldasso.financeflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samuelbaldasso.financeflow.core.model.budget.Budget
import com.samuelbaldasso.financeflow.core.model.money.Money
import java.util.UUID

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category_id", "period_month", "period_year"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "category_id") val categoryId: UUID,
    @ColumnInfo(name = "period_month") val periodMonth: Int,
    @ColumnInfo(name = "period_year") val periodYear: Int,
    @ColumnInfo(name = "limit_minor") val limitMinor: Long,
    @ColumnInfo(name = "rollover_enabled") val rolloverEnabled: Boolean = false,
    @ColumnInfo(name = "previous_rollover_minor") val previousRolloverMinor: Long = 0L
) {
    fun toDomain(): Budget = Budget(
        id = id,
        categoryId = categoryId,
        periodMonth = periodMonth,
        periodYear = periodYear,
        limitAmount = Money(limitMinor),
        rolloverEnabled = rolloverEnabled,
        previousRolloverAmount = Money(previousRolloverMinor)
    )

    companion object {
        fun fromDomain(budget: Budget): BudgetEntity = BudgetEntity(
            id = budget.id,
            categoryId = budget.categoryId,
            periodMonth = budget.periodMonth,
            periodYear = budget.periodYear,
            limitMinor = budget.limitAmount.amountMinor,
            rolloverEnabled = budget.rolloverEnabled,
            previousRolloverMinor = budget.previousRolloverAmount.amountMinor
        )
    }
}
