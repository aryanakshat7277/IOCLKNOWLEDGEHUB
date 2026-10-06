package com.iocl.knowledgehub.domain.repository

import com.iocl.knowledgehub.domain.model.Resource

/**
 * Repository for AI-powered content intelligence.
 * Uses DeepSeek to fetch current IOCL news updates, 
 * magazine summaries, and content recommendations.
 */
interface ContentIntelligenceRepository {
    
    /** Fetch latest IOCL news updates and industry highlights */
    suspend fun fetchLatestNews(): Resource<List<NewsUpdate>>
    
    /** Generate intelligent magazine summary/recommendations */
    suspend fun getMagazineInsights(category: String? = null): Resource<List<ContentInsight>>
    
    /** Get trending topics and content recommendations */
    suspend fun getTrendingTopics(): Resource<List<String>>
}

data class NewsUpdate(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val source: String = "DeepSeek AI",
    val timestamp: Long = System.currentTimeMillis(),
    val isBreaking: Boolean = false
)

data class ContentInsight(
    val title: String,
    val description: String,
    val relevanceScore: Float = 0.85f,
    val category: String,
    val actionLabel: String = "Read More"
)
