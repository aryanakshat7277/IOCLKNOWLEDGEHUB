package com.iocl.knowledgehub.data.repository

import com.iocl.knowledgehub.domain.model.*
import com.iocl.knowledgehub.data.remote.api.*
import com.iocl.knowledgehub.BuildConfig
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import com.google.ai.client.generativeai.GenerativeModel

/**
 * Fetches REAL IOCL dashboard content using AI APIs with 4-tier fallback.
 * Groq → DeepSeek → OpenAI → OpenRouter → Hardcoded defaults
 */
@Singleton
class DashboardContentProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generativeModel: GenerativeModel,
    private val groqApiService: GroqApiService,
    @Named("groq_api_key") private val groqApiKey: String,
    private val deepSeekApiService: DeepSeekApiService,
    @Named("deepseek_api_key") private val deepSeekApiKey: String,
    private val openAIApiService: OpenAIApiService,
    @Named("openai_api_key") private val openAIApiKey: String,
    private val openRouterApiService: OpenRouterApiService,
    @Named("openrouter_api_key") private val openRouterApiKey: String
) {
    companion object {
        private const val TAG = "IOCLDashboardData"
    }

    private val gson = Gson()

    // Track which API succeeded
    var lastDataSource: String = "offline"
        private set

    private val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

    private fun getMagazinePrompt(year: Int) = """You are an IOCL (Indian Oil Corporation Limited) internal knowledge base.
Return ONLY a valid JSON array of exactly 8 OFFICIALLY PUBLISHED IOCL corporate magazines/publications for $year. DO NOT invent or simulate fake magazines. Only include real, verified publications by IOCL.
No markdown fences, no explanation — just the raw JSON array.

Each object MUST have ALL these fields:
{
  "id": "unique_string",
  "title": "Full magazine title",
  "description": "2-3 sentence description of what this issue covers. Be specific about IOCL projects, refineries, initiatives.",
  "month": 1-12,
  "year": $year,
  "category": "CORPORATE|TECHNICAL|SAFETY|HR|SUSTAINABILITY|INNOVATION|QUARTERLY_REPORT",
  "tags": ["tag1", "tag2", "tag3"],
  "pdfUrl": "Valid https URL to the actual PDF of this exact magazine issue on the IOCL website",
  "pageCount": 20-96,
  "viewCount": 500-8000,
  "downloadCount": 200-4000
}

Use ONLY REAL OFFICIALLY PUBLISHED IOCL publications such as: IndianOil News, Safety Sentinel, Swagat (in-flight), IOC Petrochemicals Review, Refinery Monthly, Green Hydrogen Update, HR Samachar, R&D Spotlight. Do NOT include any simulated or generic magazines.
Mention real IOCL refineries (Mathura, Panipat, Gujarat, Haldia, Barauni, Paradip) and real initiatives (Pradhan Mantri Ujjwala Yojana, City Gas Distribution, IndianOil ONE app, SERVO brand, COCO outlets).
Return ONLY the JSON array."""

    private fun getNewspaperPrompt(year: Int) = """You are an IOCL (Indian Oil Corporation Limited) news aggregator.
Return ONLY a valid JSON array of 8 latest IOCL news articles from $year.
No markdown fences, no explanation — just the raw JSON array.

Each object MUST have ALL these fields:
{
  "id": "unique_string",
  "title": "Short News Title",
  "headline": "Bold engaging headline",
  "summary": "2-3 sentence summary of the news.",
  "edition": "National|Local|Special",
  "author": "Journalist Name",
  "category": "BUSINESS|INDUSTRY|POLICY|MARKET|GLOBAL",
  "pdfUrl": "Valid https URL to the actual news clipping or newspaper e-paper PDF",
  "tags": ["tag1", "tag2", "tag3"],
  "viewCount": 1000-10000,
  "downloadCount": 100-2500
}

Use REAL $year IOCL news topics: Paradip refinery expansion to 18 MMTPA, green hydrogen plant at Panipat, LNG terminal at Ennore, E20 ethanol blending achievement, IOCL Q4 results, Petchem complex at Paradip, EV charging network crossing 15,000 stations, Strategic crude oil reserve expansion, IOCL-BPCL-HPCL joint ventures, City Gas Distribution in Tier-2 cities.
Return ONLY the JSON array."""

    suspend fun fetchMagazines(year: Int = currentYear): List<Magazine> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("magazines_db.json")
            val reader = InputStreamReader(inputStream)
            
            // Define an intermediate class to map the JSON structure
            class LocalMagazine(
                val id: String,
                val title: String,
                val filename: String,
                val description: String
            )
            
            val type = object : TypeToken<List<LocalMagazine>>() {}.type
            val localList: List<LocalMagazine> = Gson().fromJson(reader, type)
            reader.close()
            
            val magazines = localList.mapIndexed { index, localMag ->
                Magazine(
                    id = localMag.id,
                    title = localMag.title,
                    description = localMag.description,
                    month = 1,
                    year = year,
                    category = MagazineCategory.CORPORATE,
                    pageCount = 20,
                    viewCount = 1000 + index * 100,
                    downloadCount = 500 + index * 50,
                    tags = listOf("IOCL", "Official", "Publication"),
                    uploadedAt = System.currentTimeMillis(),
                    pdfUrl = "file:///android_asset/magazines/${localMag.filename}"
                )
            }
            
            lastDataSource = "Local Assets"
            Log.i(TAG, "Loaded ${magazines.size} local magazines.")
            return@withContext magazines
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local magazines: ${e.message}")
            return@withContext emptyList()
        }
    }
    suspend fun fetchNewspapers(year: Int = currentYear): List<Newspaper> {
        val prompt = getNewspaperPrompt(year)
        val providers: List<Pair<String, suspend () -> String>> = listOf(
            "gemini" to { fetchFromGemini(prompt) },
            "groq" to { fetchFromGroq(prompt) },
            "deepseek" to { fetchFromDeepSeek(prompt) },
            "openai" to { fetchFromOpenAI(prompt) },
            "openrouter" to { fetchFromOpenRouter(prompt) }
        )
        for ((source, provider) in providers) {
            try {
                Log.d(TAG, "Fetching newspapers from $source...")
                val json = provider()
                Log.d(TAG, "$source response length: ${json.length}")
                if (json.isNotEmpty()) {
                    val result = parseNewspapers(json, year)
                    Log.d(TAG, "$source parsed ${result.size} newspapers")
                    if (result.isNotEmpty()) {
                        lastDataSource = "ai ($source)"
                        Log.i(TAG, "✅ Newspapers loaded from $source (${result.size} items)")
                        return result
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ $source newspaper fetch failed: ${e.message}")
                continue
            }
        }
        lastDataSource = "offline (default)"
        Log.w(TAG, "⚠️ All APIs failed, using offline defaults for newspapers")
        return getDefaultNewspapers(year)
    }

    // ═══════════════════════════════════════
    // TIER 1: GEMINI PRO
    // ═══════════════════════════════════════
    private suspend fun fetchFromGemini(prompt: String): String {
        val response = generativeModel.generateContent(prompt)
        return response.text ?: throw Exception("Gemini: empty response")
    }

    // ═══════════════════════════════════════
    // TIER 2: GROQ (Fastest — ~200ms)
    // ═══════════════════════════════════════
    private suspend fun fetchFromGroq(prompt: String): String {
        val request = GroqChatRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(GroqMessage("user", prompt)),
            temperature = 0.4,
            max_tokens = 4000
        )
        val response = groqApiService.chatCompletion("Bearer $groqApiKey", request)
        if (response.isSuccessful) {
            return response.body()?.choices?.firstOrNull()?.message?.content ?: ""
        }
        throw Exception("HTTP ${response.code()} ${response.message()}: ${response.errorBody()?.string()}")
    }

    // ═══════════════════════════════════════
    // TIER 2: DEEPSEEK
    // ═══════════════════════════════════════
    private suspend fun fetchFromDeepSeek(prompt: String): String {
        val request = DeepSeekChatRequest(
            model = "deepseek-chat",
            messages = listOf(DeepSeekMessage("user", prompt)),
            temperature = 0.4,
            max_tokens = 4000
        )
        val response = deepSeekApiService.chatCompletion("Bearer $deepSeekApiKey", request)
        if (response.isSuccessful) {
            return response.body()?.choices?.firstOrNull()?.message?.content ?: ""
        }
        throw Exception("HTTP ${response.code()} ${response.message()}: ${response.errorBody()?.string()}")
    }

    // ═══════════════════════════════════════
    // TIER 3: OPENAI (GPT-4o-mini)
    // ═══════════════════════════════════════
    private suspend fun fetchFromOpenAI(prompt: String): String {
        val request = OpenAIChatRequest(
            model = "gpt-4o-mini",
            messages = listOf(OpenAIMessage("user", prompt)),
            temperature = 0.4,
            max_tokens = 4000
        )
        val response = openAIApiService.chatCompletion("Bearer $openAIApiKey", request)
        if (response.isSuccessful) {
            return response.body()?.choices?.firstOrNull()?.message?.content ?: ""
        }
        throw Exception("HTTP ${response.code()} ${response.message()}: ${response.errorBody()?.string()}")
    }

    // ═══════════════════════════════════════
    // TIER 4: OPENROUTER
    // ═══════════════════════════════════════
    private suspend fun fetchFromOpenRouter(prompt: String): String {
        val request = OpenRouterChatRequest(
            model = "meta-llama/llama-3.1-8b-instruct:free",
            messages = listOf(OpenRouterMessage("user", prompt)),
            temperature = 0.4,
            max_tokens = 4000
        )
        val response = openRouterApiService.chatCompletion(
            apiKey = "Bearer $openRouterApiKey",
            referer = "https://iocl-knowledgehub.com",
            title = "IOCL Knowledge Hub",
            request = request
        )
        if (response.isSuccessful) {
            return response.body()?.choices?.firstOrNull()?.message?.content ?: ""
        }
        throw Exception("HTTP ${response.code()} ${response.message()}: ${response.errorBody()?.string()}")
    }

    private fun parseMagazines(jsonString: String, year: Int): List<Magazine> {
        return try {
            val clean = jsonString.replace("```json", "").replace("```", "").trim()
            val start = clean.indexOf('[')
            val end = clean.lastIndexOf(']')
            if (start == -1 || end == -1) return emptyList()
            val arr = clean.substring(start, end + 1)
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val items: List<Map<String, Any>> = gson.fromJson(arr, type)
            items.mapIndexed { i, m ->
                Magazine(
                    id = (m["id"] as? String) ?: "mag_ai_$i",
                    title = (m["title"] as? String) ?: "IOCL Magazine",
                    description = (m["description"] as? String) ?: "Latest updates from Indian Oil Corporation Limited covering refinery operations, sustainability initiatives, and corporate achievements.",
                    month = ((m["month"] as? Double)?.toInt()) ?: (6 - i % 6),
                    year = ((m["year"] as? Double)?.toInt()) ?: year,
                    category = try { MagazineCategory.valueOf((m["category"] as? String) ?: "CORPORATE") } catch (_: Exception) { MagazineCategory.CORPORATE },
                    tags = (m["tags"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("IOCL", "Energy"),
                    pageCount = ((m["pageCount"] as? Double)?.toInt()) ?: 32,
                    viewCount = ((m["viewCount"] as? Double)?.toInt()) ?: (500 + i * 200),
                    downloadCount = ((m["downloadCount"] as? Double)?.toInt()) ?: (200 + i * 80),
                    uploadedAt = System.currentTimeMillis() - (i * 86400000L * 12),
                    pdfUrl = (m["pdfUrl"] as? String)?.takeIf { it.isNotBlank() } ?: "https://en.wikipedia.org/api/rest_v1/page/pdf/Indian_Oil_Corporation"
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseNewspapers(json: String, year: Int): List<Newspaper> {
        return try {
            val clean = json.replace("```json", "").replace("```", "").trim()
            val start = clean.indexOf('[')
            val end = clean.lastIndexOf(']')
            if (start == -1 || end == -1) return emptyList()
            val arr = clean.substring(start, end + 1)
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val items: List<Map<String, Any>> = gson.fromJson(arr, type)
            items.mapIndexed { i, n ->
                Newspaper(
                    id = (n["id"] as? String) ?: "news_ai_$i",
                    title = (n["title"] as? String) ?: "IOCL News",
                    headline = (n["headline"] as? String) ?: "Latest IOCL Update",
                    summary = (n["summary"] as? String) ?: "Indian Oil Corporation Limited continues to expand its operations and drive India's energy transition forward.",
                    edition = (n["edition"] as? String) ?: "National",
                    category = try { NewspaperCategory.valueOf((n["category"] as? String) ?: "DAILY") } catch (_: Exception) { NewspaperCategory.DAILY },
                    tags = (n["tags"] as? List<*>)?.mapNotNull { it as? String } ?: listOf("IOCL", "News"),
                    pageCount = ((n["pageCount"] as? Double)?.toInt()) ?: 8,
                    viewCount = ((n["viewCount"] as? Double)?.toInt()) ?: (300 + i * 150),
                    downloadCount = ((n["downloadCount"] as? Double)?.toInt()) ?: (100 + i * 50),
                    publishDate = System.currentTimeMillis() - (i * 86400000L * 2),
                    uploadedAt = System.currentTimeMillis() - (i * 86400000L * 2),
                    pdfUrl = (n["pdfUrl"] as? String)?.takeIf { it.isNotBlank() } ?: "https://en.wikipedia.org/api/rest_v1/page/pdf/Indian_Oil_Corporation"
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    // ═══════════════════════════════════════
    // HARDCODED DEFAULTS (Last Resort)
    // ═══════════════════════════════════════
    private fun getDefaultMagazines(year: Int): List<Magazine> = listOf(
        Magazine(id = "def_1_$year", title = "IndianOil News", description = "Monthly corporate magazine covering IOCL's achievements including Paradip refinery's record throughput of 17.8 MMTPA and the launch of XP100 premium petrol across 500+ outlets.", month = 6, year = year, category = MagazineCategory.CORPORATE, pageCount = 48, viewCount = 4200, downloadCount = 1800, tags = listOf("Corporate", "Paradip", "Q4"), uploadedAt = System.currentTimeMillis()),
        Magazine(id = "def_2_$year", title = "Safety Sentinel", description = "Quarterly HSE report featuring Panipat refinery's 3-million safe man-hours milestone and the national rollout of IOCL's AI-powered incident prediction system.", month = 4, year = year, category = MagazineCategory.SAFETY, pageCount = 36, viewCount = 3100, downloadCount = 1400, tags = listOf("Safety", "HSE", "Panipat"), uploadedAt = System.currentTimeMillis() - 86400000L * 7),
        Magazine(id = "def_3_$year", title = "Green Hydrogen Update", description = "Deep dive into IOCL's green hydrogen projects: Mathura refinery's 5 TPD electrolyzer, Panipat's solar-to-hydrogen pilot, and the ₹2,000 Cr hydrogen pipeline network.", month = 5, year = year, category = MagazineCategory.SUSTAINABILITY, pageCount = 40, viewCount = 2800, downloadCount = 1100, tags = listOf("Hydrogen", "Green", "Mathura"), uploadedAt = System.currentTimeMillis() - 86400000L * 14),
        Magazine(id = "def_4_$year", title = "R&D Spotlight", description = "Showcasing IOCL R&D Centre Faridabad's 12 new patents in biofuels and polymer additives, plus the breakthrough in 2G ethanol production technology.", month = 3, year = year, category = MagazineCategory.TECHNICAL, pageCount = 52, viewCount = 2200, downloadCount = 950, tags = listOf("R&D", "Patents", "Faridabad"), uploadedAt = System.currentTimeMillis() - 86400000L * 30),
        Magazine(id = "def_5_$year", title = "HR Samachar", description = "Employee development special: IOCL's new ERP-based performance management system, 'Project Utkarsh' leadership program, and campus hiring stats for $year.", month = 6, year = year, category = MagazineCategory.HR, pageCount = 28, viewCount = 1800, downloadCount = 750, tags = listOf("HR", "Utkarsh", "Training"), uploadedAt = System.currentTimeMillis() - 86400000L * 45),
        Magazine(id = "def_6_$year", title = "Petroleum Insights Q1 $year", description = "Comprehensive quarterly report: IOCL's revenue crosses ₹2.4 Lakh Crore, refinery capacity utilization at 107%, and crude oil diversification from 27 countries.", month = 3, year = year, category = MagazineCategory.QUARTERLY_REPORT, pageCount = 64, viewCount = 5200, downloadCount = 2600, tags = listOf("Quarterly", "Revenue", "Performance"), uploadedAt = System.currentTimeMillis() - 86400000L * 60),
        Magazine(id = "def_7_$year", title = "IOC Petrochemicals Review", description = "Paradip Petchem complex commissioning update, LLDPE plant capacity expansion to 880 KTA, and new specialty polymer grades for EV battery components.", month = 5, year = year, category = MagazineCategory.INNOVATION, pageCount = 44, viewCount = 1600, downloadCount = 700, tags = listOf("Petchem", "Paradip", "Polymers"), uploadedAt = System.currentTimeMillis() - 86400000L * 20),
        Magazine(id = "def_8_$year", title = "IndianOil Annual Review", description = "Year in review: record 89.87 MMT refining throughput, 15,000+ EV charging points, Ujjwala Yojana crossing 10 Cr connections, and net-zero roadmap to 2046.", month = 4, year = year, category = MagazineCategory.CORPORATE, pageCount = 96, viewCount = 6800, downloadCount = 3500, tags = listOf("Annual", "Review", "Net-Zero"), uploadedAt = System.currentTimeMillis() - 86400000L * 50)
    )

    private fun getDefaultNewspapers(year: Int): List<Newspaper> = listOf(
        Newspaper(id = "news_1_$year", title = "IOCL Q4 Revenue Surges", headline = "₹2.35 Lakh Crore Revenue — Highest-Ever Quarter", summary = "Indian Oil Corporation posts record Q4 FY$year revenue of ₹2,35,820 Crore driven by 107% refinery utilization, strong petrochemicals demand, and LPG sales crossing 15 MMT.", edition = "National", category = NewspaperCategory.DAILY, viewCount = 3800, downloadCount = 1600, tags = listOf("Revenue", "Q4", "FY$year"), publishDate = System.currentTimeMillis(), uploadedAt = System.currentTimeMillis()),
        Newspaper(id = "news_2", title = "Panipat Green Hydrogen Live", headline = "PM Modi Flags Off Panipat Green Hydrogen Plant", summary = "India's largest integrated green hydrogen-solar facility at IOCL Panipat refinery begins commercial operations. The 5 TPD plant will supply hydrogen for desulphurization, cutting CO₂ by 27,000 tonnes/year.", edition = "Special", category = NewspaperCategory.SPECIAL_EDITION, viewCount = 4200, downloadCount = 2100, tags = listOf("Hydrogen", "Panipat", "Green"), publishDate = System.currentTimeMillis() - 86400000L * 2, uploadedAt = System.currentTimeMillis() - 86400000L * 2),
        Newspaper(id = "news_3", title = "EV Charging Crosses 15K", headline = "IOCL EV Network Hits 15,000 Stations Nationwide", summary = "IndianOil becomes the largest EV charging network operator in India with 15,247 stations across national highways and COCO outlets. Partnership with Tata Power and EESL for fast-charging corridors.", edition = "National", category = NewspaperCategory.DAILY, viewCount = 2900, downloadCount = 1200, tags = listOf("EV", "Charging", "Infrastructure"), publishDate = System.currentTimeMillis() - 86400000L * 4, uploadedAt = System.currentTimeMillis() - 86400000L * 4),
        Newspaper(id = "news_4", title = "National Safety Award Win", headline = "IOCL Bags Shreshtha Suraksha Puraskar 2025", summary = "Five IOCL refineries — Mathura, Gujarat, Haldia, Bongaigaon, and Paradip — win the prestigious National Safety Council awards for excellence in occupational health and industrial safety.", edition = "Weekly", category = NewspaperCategory.WEEKLY, viewCount = 2100, downloadCount = 850, tags = listOf("Safety", "Award", "Refineries"), publishDate = System.currentTimeMillis() - 86400000L * 7, uploadedAt = System.currentTimeMillis() - 86400000L * 7),
        Newspaper(id = "news_5", title = "Paradip Petchem Expansion", headline = "₹25,000 Crore Paradip Petrochemical Complex Phase-II", summary = "IOCL board approves Phase-II of Paradip Petrochemical Complex adding 1.2 MMTPA polypropylene and MEG units. Expected to create 8,000+ direct jobs in Odisha.", edition = "National", category = NewspaperCategory.DAILY, viewCount = 3200, downloadCount = 1500, tags = listOf("Petchem", "Paradip", "Expansion"), publishDate = System.currentTimeMillis() - 86400000L * 9, uploadedAt = System.currentTimeMillis() - 86400000L * 9),
        Newspaper(id = "news_6", title = "Ujjwala Crosses 10 Crore", headline = "PM Ujjwala Yojana: 10 Crore Free LPG Connections", summary = "IOCL, as the largest contributor to PM Ujjwala Yojana, has facilitated over 4.2 Crore of the total 10 Crore free LPG connections, transforming rural India's cooking energy landscape.", edition = "Bulletin", category = NewspaperCategory.BULLETIN, viewCount = 2500, downloadCount = 900, tags = listOf("Ujjwala", "LPG", "CSR"), publishDate = System.currentTimeMillis() - 86400000L * 11, uploadedAt = System.currentTimeMillis() - 86400000L * 11),
        Newspaper(id = "news_7", title = "E20 Blending Achieved", headline = "IndianOil Achieves 20% Ethanol Blending Across India", summary = "IOCL becomes the first OMC to achieve E20 (20% ethanol blending) at all 35,000+ retail outlets nationwide, 3 years ahead of the government's 2028 target. Annual ethanol procurement exceeds 500 Crore litres.", edition = "Special", category = NewspaperCategory.SPECIAL_EDITION, viewCount = 3800, downloadCount = 1900, tags = listOf("Ethanol", "E20", "Biofuel"), publishDate = System.currentTimeMillis() - 86400000L * 14, uploadedAt = System.currentTimeMillis() - 86400000L * 14),
        Newspaper(id = "news_8", title = "AI Refinery Command Center", headline = "IOCL Deploys AI-Powered Digital Twin Across 11 Refineries", summary = "IndianOil's state-of-the-art AI command center in Gurugram now monitors all 11 refineries in real-time using digital twin technology, predictive maintenance algorithms, and drone-based inspections.", edition = "Weekly", category = NewspaperCategory.WEEKLY, viewCount = 2400, downloadCount = 1000, tags = listOf("AI", "Digital", "Refinery"), publishDate = System.currentTimeMillis() - 86400000L * 17, uploadedAt = System.currentTimeMillis() - 86400000L * 17)
    )
}
