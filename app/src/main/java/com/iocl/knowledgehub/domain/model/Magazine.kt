package com.iocl.knowledgehub.domain.model

data class Magazine(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val pdfUrl: String = "https://en.wikipedia.org/api/rest_v1/page/pdf/Indian_Oil_Corporation",
    val month: Int = 1,
    val year: Int = 2024,
    val category: MagazineCategory = MagazineCategory.CORPORATE,
    val tags: List<String> = emptyList(),
    val pageCount: Int = 0,
    val fileSize: Long = 0,
    val viewCount: Int = 0,
    val downloadCount: Int = 0,
    val uploadedBy: String = "",
    val uploadedAt: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

enum class MagazineCategory {
    CORPORATE, TECHNICAL, SAFETY, HR, SUSTAINABILITY, INNOVATION, QUARTERLY_REPORT
}

fun Int.toMonthName(): String = when (this) {
    1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
    5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
    9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
    else -> "Unknown"
}
