package com.iocl.knowledgehub.data.repository

import com.iocl.knowledgehub.data.local.dao.BookmarkDao
import com.iocl.knowledgehub.data.local.entity.BookmarkEntity
import com.iocl.knowledgehub.domain.model.Bookmark
import com.iocl.knowledgehub.domain.model.DocumentType
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override fun getBookmarks(): Flow<Resource<List<Bookmark>>> {
        return bookmarkDao.getAll().map<List<BookmarkEntity>, Resource<List<Bookmark>>> { entities ->
            Resource.Success(entities.map { entity ->
                Bookmark(
                    id = entity.id,
                    documentId = entity.documentId,
                    documentType = try {
                        DocumentType.valueOf(entity.documentType)
                    } catch (e: Exception) {
                        DocumentType.MAGAZINE
                    },
                    title = entity.title,
                    coverImageUrl = entity.coverImageUrl,
                    addedAt = entity.addedAt
                )
            })
        }.catch { e ->
            emit(Resource.Error(e.message ?: "Failed to load bookmarks"))
        }
    }

    override suspend fun addBookmark(bookmark: Bookmark): Resource<Unit> {
        return try {
            bookmarkDao.insert(
                BookmarkEntity(
                    id = bookmark.id.ifBlank { UUID.randomUUID().toString() },
                    documentId = bookmark.documentId,
                    documentType = bookmark.documentType.name,
                    title = bookmark.title,
                    coverImageUrl = bookmark.coverImageUrl,
                    addedAt = bookmark.addedAt
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add bookmark")
        }
    }

    override suspend fun removeBookmark(documentId: String): Resource<Unit> {
        return try {
            bookmarkDao.deleteByDocumentId(documentId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove bookmark")
        }
    }

    override suspend fun isBookmarked(documentId: String): Boolean {
        return try {
            bookmarkDao.isBookmarked(documentId)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun toggleBookmark(bookmark: Bookmark): Resource<Boolean> {
        return try {
            val isCurrentlyBookmarked = bookmarkDao.isBookmarked(bookmark.documentId)
            if (isCurrentlyBookmarked) {
                bookmarkDao.deleteByDocumentId(bookmark.documentId)
                Resource.Success(false)
            } else {
                bookmarkDao.insert(
                    BookmarkEntity(
                        id = bookmark.id.ifBlank { UUID.randomUUID().toString() },
                        documentId = bookmark.documentId,
                        documentType = bookmark.documentType.name,
                        title = bookmark.title,
                        coverImageUrl = bookmark.coverImageUrl,
                        addedAt = System.currentTimeMillis()
                    )
                )
                Resource.Success(true)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to toggle bookmark")
        }
    }
}
