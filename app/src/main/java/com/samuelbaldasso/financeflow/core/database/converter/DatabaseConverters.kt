package com.samuelbaldasso.financeflow.core.database.converter

import androidx.room.TypeConverter
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.audit.AuditAction
import com.samuelbaldasso.financeflow.core.model.card.InvoiceStatus
import com.samuelbaldasso.financeflow.core.model.category.CategoryType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import java.time.Instant
import java.util.UUID

class DatabaseConverters {

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(uuidStr: String?): UUID? = uuidStr?.let { UUID.fromString(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(millis: Long?): Instant? = millis?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromCurrencyCode(currency: CurrencyCode?): String? = currency?.code

    @TypeConverter
    fun toCurrencyCode(code: String?): CurrencyCode? = code?.let { CurrencyCode.fromCode(it) }

    @TypeConverter
    fun fromAccountType(type: AccountType?): String? = type?.name

    @TypeConverter
    fun toAccountType(name: String?): AccountType? = name?.let { AccountType.valueOf(it) }

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun toTransactionType(name: String?): TransactionType? = name?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus?): String? = status?.name

    @TypeConverter
    fun toTransactionStatus(name: String?): TransactionStatus? = name?.let { TransactionStatus.valueOf(it) }

    @TypeConverter
    fun fromCategoryType(type: CategoryType?): String? = type?.name

    @TypeConverter
    fun toCategoryType(name: String?): CategoryType? = name?.let { CategoryType.valueOf(it) }

    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus?): String? = status?.name

    @TypeConverter
    fun toInvoiceStatus(name: String?): InvoiceStatus? = name?.let { InvoiceStatus.valueOf(it) }

    @TypeConverter
    fun fromAuditAction(action: AuditAction?): String? = action?.name

    @TypeConverter
    fun toAuditAction(name: String?): AuditAction? = name?.let { AuditAction.valueOf(it) }

    @TypeConverter
    fun fromTagsList(tags: List<String>?): String? = tags?.joinToString(",")

    @TypeConverter
    fun toTagsList(data: String?): List<String> =
        if (data.isNullOrBlank()) emptyList() else data.split(",").map { it.trim() }
}
