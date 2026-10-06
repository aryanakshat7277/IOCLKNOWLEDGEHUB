package com.iocl.knowledgehub.domain.usecase.chat

import com.iocl.knowledgehub.domain.model.ChatMessage
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(message: String): Resource<ChatMessage> {
        if (message.isBlank()) return Resource.Error("Message cannot be empty")
        return chatRepository.sendMessage(message.trim())
    }
}
