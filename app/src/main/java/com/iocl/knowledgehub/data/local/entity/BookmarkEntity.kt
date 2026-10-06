package com.iocl.knowledgehub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val documentType: String,
    val title: String,
    val coverImageUrl: String,
    val addedAt: Long
)
