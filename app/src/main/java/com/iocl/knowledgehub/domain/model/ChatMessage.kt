package com.iocl.knowledgehub.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val isFromUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val suggestions: List<String> = emptyList()
)
