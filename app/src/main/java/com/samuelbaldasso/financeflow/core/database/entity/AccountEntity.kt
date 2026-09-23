package com.samuelbaldasso.financeflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import java.time.Instant
import java.util.UUID

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val type: AccountType,
    val currency: CurrencyCode,
    @ColumnInfo(name = "initial_balance_minor") val initialBalanceMinor: Long,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "color_hex") val colorHex: String? = null,
    @ColumnInfo(name = "icon_key") val iconKey: String? = null,
    @ColumnInfo(name = "credit_limit_minor") val creditLimitMinor: Long? = null,
    @ColumnInfo(name = "closing_day") val closingDay: Int? = null,
    @ColumnInfo(name = "due_day") val dueDay: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
) {
    fun toDomain(): Account = Account(
        id = id,
        name = name,
        type = type,
        currency = currency,
        initialBalance = Money(initialBalanceMinor),
        isArchived = isArchived,
        colorHex = colorHex,
        iconKey = iconKey,
        creditLimit = creditLimitMinor?.let { Money(it) },
        closingDay = closingDay,
        dueDay = dueDay,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            name = account.name,
            type = account.type,
            currency = account.currency,
            initialBalanceMinor = account.initialBalance.amountMinor,
            isArchived = account.isArchived,
            colorHex = account.colorHex,
            iconKey = account.iconKey,
            creditLimitMinor = account.creditLimit?.amountMinor,
            closingDay = account.closingDay,
            dueDay = account.dueDay,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }
}
