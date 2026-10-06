package com.iocl.knowledgehub.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI ChatGPT API Service — 5th tier fallback.
 * Uses GPT-4o-mini for cost-effective, high-quality responses.
 */
interface OpenAIApiService {

    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: OpenAIChatRequest
    ): Response<OpenAIChatResponse>
}

data class OpenAIChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024,
    val stream: Boolean = false
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIChatResponse(
    val id: String?,
    val choices: List<OpenAIChoice>?
)

data class OpenAIChoice(
    val message: OpenAIMessage?,
    val finish_reason: String?
)
