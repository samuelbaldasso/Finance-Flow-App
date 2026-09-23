package com.samuelbaldasso.financeflow.domain.repository

import com.samuelbaldasso.financeflow.core.model.audit.AuditEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface AuditRepository {
    suspend fun recordEvent(event: AuditEvent)
    fun getLogsForEntityFlow(entityType: String, entityId: UUID): Flow<List<AuditEvent>>
    fun getRecentLogsFlow(limit: Int = 100): Flow<List<AuditEvent>>
}
