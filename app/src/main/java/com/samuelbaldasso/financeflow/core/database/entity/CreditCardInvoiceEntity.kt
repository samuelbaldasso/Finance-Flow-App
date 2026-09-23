package com.samuelbaldasso.financeflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samuelbaldasso.financeflow.core.model.card.CreditCardInvoice
import com.samuelbaldasso.financeflow.core.model.card.InvoiceStatus
import com.samuelbaldasso.financeflow.core.model.money.Money
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "credit_card_invoices",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["card_account_id"]),
        Index(value = ["status"])
    ]
)
data class CreditCardInvoiceEntity(
    @PrimaryKey val id: UUID,
    @ColumnInfo(name = "card_account_id") val cardAccountId: UUID,
    @ColumnInfo(name = "closing_date") val closingDate: Instant,
    @ColumnInfo(name = "due_date") val dueDate: Instant,
    @ColumnInfo(name = "total_minor") val totalMinor: Long = 0L,
    @ColumnInfo(name = "paid_minor") val paidMinor: Long = 0L,
    val status: InvoiceStatus = InvoiceStatus.OPEN
) {
    fun toDomain(): CreditCardInvoice = CreditCardInvoice(
        id = id,
        cardAccountId = cardAccountId,
        closingDate = closingDate,
        dueDate = dueDate,
        totalAmount = Money(totalMinor),
        paidAmount = Money(paidMinor),
        status = status
    )

    companion object {
        fun fromDomain(invoice: CreditCardInvoice): CreditCardInvoiceEntity = CreditCardInvoiceEntity(
            id = invoice.id,
            cardAccountId = invoice.cardAccountId,
            closingDate = invoice.closingDate,
            dueDate = invoice.dueDate,
            totalMinor = invoice.totalAmount.amountMinor,
            paidMinor = invoice.paidAmount.amountMinor,
            status = invoice.status
        )
    }
}
