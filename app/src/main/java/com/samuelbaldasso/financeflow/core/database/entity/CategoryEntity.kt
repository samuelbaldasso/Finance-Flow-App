package com.samuelbaldasso.financeflow.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samuelbaldasso.financeflow.core.model.category.Category
import com.samuelbaldasso.financeflow.core.model.category.CategoryType
import java.util.UUID

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["parent_category_id"])]
)
data class CategoryEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val type: CategoryType,
    @ColumnInfo(name = "parent_category_id") val parentCategoryId: UUID? = null,
    @ColumnInfo(name = "icon_key") val iconKey: String? = null,
    @ColumnInfo(name = "color_hex") val colorHex: String? = null,
    @ColumnInfo(name = "is_system") val isSystem: Boolean = false
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        type = type,
        parentCategoryId = parentCategoryId,
        iconKey = iconKey,
        colorHex = colorHex,
        isSystem = isSystem
    )

    companion object {
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            name = category.name,
            type = category.type,
            parentCategoryId = category.parentCategoryId,
            iconKey = category.iconKey,
            colorHex = category.colorHex,
            isSystem = category.isSystem
        )
    }
}
