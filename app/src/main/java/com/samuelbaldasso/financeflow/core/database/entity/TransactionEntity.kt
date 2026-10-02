package com.samuelbaldasso.financeflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["category_id"]),
        Index(value = ["transfer_id"]),
        Index(value = ["installment_group_id"]),
        Index(value = ["effective_date"]),
        Index(value = ["status"]),
        Index(value = ["account_id", "status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "account_id") val accountId: UUID,
    val type: TransactionType,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "competence_date") val competenceDate: Instant,
    @ColumnInfo(name = "effective_date") val effectiveDate: Instant,
    @ColumnInfo(name = "category_id") val categoryId: UUID? = null,
    val description: String,
    val tags: List<String> = emptyList(),
    @ColumnInfo(name = "attachment_path") val attachmentPath: String? = null,
    val status: TransactionStatus,
    @ColumnInfo(name = "transfer_id") val transferId: UUID? = null,
    @ColumnInfo(name = "destination_account_id") val destinationAccountId: UUID? = null,
    @ColumnInfo(name = "installment_group_id") val installmentGroupId: UUID? = null,
    @ColumnInfo(name = "installment_number") val installmentNumber: Int? = null,
    @ColumnInfo(name = "total_installments") val totalInstallments: Int? = null,
    @ColumnInfo(name = "goal_id") val goalId: UUID? = null,
    @ColumnInfo(name = "recurrence_rule_id") val recurrenceRuleId: UUID? = null,
    @ColumnInfo(name = "invoice_id") val invoiceId: UUID? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        accountId = accountId,
        type = type,
        amount = Money(amountMinor),
        competenceDate = competenceDate,
        effectiveDate = effectiveDate,
        categoryId = categoryId,
        description = description,
        tags = tags,
        attachmentPath = attachmentPath,
        status = status,
        transferId = transferId,
        destinationAccountId = destinationAccountId,
        installmentGroupId = installmentGroupId,
        installmentNumber = installmentNumber,
        totalInstallments = totalInstallments,
        goalId = goalId,
        recurrenceRuleId = recurrenceRuleId,
        invoiceId = invoiceId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(tx: Transaction): TransactionEntity = TransactionEntity(
            id = tx.id,
            accountId = tx.accountId,
            type = tx.type,
            amountMinor = tx.amount.amountMinor,
            competenceDate = tx.competenceDate,
            effectiveDate = tx.effectiveDate,
            categoryId = tx.categoryId,
            description = tx.description,
            tags = tx.tags,
            attachmentPath = tx.attachmentPath,
            status = tx.status,
            transferId = tx.transferId,
            destinationAccountId = tx.destinationAccountId,
            installmentGroupId = tx.installmentGroupId,
            installmentNumber = tx.installmentNumber,
            totalInstallments = tx.totalInstallments,
            goalId = tx.goalId,
            recurrenceRuleId = tx.recurrenceRuleId,
            invoiceId = tx.invoiceId,
            createdAt = tx.createdAt,
            updatedAt = tx.updatedAt
        )
    }
}
