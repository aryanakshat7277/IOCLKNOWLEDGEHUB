package com.iocl.knowledgehub.data.remote.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

data class CloudinaryUploadResponse(
    val secure_url: String
)

interface CloudinaryApiService {
    @Multipart
    @POST("{cloud_name}/{resource_type}/upload")
    suspend fun uploadFile(
        @Path("cloud_name") cloudName: String,
        @Path("resource_type") resourceType: String,
        @Part file: MultipartBody.Part,
        @Part uploadPreset: MultipartBody.Part
    ): Response<CloudinaryUploadResponse>
}
