package com.samuelbaldasso.financeflow.core.model.audit

import java.time.Instant
import java.util.UUID

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    RECONCILE,
    UNRECONCILE
}

data class AuditEvent(
    val id: UUID = UUID.randomUUID(),
    val entityType: String,
    val entityId: UUID,
    val action: AuditAction,
    val actor: String = "local_user",
    val beforeState: String? = null,
    val afterState: String? = null,
    val timestamp: Instant = Instant.now()
)
