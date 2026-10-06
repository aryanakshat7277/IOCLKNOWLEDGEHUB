package com.iocl.knowledgehub.domain.repository

import com.iocl.knowledgehub.domain.model.Bookmark
import com.iocl.knowledgehub.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun getBookmarks(): Flow<Resource<List<Bookmark>>>
    suspend fun addBookmark(bookmark: Bookmark): Resource<Unit>
    suspend fun removeBookmark(documentId: String): Resource<Unit>
    suspend fun isBookmarked(documentId: String): Boolean
    suspend fun toggleBookmark(bookmark: Bookmark): Resource<Boolean>
}
