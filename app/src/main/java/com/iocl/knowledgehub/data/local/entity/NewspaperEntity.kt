package com.iocl.knowledgehub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "newspapers")
data class NewspaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val headline: String,
    val summary: String,
    val coverImageUrl: String,
    val pdfUrl: String,
    val publishDate: Long,
    val edition: String,
    val category: String,
    val tags: String,
    val pageCount: Int,
    val fileSize: Long,
    val viewCount: Int,
    val downloadCount: Int,
    val uploadedBy: String,
    val uploadedAt: Long,
    val isBookmarked: Boolean = false
)
