package com.iocl.knowledgehub.data.local.dao

import androidx.room.*
import com.iocl.knowledgehub.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun getAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE documentId = :documentId)")
    suspend fun isBookmarked(documentId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
