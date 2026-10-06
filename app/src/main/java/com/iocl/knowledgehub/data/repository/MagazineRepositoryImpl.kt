package com.iocl.knowledgehub.data.repository

import com.iocl.knowledgehub.data.local.dao.MagazineDao
import com.iocl.knowledgehub.data.mapper.MagazineMapper
import com.iocl.knowledgehub.data.remote.cloudinary.CloudinaryStorageSource
import com.iocl.knowledgehub.data.remote.firebase.FirestoreSource
import com.iocl.knowledgehub.domain.model.Magazine
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.MagazineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import com.iocl.knowledgehub.data.repository.DashboardContentProvider

@Singleton
class MagazineRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreSource,
    private val storageSource: CloudinaryStorageSource,
    private val magazineDao: MagazineDao,
    private val mapper: MagazineMapper,
    private val contentProvider: DashboardContentProvider
) : MagazineRepository {

    override fun getMagazines(): Flow<Resource<List<Magazine>>> = flow {
        emit(Resource.Loading)
        val localMagazines = try { contentProvider.fetchMagazines() } catch (e: Exception) { emptyList() }
        
        try {
            firestoreSource.getMagazines().collect { firestoreMagazines ->
                magazineDao.deleteAll()
                val combined = localMagazines + firestoreMagazines
                val sorted = combined.sortedByDescending { it.uploadedAt }
                magazineDao.insertAll(sorted.map { mapper.domainToEntity(it) })
                emit(Resource.Success(sorted))
            }
        } catch (e: Exception) {
            val cached = try { magazineDao.getAll().first().map { mapper.entityToDomain(it) } } catch (e: Exception) { emptyList() }
            val combinedFallback = (localMagazines + cached).distinctBy { it.id }.sortedByDescending { it.uploadedAt }
            
            if (combinedFallback.isNotEmpty()) {
                emit(Resource.Success(combinedFallback))
            } else {
                emit(Resource.Error(e.message ?: "Failed to load magazines"))
            }
        }
    }

    override fun getMagazinesByYear(year: Int): Flow<Resource<List<Magazine>>> = flow {
        emit(Resource.Loading)
        val localMagazines = try { contentProvider.fetchMagazines(year) } catch (e: Exception) { emptyList() }
        val localFiltered = localMagazines.filter { it.year == year }
        
        try {
            firestoreSource.getMagazinesByYear(year).collect { firestoreMagazines ->
                magazineDao.deleteAll()
                val combined = localFiltered + firestoreMagazines
                val sorted = combined.sortedByDescending { it.uploadedAt }
                magazineDao.insertAll(sorted.map { mapper.domainToEntity(it) })
                emit(Resource.Success(sorted))
            }
        } catch (e: Exception) {
            val cached = try { magazineDao.getByYear(year).first().map { mapper.entityToDomain(it) } } catch (e: Exception) { emptyList() }
            val combinedFallback = (localFiltered + cached).distinctBy { it.id }.sortedByDescending { it.uploadedAt }
            
            if (combinedFallback.isNotEmpty()) {
                emit(Resource.Success(combinedFallback))
            } else {
                emit(Resource.Error(e.message ?: "Failed to load magazines for year $year"))
            }
        }
    }

    override fun getMagazinesByMonthAndYear(month: Int, year: Int): Flow<Resource<List<Magazine>>> = flow {
        emit(Resource.Loading)
        magazineDao.getByMonthAndYear(month, year).collect { entities ->
            emit(Resource.Success(entities.map { mapper.entityToDomain(it) }))
        }
    }.catch { e ->
        emit(Resource.Error(e.message ?: "Failed to load magazines"))
    }

    override fun searchMagazines(query: String): Flow<Resource<List<Magazine>>> = flow {
        emit(Resource.Loading)
        magazineDao.search(query).collect { entities ->
            emit(Resource.Success(entities.map { mapper.entityToDomain(it) }))
        }
    }.catch { e ->
        emit(Resource.Error(e.message ?: "Search failed"))
    }

    override suspend fun getMagazineById(id: String): Resource<Magazine> {
        return try {
            val magazine = firestoreSource.getMagazineById(id)
            if (magazine != null) {
                Resource.Success(magazine)
            } else {
                val entity = magazineDao.getById(id)
                if (entity != null) Resource.Success(mapper.entityToDomain(entity))
                else Resource.Error("Magazine not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get magazine")
        }
    }

    override suspend fun uploadMagazine(
        magazine: Magazine,
        pdfBytes: ByteArray,
        coverBytes: ByteArray?
    ): Resource<Magazine> {
        return try {
            val pdfPath = "magazines/pdfs/${System.currentTimeMillis()}.txt"
            val pdfResult = storageSource.uploadPdf(pdfPath, pdfBytes)
            if (pdfResult is Resource.Error) return Resource.Error(pdfResult.message)
            val pdfUrl = (pdfResult as Resource.Success).data

            var coverUrl = ""
            if (coverBytes != null) {
                val coverPath = "magazines/covers/${System.currentTimeMillis()}.jpg"
                val coverResult = storageSource.uploadImage(coverPath, coverBytes)
                if (coverResult is Resource.Success) coverUrl = coverResult.data
            }

            val newMagazine = magazine.copy(pdfUrl = pdfUrl, coverImageUrl = coverUrl)
            val id = firestoreSource.addMagazine(newMagazine)
            Resource.Success(newMagazine.copy(id = id))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Upload failed")
        }
    }

    override suspend fun deleteMagazine(id: String): Resource<Unit> {
        return try {
            firestoreSource.deleteMagazine(id)
            magazineDao.deleteById(id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed")
        }
    }

    override suspend fun updateMagazine(magazine: Magazine): Resource<Magazine> {
        return try {
            firestoreSource.updateMagazine(magazine)
            magazineDao.insert(mapper.domainToEntity(magazine))
            Resource.Success(magazine)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override suspend fun incrementViewCount(id: String) {
        try {
            firestoreSource.incrementMagazineViewCount(id)
        } catch (_: Exception) {
        }
    }

    override suspend fun incrementDownloadCount(id: String) {
        try {
            firestoreSource.incrementMagazineDownloadCount(id)
        } catch (_: Exception) {
        }
    }
}
