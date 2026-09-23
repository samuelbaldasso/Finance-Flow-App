package com.samuelbaldasso.financeflow.domain.usecase.category

import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import java.util.UUID

class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: UUID, reassignToCategoryId: UUID) {
        categoryRepository.deleteCategory(categoryId, reassignToCategoryId)
    }
}
