package com.iocl.knowledgehub.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Custom backend API for IOCL Knowledge Hub
 * This API wraps Gemini AI and provides additional context-aware responses
 */
interface IOCLApiService {

    @POST("api/v1/chat/message")
    suspend fun sendChatMessage(
        @Body request: ChatRequest,
        @Header("Authorization") token: String
    ): Response<ChatResponse>

    @GET("api/v1/chat/history")
    suspend fun getChatHistory(
        @Header("Authorization") token: String
    ): Response<List<ChatMessageDto>>

    @POST("api/v1/chat/clear")
    suspend fun clearChatHistory(
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("api/v1/analytics/dashboard")
    suspend fun getDashboardAnalytics(
        @Header("Authorization") token: String
    ): Response<AnalyticsDto>
}

data class ChatRequest(
    val message: String,
    val conversationId: String? = null,
    val context: String? = null
)

data class ChatResponse(
    val id: String,
    val response: String,
    val suggestions: List<String> = emptyList(),
    val conversationId: String,
    val timestamp: Long
)

data class ChatMessageDto(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val suggestions: List<String> = emptyList()
)

data class AnalyticsDto(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalMagazines: Int = 0,
    val totalNewspapers: Int = 0,
    val totalDownloads: Int = 0,
    val totalViews: Int = 0,
    val monthlyActiveUsers: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap()
)
