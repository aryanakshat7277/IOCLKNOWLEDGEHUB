package com.iocl.knowledgehub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String,
    val documentId: String?,
    val documentType: String?,
    val isRead: Boolean,
    val createdAt: Long
)
