package com.iocl.knowledgehub.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Groq API Service — OpenAI-compatible endpoint for fast LLM inference.
 * Used as fallback when both custom backend and Gemini fail.
 */
interface GroqApiService {

    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>
}

data class GroqChatRequest(
    val model: String = "llama-3.3-70b-versatile",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024,
    val stream: Boolean = false
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqChatResponse(
    val id: String?,
    val choices: List<GroqChoice>?
)

data class GroqChoice(
    val message: GroqMessage?,
    val finish_reason: String?
)
