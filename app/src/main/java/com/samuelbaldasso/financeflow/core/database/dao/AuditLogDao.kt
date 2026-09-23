package com.samuelbaldasso.financeflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samuelbaldasso.financeflow.core.database.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY timestamp DESC")
    fun getLogsForEntityFlow(entityType: String, entityId: UUID): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int = 100): Flow<List<AuditLogEntity>>
}
