package com.iocl.knowledgehub.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenRouter API Service — 6th tier fallback.
 * Routes to the best available model across multiple providers.
 */
interface OpenRouterApiService {

    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") apiKey: String,
        @Header("HTTP-Referer") referer: String = "https://iocl-knowledgehub.com",
        @Header("X-Title") title: String = "IOCL Knowledge Hub",
        @Body request: OpenRouterChatRequest
    ): Response<OpenRouterChatResponse>
}

data class OpenRouterChatRequest(
    val model: String = "meta-llama/llama-3.1-8b-instruct",
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024
)

data class OpenRouterMessage(
    val role: String,
    val content: String
)

data class OpenRouterChatResponse(
    val id: String?,
    val choices: List<OpenRouterChoice>?
)

data class OpenRouterChoice(
    val message: OpenRouterMessage?,
    val finish_reason: String?
)
