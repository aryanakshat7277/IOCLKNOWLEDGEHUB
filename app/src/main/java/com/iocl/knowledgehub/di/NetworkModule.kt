package com.iocl.knowledgehub.di

import com.google.ai.client.generativeai.GenerativeModel
import com.iocl.knowledgehub.BuildConfig
import com.iocl.knowledgehub.data.remote.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.iocl-knowledgehub.com/"
    private const val GROQ_BASE_URL = "https://api.groq.com/"
    private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/"
    private const val OPENAI_BASE_URL = "https://api.openai.com/"
    private const val OPENROUTER_BASE_URL = "https://openrouter.ai/"

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("X-App-Version", "1.0.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton
    fun provideIOCLApiService(retrofit: Retrofit): IOCLApiService =
        retrofit.create(IOCLApiService::class.java)

    @Provides @Singleton
    fun provideGenerativeModel(): GenerativeModel =
        GenerativeModel(modelName = "gemini-pro", apiKey = BuildConfig.GEMINI_API_KEY)

    @Provides @Singleton
    fun provideGroqApiService(okHttpClient: OkHttpClient): GroqApiService =
        Retrofit.Builder().baseUrl(GROQ_BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(GroqApiService::class.java)

    @Provides @Singleton @Named("groq_api_key")
    fun provideGroqApiKey(): String = BuildConfig.GROQ_API_KEY

    @Provides @Singleton
    fun provideDeepSeekApiService(okHttpClient: OkHttpClient): DeepSeekApiService =
        Retrofit.Builder().baseUrl(DEEPSEEK_BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(DeepSeekApiService::class.java)

    @Provides @Singleton @Named("deepseek_api_key")
    fun provideDeepSeekApiKey(): String = BuildConfig.DEEPSEEK_API_KEY

    @Provides @Singleton
    fun provideOpenAIApiService(okHttpClient: OkHttpClient): OpenAIApiService =
        Retrofit.Builder().baseUrl(OPENAI_BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(OpenAIApiService::class.java)

    @Provides @Singleton @Named("openai_api_key")
    fun provideOpenAIApiKey(): String = BuildConfig.OPENAI_API_KEY

    @Provides @Singleton
    fun provideOpenRouterApiService(okHttpClient: OkHttpClient): OpenRouterApiService =
        Retrofit.Builder().baseUrl(OPENROUTER_BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(OpenRouterApiService::class.java)

    @Provides @Singleton @Named("openrouter_api_key")
    fun provideOpenRouterApiKey(): String = BuildConfig.OPENROUTER_API_KEY

    private const val CLOUDINARY_BASE_URL = "https://api.cloudinary.com/v1_1/"

    @Provides @Singleton
    fun provideCloudinaryApiService(okHttpClient: OkHttpClient): CloudinaryApiService =
        Retrofit.Builder().baseUrl(CLOUDINARY_BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(CloudinaryApiService::class.java)
}
