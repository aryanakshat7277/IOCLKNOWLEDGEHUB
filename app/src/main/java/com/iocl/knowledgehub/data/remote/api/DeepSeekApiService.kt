package com.iocl.knowledgehub.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * DeepSeek API Service — OpenAI-compatible endpoint.
 * Used for fetching current IOCL news updates, magazine summaries,
 * and content intelligence.
 */
interface DeepSeekApiService {

    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: DeepSeekChatRequest
    ): Response<DeepSeekChatResponse>
}

data class DeepSeekChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2048,
    val stream: Boolean = false
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekChatResponse(
    val id: String?,
    val choices: List<DeepSeekChoice>?
)

data class DeepSeekChoice(
    val message: DeepSeekMessage?,
    val finish_reason: String?
)
