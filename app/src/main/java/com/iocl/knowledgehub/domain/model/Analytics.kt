package com.iocl.knowledgehub.domain.model

data class Analytics(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalMagazines: Int = 0,
    val totalNewspapers: Int = 0,
    val totalDownloads: Int = 0,
    val totalViews: Int = 0,
    val monthlyActiveUsers: Int = 0,
    val topMagazines: List<Magazine> = emptyList(),
    val topNewspapers: List<Newspaper> = emptyList(),
    val recentUploads: List<Magazine> = emptyList(),
    val categoryDistribution: Map<String, Int> = emptyMap()
)
