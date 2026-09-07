package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.entity.CategoryEntity
import sayan.apps.rupeeflow.domain.model.Category

fun CategoryEntity.toDomainModel(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        type = type,
        budget = budget,
        parentCategoryId = parentCategoryId,
        isDeleted = isDeleted
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        type = type,
        budget = budget,
        parentCategoryId = parentCategoryId,
        isDeleted = isDeleted
    )
}
