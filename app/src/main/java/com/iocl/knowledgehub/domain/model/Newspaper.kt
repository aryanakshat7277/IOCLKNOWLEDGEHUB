package com.iocl.knowledgehub.domain.model

data class Newspaper(
    val id: String = "",
    val title: String = "",
    val headline: String = "",
    val summary: String = "",
    val coverImageUrl: String = "",
    val pdfUrl: String = "https://en.wikipedia.org/api/rest_v1/page/pdf/Indian_Oil_Corporation",
    val publishDate: Long = System.currentTimeMillis(),
    val edition: String = "",
    val category: NewspaperCategory = NewspaperCategory.DAILY,
    val tags: List<String> = emptyList(),
    val pageCount: Int = 0,
    val fileSize: Long = 0,
    val viewCount: Int = 0,
    val downloadCount: Int = 0,
    val uploadedBy: String = "",
    val uploadedAt: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

enum class NewspaperCategory {
    DAILY, WEEKLY, SPECIAL_EDITION, BULLETIN, CIRCULAR
}
