package com.samuelbaldasso.financeflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samuelbaldasso.financeflow.core.model.audit.AuditAction
import com.samuelbaldasso.financeflow.core.model.audit.AuditEvent
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: UUID,
    val action: AuditAction,
    val actor: String,
    @ColumnInfo(name = "before_state") val beforeState: String?,
    @ColumnInfo(name = "after_state") val afterState: String?,
    val timestamp: Instant
) {
    fun toDomain(): AuditEvent = AuditEvent(
        id = id,
        entityType = entityType,
        entityId = entityId,
        action = action,
        actor = actor,
        beforeState = beforeState,
        afterState = afterState,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(event: AuditEvent): AuditLogEntity = AuditLogEntity(
            id = event.id,
            entityType = event.entityType,
            entityId = event.entityId,
            action = event.action,
            actor = event.actor,
            beforeState = event.beforeState,
            afterState = event.afterState,
            timestamp = event.timestamp
        )
    }
}
