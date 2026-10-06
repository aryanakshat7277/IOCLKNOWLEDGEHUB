package com.iocl.knowledgehub.data.remote.firebase

import com.google.firebase.storage.FirebaseStorage
import com.iocl.knowledgehub.domain.model.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadPdf(path: String, data: ByteArray): Resource<String> {
        return try {
            val ref = storage.reference.child(path)
            ref.putBytes(data).await()
            val downloadUrl = ref.downloadUrl.await()
            Resource.Success(downloadUrl.toString())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Upload failed")
        }
    }

    suspend fun uploadImage(path: String, data: ByteArray): Resource<String> {
        return try {
            val ref = storage.reference.child(path)
            ref.putBytes(data).await()
            val downloadUrl = ref.downloadUrl.await()
            Resource.Success(downloadUrl.toString())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Image upload failed")
        }
    }

    suspend fun deleteFile(path: String): Resource<Unit> {
        return try {
            storage.reference.child(path).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed")
        }
    }

    suspend fun getDownloadUrl(path: String): Resource<String> {
        return try {
            val url = storage.reference.child(path).downloadUrl.await()
            Resource.Success(url.toString())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get download URL")
        }
    }
}
