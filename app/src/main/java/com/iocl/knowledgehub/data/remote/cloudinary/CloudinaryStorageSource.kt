package com.iocl.knowledgehub.data.remote.cloudinary

import com.iocl.knowledgehub.data.remote.api.CloudinaryApiService
import com.iocl.knowledgehub.domain.model.Resource
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudinaryStorageSource @Inject constructor(
    private val cloudinaryApi: CloudinaryApiService
) {
    private val cloudName = "qxnjdiyt"
    private val uploadPreset = "IOCLDATABASE"

    suspend fun uploadPdf(path: String, data: ByteArray): Resource<String> {
        return uploadToCloudinary(data, "application/octet-stream", path, "raw")
    }

    suspend fun uploadImage(path: String, data: ByteArray): Resource<String> {
        return uploadToCloudinary(data, "image/jpeg", path, "image")
    }

    private suspend fun uploadToCloudinary(data: ByteArray, mimeType: String, path: String, resourceType: String): Resource<String> {
        return try {
            val requestFile = data.toRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", path.substringAfterLast('/'), requestFile)
            val presetBody = MultipartBody.Part.createFormData("upload_preset", uploadPreset)
            
            val response = cloudinaryApi.uploadFile(cloudName, resourceType, body, presetBody)
            if (response.isSuccessful) {
                val url = response.body()?.secure_url
                if (url != null) {
                    Resource.Success(url)
                } else {
                    Resource.Error("Upload successful but URL is null")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Resource.Error("Cloudinary upload failed: ${response.code()} ${response.message()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Upload failed due to exception")
        }
    }
}
