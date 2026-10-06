package com.iocl.knowledgehub.domain.model

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val documentId: String? = null,
    val documentType: DocumentType? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    NEW_MAGAZINE, NEW_NEWSPAPER, GENERAL, SYSTEM, UPDATE
}
