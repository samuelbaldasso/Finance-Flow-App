package com.samuelbaldasso.financeflow.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samuelbaldasso.financeflow.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND period_month = :month AND period_year = :year")
    suspend fun getForCategoryAndPeriod(categoryId: UUID, month: Int, year: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE period_month = :month AND period_year = :year")
    fun getAllForPeriodFlow(month: Int, year: Int): Flow<List<BudgetEntity>>
}
