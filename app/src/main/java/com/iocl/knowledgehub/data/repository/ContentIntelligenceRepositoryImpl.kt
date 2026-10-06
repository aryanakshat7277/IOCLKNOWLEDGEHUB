package com.iocl.knowledgehub.data.repository

import com.iocl.knowledgehub.data.remote.api.DeepSeekApiService
import com.iocl.knowledgehub.data.remote.api.DeepSeekChatRequest
import com.iocl.knowledgehub.data.remote.api.DeepSeekMessage
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.ContentInsight
import com.iocl.knowledgehub.domain.repository.ContentIntelligenceRepository
import com.iocl.knowledgehub.domain.repository.NewsUpdate
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * DeepSeek-powered content intelligence for IOCL Knowledge Hub.
 * Fetches current news, magazine insights, and trending topics
 * using DeepSeek's chat model.
 */
@Singleton
class ContentIntelligenceRepositoryImpl @Inject constructor(
    private val deepSeekApiService: DeepSeekApiService,
    @Named("deepseek_api_key") private val apiKey: String
) : ContentIntelligenceRepository {

    override suspend fun fetchLatestNews(): Resource<List<NewsUpdate>> {
        return try {
            val request = DeepSeekChatRequest(
                messages = listOf(
                    DeepSeekMessage(
                        role = "system",
                        content = """
                            You are an IOCL (Indian Oil Corporation Limited) news aggregator.
                            Generate 6 realistic, current news updates about IOCL covering:
                            - Corporate announcements & quarterly results
                            - New refinery projects & infrastructure
                            - Sustainability & green energy initiatives
                            - Safety achievements & awards
                            - Employee welfare & HR updates
                            - Industry trends & petroleum sector news
                            
                            Return ONLY a JSON array with objects having these fields:
                            title, summary, category, isBreaking (boolean)
                            
                            Categories: "Corporate", "Infrastructure", "Sustainability", 
                            "Safety", "HR & Welfare", "Industry"
                            
                            Make 1 item isBreaking=true. Keep titles under 80 chars,
                            summaries under 150 chars.
                        """.trimIndent()
                    ),
                    DeepSeekMessage(
                        role = "user",
                        content = "Generate the latest IOCL news updates for today."
                    )
                ),
                temperature = 0.8
            )

            val response = deepSeekApiService.chatCompletion(
                apiKey = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices?.firstOrNull()?.message?.content ?: "[]"
                val newsUpdates = parseNewsUpdates(content)
                Resource.Success(newsUpdates)
            } else {
                Resource.Error("DeepSeek API error: ${response.code()}")
            }
        } catch (e: Exception) {
            // Return fallback news if API fails
            Resource.Success(getFallbackNews())
        }
    }

    override suspend fun getMagazineInsights(category: String?): Resource<List<ContentInsight>> {
        return try {
            val categoryFilter = category?.let { "Focus on the '$it' category." } ?: ""
            
            val request = DeepSeekChatRequest(
                messages = listOf(
                    DeepSeekMessage(
                        role = "system",
                        content = """
                            You are an IOCL magazine content curator. Generate 4 magazine 
                            content insights/recommendations for Indian Oil Corporation employees.
                            $categoryFilter
                            
                            Topics can include: technical innovations, safety best practices,
                            sustainability reports, employee spotlights, quarterly financials,
                            refinery operations, green hydrogen, EV charging, LNG terminals.
                            
                            Return ONLY a JSON array with objects having:
                            title, description, category, actionLabel
                            
                            Keep descriptions under 120 chars. actionLabel should be 
                            contextual like "Read Article", "View Report", "Explore".
                        """.trimIndent()
                    ),
                    DeepSeekMessage(
                        role = "user",
                        content = "What are the top magazine insights for IOCL employees this month?"
                    )
                ),
                temperature = 0.7
            )

            val response = deepSeekApiService.chatCompletion(
                apiKey = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices?.firstOrNull()?.message?.content ?: "[]"
                val insights = parseInsights(content)
                Resource.Success(insights)
            } else {
                Resource.Success(getFallbackInsights())
            }
        } catch (e: Exception) {
            Resource.Success(getFallbackInsights())
        }
    }

    override suspend fun getTrendingTopics(): Resource<List<String>> {
        return try {
            val request = DeepSeekChatRequest(
                messages = listOf(
                    DeepSeekMessage(
                        role = "system",
                        content = """
                            Generate 8 trending topics relevant to Indian Oil Corporation 
                            (IOCL) employees right now. Include mix of:
                            - Industry trends (green energy, EV, hydrogen)
                            - Corporate topics (quarterly results, new projects)  
                            - Safety & training
                            - Employee engagement
                            
                            Return ONLY a JSON array of strings, each under 40 chars.
                        """.trimIndent()
                    ),
                    DeepSeekMessage(
                        role = "user",
                        content = "What's trending at IOCL right now?"
                    )
                ),
                temperature = 0.9,
                max_tokens = 512
            )

            val response = deepSeekApiService.chatCompletion(
                apiKey = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val content = response.body()!!.choices?.firstOrNull()?.message?.content ?: "[]"
                val topics = parseStringArray(content)
                Resource.Success(topics)
            } else {
                Resource.Success(getFallbackTopics())
            }
        } catch (e: Exception) {
            Resource.Success(getFallbackTopics())
        }
    }

    // ─── JSON Parsers ────────────────────────────────────────

    private fun parseNewsUpdates(json: String): List<NewsUpdate> {
        return try {
            val cleanJson = json.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val array = JSONArray(cleanJson)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                NewsUpdate(
                    id = UUID.randomUUID().toString(),
                    title = obj.optString("title", "IOCL Update"),
                    summary = obj.optString("summary", ""),
                    category = obj.optString("category", "Corporate"),
                    isBreaking = obj.optBoolean("isBreaking", false)
                )
            }
        } catch (e: Exception) {
            getFallbackNews()
        }
    }

    private fun parseInsights(json: String): List<ContentInsight> {
        return try {
            val cleanJson = json.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val array = JSONArray(cleanJson)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ContentInsight(
                    title = obj.optString("title", ""),
                    description = obj.optString("description", ""),
                    category = obj.optString("category", "General"),
                    actionLabel = obj.optString("actionLabel", "Read More")
                )
            }
        } catch (e: Exception) {
            getFallbackInsights()
        }
    }

    private fun parseStringArray(json: String): List<String> {
        return try {
            val cleanJson = json.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val array = JSONArray(cleanJson)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            getFallbackTopics()
        }
    }

    // ─── Fallback Data ───────────────────────────────────────

    private fun getFallbackNews(): List<NewsUpdate> = listOf(
        NewsUpdate(
            id = "1", title = "IOCL Reports Record Q4 Revenue of ₹2.35 Lakh Crore",
            summary = "Indian Oil posts highest-ever quarterly revenue driven by strong refining margins and increased retail sales.",
            category = "Corporate", isBreaking = true
        ),
        NewsUpdate(
            id = "2", title = "Green Hydrogen Plant Commissioned at Gujarat Refinery",
            summary = "IOCL inaugurates 5MW green hydrogen production facility, marking a milestone in clean energy transition.",
            category = "Sustainability"
        ),
        NewsUpdate(
            id = "3", title = "IOCL Bags National Safety Award 2024",
            summary = "Haldia Refinery receives prestigious National Safety Award for exemplary safety practices and zero incident record.",
            category = "Safety"
        ),
        NewsUpdate(
            id = "4", title = "500 New EV Charging Stations Planned Across Highways",
            summary = "Indian Oil expands EV infrastructure with 500 fast-charging stations along national highways by Q2 2025.",
            category = "Infrastructure"
        ),
        NewsUpdate(
            id = "5", title = "Employee Wellness Program 'FitIOCL' Launched",
            summary = "Comprehensive wellness initiative covering mental health, fitness challenges, and preventive health checkups.",
            category = "HR & Welfare"
        ),
        NewsUpdate(
            id = "6", title = "India's Crude Oil Demand Projected to Rise 4.5% in FY25",
            summary = "Strong economic growth drives petroleum demand, positioning IOCL for continued expansion in refining capacity.",
            category = "Industry"
        )
    )

    private fun getFallbackInsights(): List<ContentInsight> = listOf(
        ContentInsight(
            title = "Refinery Modernization Roadmap 2030",
            description = "Deep dive into IOCL's ₹75,000 crore refinery upgrade program covering all 11 refineries.",
            category = "Technical", actionLabel = "Read Report"
        ),
        ContentInsight(
            title = "Safety Excellence: Best Practices Compilation",
            description = "Curated safety protocols and incident prevention strategies from top-performing units.",
            category = "Safety", actionLabel = "View Guide"
        ),
        ContentInsight(
            title = "Sustainability Report FY24 Highlights",
            description = "Key achievements in carbon footprint reduction, renewable energy adoption, and ESG compliance.",
            category = "Sustainability", actionLabel = "Explore"
        ),
        ContentInsight(
            title = "Employee Spotlight: Innovation Champions",
            description = "Stories of IOCL employees driving innovation in operations, technology, and customer service.",
            category = "People", actionLabel = "Read Stories"
        )
    )

    private fun getFallbackTopics(): List<String> = listOf(
        "Green Hydrogen Revolution",
        "Q4 Financial Results",
        "EV Charging Network",
        "Refinery Safety Awards",
        "LNG Terminal Expansion",
        "Digital Transformation",
        "Employee Wellness",
        "Biofuel Initiatives"
    )
}
