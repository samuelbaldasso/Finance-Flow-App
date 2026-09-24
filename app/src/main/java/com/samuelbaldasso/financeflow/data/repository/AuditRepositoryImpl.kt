package com.samuelbaldasso.financeflow.data.repository

import com.samuelbaldasso.financeflow.core.database.dao.AuditLogDao
import com.samuelbaldasso.financeflow.core.database.entity.AuditLogEntity
import com.samuelbaldasso.financeflow.core.model.audit.AuditEvent
import com.samuelbaldasso.financeflow.domain.repository.AuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.util.UUID

class AuditRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao
) : AuditRepository {

    override suspend fun recordEvent(event: AuditEvent) {
        auditLogDao.insert(AuditLogEntity.fromDomain(event))
    }

    override fun getLogsForEntityFlow(entityType: String, entityId: UUID): Flow<List<AuditEvent>> {
        return auditLogDao.getLogsForEntityFlow(entityType, entityId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecentLogsFlow(limit: Int): Flow<List<AuditEvent>> {
        return auditLogDao.getRecentLogsFlow(limit).map { list ->
            list.map { it.toDomain() }
        }
    }
}
