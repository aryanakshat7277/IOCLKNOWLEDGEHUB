package com.iocl.knowledgehub.domain.repository

import com.iocl.knowledgehub.domain.model.ChatMessage
import com.iocl.knowledgehub.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatHistory(): Flow<List<ChatMessage>>
    suspend fun sendMessage(message: String): Resource<ChatMessage>
    suspend fun clearHistory(): Resource<Unit>
}
