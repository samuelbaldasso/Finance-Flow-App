package com.samuelbaldasso.financeflow.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samuelbaldasso.financeflow.core.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: UUID): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<GoalEntity>>
}
