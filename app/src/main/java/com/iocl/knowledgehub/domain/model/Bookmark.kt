package com.iocl.knowledgehub.domain.model

data class Bookmark(
    val id: String = "",
    val documentId: String = "",
    val documentType: DocumentType = DocumentType.MAGAZINE,
    val title: String = "",
    val coverImageUrl: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

enum class DocumentType {
    MAGAZINE, NEWSPAPER
}
