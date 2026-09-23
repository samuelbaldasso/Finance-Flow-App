package com.samuelbaldasso.financeflow.core.model.category

import com.samuelbaldasso.financeflow.core.model.error.DomainException
import java.util.UUID

enum class CategoryType {
    INCOME,
    EXPENSE
}

data class Category(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: CategoryType,
    val parentCategoryId: UUID? = null,
    val iconKey: String? = null,
    val colorHex: String? = null,
    val isSystem: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Category name cannot be blank" }
    }

    val isSubcategory: Boolean get() = parentCategoryId != null

    fun validateDeletionAllowed() {
        if (isSystem) {
            throw DomainException.SystemCategoryImmutableException("System default category '$name' cannot be deleted")
        }
    }
}
