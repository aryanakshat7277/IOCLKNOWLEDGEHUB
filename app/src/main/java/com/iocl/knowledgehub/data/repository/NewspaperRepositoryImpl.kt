package com.iocl.knowledgehub.data.repository

import com.iocl.knowledgehub.data.local.dao.NewspaperDao
import com.iocl.knowledgehub.data.mapper.NewspaperMapper
import com.iocl.knowledgehub.data.remote.cloudinary.CloudinaryStorageSource
import com.iocl.knowledgehub.data.remote.firebase.FirestoreSource
import com.iocl.knowledgehub.domain.model.Newspaper
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.NewspaperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import com.iocl.knowledgehub.data.repository.DashboardContentProvider

@Singleton
class NewspaperRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreSource,
    private val storageSource: CloudinaryStorageSource,
    private val newspaperDao: NewspaperDao,
    private val mapper: NewspaperMapper,
    private val contentProvider: DashboardContentProvider
) : NewspaperRepository {

    override fun getNewspapers(): Flow<Resource<List<Newspaper>>> = flow {
        emit(Resource.Loading)
        try {
            firestoreSource.getNewspapers().collect { newspapers ->
                newspaperDao.insertAll(newspapers.map { mapper.domainToEntity(it) })
                emit(Resource.Success(newspapers))
            }
        } catch (e: Exception) {
            val cached = newspaperDao.getAll().first().map { mapper.entityToDomain(it) }
            if (cached.isNotEmpty()) {
                emit(Resource.Success(cached))
            } else {
                try {
                    val aiNewspapers = contentProvider.fetchNewspapers()
                    newspaperDao.insertAll(aiNewspapers.map { mapper.domainToEntity(it) })
                    emit(Resource.Success(aiNewspapers))
                } catch (aiError: Exception) {
                    emit(Resource.Error(e.message ?: "Failed to load newspapers"))
                }
            }
        }
    }

    override fun getNewspapersByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<Resource<List<Newspaper>>> = flow {
        emit(Resource.Loading)
        try {
            firestoreSource.getNewspapersByDateRange(startDate, endDate).collect { newspapers ->
                newspaperDao.insertAll(newspapers.map { mapper.domainToEntity(it) })
                emit(Resource.Success(newspapers))
            }
        } catch (e: Exception) {
            // Note: newspaperDao doesn't have getByDateRange, use getAll for fallback caching
            val cached = newspaperDao.getAll().first().map { mapper.entityToDomain(it) }
                .filter { it.publishDate in startDate..endDate }
            
            if (cached.isNotEmpty()) {
                emit(Resource.Success(cached))
            } else {
                try {
                    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = startDate }
                    val year = calendar.get(java.util.Calendar.YEAR)
                    val aiNewspapers = contentProvider.fetchNewspapers(year)
                    val filtered = aiNewspapers.filter { it.publishDate in startDate..endDate }
                    if (filtered.isNotEmpty()) {
                        newspaperDao.insertAll(filtered.map { mapper.domainToEntity(it) })
                        emit(Resource.Success(filtered))
                    } else {
                        emit(Resource.Error("No newspapers found in date range"))
                    }
                } catch (aiError: Exception) {
                    emit(Resource.Error(e.message ?: "Failed to load newspapers"))
                }
            }
        }
    }

    override fun searchNewspapers(query: String): Flow<Resource<List<Newspaper>>> = flow {
        emit(Resource.Loading)
        newspaperDao.search(query).collect { entities ->
            emit(Resource.Success(entities.map { mapper.entityToDomain(it) }))
        }
    }.catch { e ->
        emit(Resource.Error(e.message ?: "Search failed"))
    }

    override suspend fun getNewspaperById(id: String): Resource<Newspaper> {
        return try {
            val newspaper = firestoreSource.getNewspaperById(id)
            if (newspaper != null) {
                Resource.Success(newspaper)
            } else {
                val entity = newspaperDao.getById(id)
                if (entity != null) Resource.Success(mapper.entityToDomain(entity))
                else Resource.Error("Newspaper not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get newspaper")
        }
    }

    override suspend fun uploadNewspaper(
        newspaper: Newspaper,
        pdfBytes: ByteArray,
        coverBytes: ByteArray?
    ): Resource<Newspaper> {
        return try {
            val pdfPath = "newspapers/pdfs/${System.currentTimeMillis()}.txt"
            val pdfResult = storageSource.uploadPdf(pdfPath, pdfBytes)
            if (pdfResult is Resource.Error) return Resource.Error(pdfResult.message)
            val pdfUrl = (pdfResult as Resource.Success).data

            var coverUrl = ""
            if (coverBytes != null) {
                val coverPath = "newspapers/covers/${System.currentTimeMillis()}.jpg"
                val coverResult = storageSource.uploadImage(coverPath, coverBytes)
                if (coverResult is Resource.Success) coverUrl = coverResult.data
            }

            val newNewspaper = newspaper.copy(pdfUrl = pdfUrl, coverImageUrl = coverUrl)
            val id = firestoreSource.addNewspaper(newNewspaper)
            Resource.Success(newNewspaper.copy(id = id))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Upload failed")
        }
    }

    override suspend fun deleteNewspaper(id: String): Resource<Unit> {
        return try {
            firestoreSource.deleteNewspaper(id)
            newspaperDao.deleteById(id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed")
        }
    }

    override suspend fun updateNewspaper(newspaper: Newspaper): Resource<Newspaper> {
        return try {
            firestoreSource.updateNewspaper(newspaper)
            newspaperDao.insert(mapper.domainToEntity(newspaper))
            Resource.Success(newspaper)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    override suspend fun incrementViewCount(id: String) {
        try {
            firestoreSource.incrementNewspaperViewCount(id)
        } catch (_: Exception) {
        }
    }

    override suspend fun incrementDownloadCount(id: String) {
        try {
            firestoreSource.incrementNewspaperDownloadCount(id)
        } catch (_: Exception) {
        }
    }
}
