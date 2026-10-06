package com.iocl.knowledgehub.domain.repository

import com.iocl.knowledgehub.domain.model.Newspaper
import com.iocl.knowledgehub.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface NewspaperRepository {
    fun getNewspapers(): Flow<Resource<List<Newspaper>>>
    fun getNewspapersByDateRange(startDate: Long, endDate: Long): Flow<Resource<List<Newspaper>>>
    fun searchNewspapers(query: String): Flow<Resource<List<Newspaper>>>
    suspend fun getNewspaperById(id: String): Resource<Newspaper>
    suspend fun uploadNewspaper(newspaper: Newspaper, pdfBytes: ByteArray, coverBytes: ByteArray?): Resource<Newspaper>
    suspend fun deleteNewspaper(id: String): Resource<Unit>
    suspend fun updateNewspaper(newspaper: Newspaper): Resource<Newspaper>
    suspend fun incrementViewCount(id: String)
    suspend fun incrementDownloadCount(id: String)
}
