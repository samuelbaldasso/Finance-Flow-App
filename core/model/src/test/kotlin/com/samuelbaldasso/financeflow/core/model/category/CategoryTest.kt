package com.samuelbaldasso.financeflow.core.model.category

import com.samuelbaldasso.financeflow.core.model.error.DomainException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CategoryTest {

    @Test
    fun `test subcategory detection via parent id`() {
        val parentId = UUID.randomUUID()
        val rootCat = Category(
            name = "Alimentação",
            type = CategoryType.EXPENSE,
            isSystem = true
        )
        assertFalse(rootCat.isSubcategory)

        val subCat = Category(
            name = "Restaurante",
            type = CategoryType.EXPENSE,
            parentCategoryId = parentId
        )
        assertTrue(subCat.isSubcategory)
        assertEquals(parentId, subCat.parentCategoryId)
    }

    @Test
    fun `test system category deletion throws SystemCategoryImmutableException`() {
        val systemCat = Category(
            name = "Salário",
            type = CategoryType.INCOME,
            isSystem = true
        )

        assertThrows(DomainException.SystemCategoryImmutableException::class.java) {
            systemCat.validateDeletionAllowed()
        }
    }
}
