package com.iocl.knowledgehub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "magazines")
data class MagazineEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val coverImageUrl: String,
    val pdfUrl: String,
    val month: Int,
    val year: Int,
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
