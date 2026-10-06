package com.iocl.knowledgehub.domain.model

data class Broadcast(
    val id: String = "",
    val senderId: String = "",
    val title: String = "",
    val content: String = "",
    val type: BroadcastType = BroadcastType.COMPANY_WIDE,
    val targetDepartment: String = "",
    val targetUserId: String = "",
    val attachments: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isAcknowledged: Boolean = false
)

enum class BroadcastType {
    COMPANY_WIDE, DEPARTMENT_WISE, INDIVIDUAL, EMERGENCY
}
