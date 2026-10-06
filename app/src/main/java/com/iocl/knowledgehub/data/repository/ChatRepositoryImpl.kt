package com.iocl.knowledgehub.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.iocl.knowledgehub.data.remote.api.*
import com.iocl.knowledgehub.domain.model.ChatMessage
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Chat repository with 6-tier AI fallback — data fetching NEVER fails.
 * On ANY negative response, instantly switches to the next provider.
 * 
 * 1. Custom Backend API (context-aware, knowledge-base)
 * 2. Google Gemini Pro
 * 3. Groq (Llama 3.3 70B) — ultra-fast
 * 4. DeepSeek (deepseek-chat) — deep reasoning
 * 5. OpenAI ChatGPT (gpt-4o-mini) — reliable
 * 6. OpenRouter (meta-llama) — ultimate safety net
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: IOCLApiService,
    private val generativeModel: GenerativeModel,
    private val groqApiService: GroqApiService,
    @Named("groq_api_key") private val groqApiKey: String,
    private val deepSeekApiService: DeepSeekApiService,
    @Named("deepseek_api_key") private val deepSeekApiKey: String,
    private val openAIApiService: OpenAIApiService,
    @Named("openai_api_key") private val openAIApiKey: String,
    private val openRouterApiService: OpenRouterApiService,
    @Named("openrouter_api_key") private val openRouterApiKey: String
) : ChatRepository {

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                content = "Hello! I'm your IOCL Knowledge Hub assistant powered by multi-AI. " +
                        "I can help you find magazines, newspapers, and answer questions " +
                        "about Indian Oil Corporation's publications. How can I help you today?",
                isFromUser = false,
                suggestions = listOf(
                    "Show latest magazines",
                    "Find safety guidelines",
                    "Search quarterly reports",
                    "What's new this month?"
                )
            )
        )
    )

    private var conversationId: String? = null

    companion object {
        private const val SYSTEM_PROMPT = """
            You are a helpful and intelligent AI Assistant. You must answer ALL user questions on any topic like a general AI assistant.
            However, when any question is asked regarding IOCL (Indian Oil Corporation Limited), you MUST act as the ultimate IOCL expert. For IOCL topics, you must provide highly detailed, accurate, and comprehensive answers using all available IOCL data and knowledge on the entire internet.
            
            Guidelines:
            - Answer general questions on any topic helpfully and professionally.
            - For IOCL questions: deeply analyze the request and provide the most accurate corporate information, policies, guidelines, or historical data available on the internet.
            - Always be professional, concise, and maintain corporate decorum.
        """
    }

    override fun getChatHistory(): Flow<List<ChatMessage>> = _chatHistory.asStateFlow()

    override suspend fun sendMessage(message: String): Resource<ChatMessage> {
        val userMessage = ChatMessage(content = message, isFromUser = true)
        _chatHistory.value = _chatHistory.value + userMessage

        // 6-tier cascade — try each, instantly switch on failure
        val providers: List<suspend () -> Resource<ChatMessage>> = listOf(
            { sendViaBackend(message) },
            { sendViaGemini(message) },
            { sendViaGroq(message) },
            { sendViaDeepSeek(message) },
            { sendViaOpenAI(message) },
            { sendViaOpenRouter(message) }
        )

        for ((index, provider) in providers.withIndex()) {
            try {
                val result = provider()
                if (result is Resource.Success) return result
                // Negative response → instantly try next
            } catch (_: Exception) {
                // Exception → instantly try next
            }
        }

        // All 6 tiers exhausted
        val errorMessage = ChatMessage(
            content = "Sorry, I'm having trouble connecting right now. " +
                    "Please check your internet connection and try again.",
            isFromUser = false,
            isError = true
        )
        _chatHistory.value = _chatHistory.value + errorMessage
        return Resource.Error("All 6 AI services unavailable")
    }

    // ═══════════════════════════════════════════════════════════
    // TIER 1: Custom Backend
    // ═══════════════════════════════════════════════════════════
    private suspend fun sendViaBackend(message: String): Resource<ChatMessage> {
        val response = apiService.sendChatMessage(
            request = ChatRequest(
                message = message,
                conversationId = conversationId,
                context = "IOCL Knowledge Hub - Indian Oil Corporation publications assistant"
            ),
            token = "Bearer "
        )
        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            conversationId = body.conversationId
            val botMessage = ChatMessage(
                id = body.id,
                content = body.response,
                isFromUser = false,
                timestamp = body.timestamp,
                suggestions = body.suggestions
            )
            _chatHistory.value = _chatHistory.value + botMessage
            return Resource.Success(botMessage)
        }
        throw Exception("Backend: ${response.code()}")
    }

    // ═══════════════════════════════════════════════════════════
    // TIER 2: Google Gemini
    // ═══════════════════════════════════════════════════════════
    private suspend fun sendViaGemini(message: String): Resource<ChatMessage> {
        val fullPrompt = "${SYSTEM_PROMPT.trimIndent()}\n\nUser: $message"
        val response = generativeModel.generateContent(fullPrompt)
        val text = response.text
            ?: throw Exception("Gemini: empty response")
        return appendBotMessage(text, message)
    }

    // ═══════════════════════════════════════════════════════════
    // TIER 3: Groq (Llama 3.3 70B)
    // ═══════════════════════════════════════════════════════════
    private suspend fun sendViaGroq(message: String): Resource<ChatMessage> {
        val response = groqApiService.chatCompletion(
            apiKey = "Bearer $groqApiKey",
            request = GroqChatRequest(
                messages = listOf(
                    GroqMessage("system", SYSTEM_PROMPT.trimIndent()),
                    GroqMessage("user", message)
                )
            )
        )
        return extractOpenAIStyle(response, "Groq", message)
    }

    // ═══════════════════════════════════════════════════════════
    // TIER 4: DeepSeek
    // ═══════════════════════════════════════════════════════════
    private suspend fun sendViaDeepSeek(message: String): Resource<ChatMessage> {
        val response = deepSeekApiService.chatCompletion(
            apiKey = "Bearer $deepSeekApiKey",
            request = DeepSeekChatRequest(
                messages = listOf(
                    DeepSeekMessage("system", SYSTEM_PROMPT.trimIndent()),
                    DeepSeekMessage("user", message)
                )
            )
        )
        return extractOpenAIStyle(response, "DeepSeek", message)
    }

    // ═══════════════════════════════════════════════════════════
    // TIER 5: OpenAI ChatGPT
    // ═══════════════════════════════════════════════════════════
    private suspend fun sendViaOpenAI(message: String): Resource<ChatMessage> {
        val response = openAIApiService.chatCompletion(
            apiKey = "Bearer $openAIApiKey",
            request = OpenAIChatRequest(
                messages = listOf(
                    OpenAIMessage("system", SYSTEM_PROMPT.trimIndent()),
                    OpenAIMessage("user", message)
                )
            )
        )
        return extractOpenAIStyle(response, "OpenAI", message)
    }

    // ═══════════════════════════════════════════════════════════
    // TIER 6: OpenRouter
    // ═══════════════════════════════════════════════════════════
    private suspend fun sendViaOpenRouter(message: String): Resource<ChatMessage> {
        val response = openRouterApiService.chatCompletion(
            apiKey = "Bearer $openRouterApiKey",
            request = OpenRouterChatRequest(
                messages = listOf(
                    OpenRouterMessage("system", SYSTEM_PROMPT.trimIndent()),
                    OpenRouterMessage("user", message)
                )
            )
        )
        return extractOpenAIStyle(response, "OpenRouter", message)
    }

    // ═══════════════════════════════════════════════════════════
    // SHARED HELPERS
    // ═══════════════════════════════════════════════════════════

    /** Generic extractor for OpenAI-compatible response formats */
    private fun <T> extractOpenAIStyle(
        response: retrofit2.Response<T>,
        name: String,
        message: String
    ): Resource<ChatMessage> {
        if (!response.isSuccessful) throw Exception("$name: ${response.code()}")
        val body = response.body() ?: throw Exception("$name: null body")

        // Use reflection-free approach — all our response types have .choices
        val content = when (body) {
            is GroqChatResponse -> body.choices?.firstOrNull()?.message?.content
            is DeepSeekChatResponse -> body.choices?.firstOrNull()?.message?.content
            is OpenAIChatResponse -> body.choices?.firstOrNull()?.message?.content
            is OpenRouterChatResponse -> body.choices?.firstOrNull()?.message?.content
            else -> null
        } ?: throw Exception("$name: empty choices")

        return appendBotMessage(content, message)
    }

    private fun appendBotMessage(content: String, userMessage: String): Resource<ChatMessage> {
        val botMessage = ChatMessage(
            content = content,
            isFromUser = false,
            suggestions = generateSuggestions(userMessage)
        )
        _chatHistory.value = _chatHistory.value + botMessage
        return Resource.Success(botMessage)
    }

    private fun generateSuggestions(lastMessage: String): List<String> {
        return when {
            lastMessage.contains("magazine", ignoreCase = true) -> listOf(
                "Show recent magazines", "Find by category", "Download latest issue"
            )
            lastMessage.contains("newspaper", ignoreCase = true) -> listOf(
                "Show today's paper", "Browse editions", "Search archives"
            )
            lastMessage.contains("safety", ignoreCase = true) -> listOf(
                "Safety guidelines", "Recent safety updates", "Safety training materials"
            )
            lastMessage.contains("report", ignoreCase = true) -> listOf(
                "Quarterly reports", "Annual report", "Sustainability report"
            )
            else -> listOf("Browse magazines", "Latest newspapers", "Help & FAQ")
        }
    }

    override suspend fun clearHistory(): Resource<Unit> {
        try { apiService.clearChatHistory("Bearer ") } catch (_: Exception) {}
        _chatHistory.value = listOf(
            ChatMessage(
                content = "Chat cleared. How can I help you today?",
                isFromUser = false,
                suggestions = listOf(
                    "Show latest magazines",
                    "Find safety guidelines",
                    "Search quarterly reports"
                )
            )
        )
        conversationId = null
        return Resource.Success(Unit)
    }
}
