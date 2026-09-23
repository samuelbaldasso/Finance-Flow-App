package com.samuelbaldasso.financeflow.core.model.error

sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class CurrencyMismatchException(message: String) : DomainException(message)
    class ZeroAmountException(message: String = "Transaction amount cannot be zero") : DomainException(message)
    class InvalidFutureDateException(message: String) : DomainException(message)
    class AccountArchivedException(message: String = "Archived account cannot receive mutations") : DomainException(message)
    class ReconciledTransactionLockedException(message: String = "Reconciled transactions cannot be modified directly") : DomainException(message)
    class CategoryInUseException(message: String = "Category is in use and requires reassignment before deletion") : DomainException(message)
    class SystemCategoryImmutableException(message: String = "System default categories cannot be deleted or renamed") : DomainException(message)
    class NegativeAvailableLimitException(message: String) : DomainException(message)
}
