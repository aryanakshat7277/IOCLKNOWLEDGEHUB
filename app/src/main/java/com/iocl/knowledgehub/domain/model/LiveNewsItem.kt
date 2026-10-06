package com.iocl.knowledgehub.domain.model

/**
 * Represents a live IOCL news article fetched via the existing AI model fallback chain.
 * Data source tracks which AI tier successfully responded.
 */
data class LiveNewsItem(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val imageUrl: String = "",
    val articleUrl: String = "",
    val sourceName: String = "",         // e.g. "Economic Times", "Livemint"
    val publishedAt: Long = System.currentTimeMillis(),
    val publishedAtRaw: String = "",     // ISO-8601 string as returned by AI
    val category: LiveNewsCategory = LiveNewsCategory.CORPORATE,
    val dataSource: LiveNewsSource = LiveNewsSource.GROQ
)

enum class LiveNewsCategory {
    CORPORATE, OPERATIONS, FINANCE, SUSTAINABILITY, SAFETY,
    HR, PROJECTS, CSR, TECHNOLOGY, EMERGENCY
}

/** Which AI tier produced this article */
enum class LiveNewsSource {
    GROQ,           // Tier 1 — Llama-3.3-70b (fastest)
    DEEPSEEK,       // Tier 2 — DeepSeek-chat
    OPENAI,         // Tier 3 — GPT-4o-mini
    OPENROUTER,     // Tier 4 — Llama-3.1-8b-instruct:free
    AI_GENERATED,   // Generic AI (used for hardcoded fallback display)
}
