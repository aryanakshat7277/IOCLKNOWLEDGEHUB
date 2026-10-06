package com.iocl.knowledgehub.data.mapper

import com.iocl.knowledgehub.data.local.entity.NewspaperEntity
import com.iocl.knowledgehub.domain.model.Newspaper
import com.iocl.knowledgehub.domain.model.NewspaperCategory
import javax.inject.Inject

class NewspaperMapper @Inject constructor() {
    fun entityToDomain(entity: NewspaperEntity): Newspaper = Newspaper(
        id = entity.id,
        title = entity.title,
        headline = entity.headline,
        summary = entity.summary,
        coverImageUrl = entity.coverImageUrl,
        pdfUrl = entity.pdfUrl,
        publishDate = entity.publishDate,
        edition = entity.edition,
        category = try {
            NewspaperCategory.valueOf(entity.category)
        } catch (e: Exception) {
            NewspaperCategory.DAILY
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

    fun domainToEntity(newspaper: Newspaper): NewspaperEntity = NewspaperEntity(
        id = newspaper.id,
        title = newspaper.title,
        headline = newspaper.headline,
        summary = newspaper.summary,
        coverImageUrl = newspaper.coverImageUrl,
        pdfUrl = newspaper.pdfUrl,
        publishDate = newspaper.publishDate,
        edition = newspaper.edition,
        category = newspaper.category.name,
        tags = newspaper.tags.joinToString(","),
        pageCount = newspaper.pageCount,
        fileSize = newspaper.fileSize,
        viewCount = newspaper.viewCount,
        downloadCount = newspaper.downloadCount,
        uploadedBy = newspaper.uploadedBy,
        uploadedAt = newspaper.uploadedAt,
        isBookmarked = newspaper.isBookmarked
    )
}
