package com.vboard.aac.data.mapper

import com.vboard.aac.data.local.db.entity.CategoryEntity
import com.vboard.aac.data.local.db.entity.VocabCardEntity
import com.vboard.aac.data.local.db.entity.WordUsageEntity
import com.vboard.aac.domain.model.Category
import com.vboard.aac.domain.model.VocabCard
import com.vboard.aac.domain.model.WordUsage

object VocabCardMapper {

    fun VocabCardEntity.toDomain(): VocabCard = VocabCard(
        id = id,
        word = word,
        categoryId = categoryId,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        isCustom = isCustom,
        displayOrder = displayOrder
    )

    fun VocabCard.toEntity(): VocabCardEntity = VocabCardEntity(
        id = id,
        word = word,
        categoryId = categoryId,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        isCustom = isCustom,
        displayOrder = displayOrder,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        name = name,
        icon = icon,
        color = color,
        displayOrder = displayOrder
    )

    fun Category.toEntity(): CategoryEntity = CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color,
        displayOrder = displayOrder
    )

    fun WordUsageEntity.toDomain(): WordUsage = WordUsage(
        word = word,
        count = count,
        usageDate = usageDate
    )
}
