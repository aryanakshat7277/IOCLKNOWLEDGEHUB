package com.iocl.knowledgehub.data.mapper

import com.iocl.knowledgehub.data.local.entity.MagazineEntity
import com.iocl.knowledgehub.domain.model.Magazine
import com.iocl.knowledgehub.domain.model.MagazineCategory
import javax.inject.Inject

class MagazineMapper @Inject constructor() {
    fun entityToDomain(entity: MagazineEntity): Magazine = Magazine(
        id = entity.id,
        title = entity.title,
        description = entity.description,
        coverImageUrl = entity.coverImageUrl,
        pdfUrl = entity.pdfUrl,
        month = entity.month,
        year = entity.year,
        category = try {
            MagazineCategory.valueOf(entity.category)
        } catch (e: Exception) {
            MagazineCategory.CORPORATE
        },
        tags = entity.tags.split(",").filter { it.isNotBlank() },
        pageCount = entity.pageCount,
        fileSize = entity.fileSize,
        viewCount = entity.viewCount,
        downloadCount = entity.downloadCount,
        uploadedBy = entity.uploadedBy,
        uploadedAt = entity.uploadedAt,
        isBookmarked = entity.isBookmarked
    )

    fun domainToEntity(magazine: Magazine): MagazineEntity = MagazineEntity(
        id = magazine.id,
        title = magazine.title,
        description = magazine.description,
        coverImageUrl = magazine.coverImageUrl,
        pdfUrl = magazine.pdfUrl,
        month = magazine.month,
        year = magazine.year,
        category = magazine.category.name,
        tags = magazine.tags.joinToString(","),
        pageCount = magazine.pageCount,
        fileSize = magazine.fileSize,
        viewCount = magazine.viewCount,
        downloadCount = magazine.downloadCount,
        uploadedBy = magazine.uploadedBy,
        uploadedAt = magazine.uploadedAt,
        isBookmarked = magazine.isBookmarked
    )
}
