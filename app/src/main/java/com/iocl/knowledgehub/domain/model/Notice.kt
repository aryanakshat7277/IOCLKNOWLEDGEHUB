package com.iocl.knowledgehub.domain.model

data class Notice(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val department: String = "",
    val date: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000, // Default 7 days
    val priority: NoticePriority = NoticePriority.NORMAL,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

enum class NoticePriority {
    NORMAL, IMPORTANT, URGENT
}
