package com.iocl.knowledgehub.domain.repository

import com.iocl.knowledgehub.domain.model.Magazine
import com.iocl.knowledgehub.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface MagazineRepository {
    fun getMagazines(): Flow<Resource<List<Magazine>>>
    fun getMagazinesByYear(year: Int): Flow<Resource<List<Magazine>>>
    fun getMagazinesByMonthAndYear(month: Int, year: Int): Flow<Resource<List<Magazine>>>
    fun searchMagazines(query: String): Flow<Resource<List<Magazine>>>
    suspend fun getMagazineById(id: String): Resource<Magazine>
    suspend fun uploadMagazine(magazine: Magazine, pdfBytes: ByteArray, coverBytes: ByteArray?): Resource<Magazine>
    suspend fun deleteMagazine(id: String): Resource<Unit>
    suspend fun updateMagazine(magazine: Magazine): Resource<Magazine>
    suspend fun incrementViewCount(id: String)
    suspend fun incrementDownloadCount(id: String)
}
