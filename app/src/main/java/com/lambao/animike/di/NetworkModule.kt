package com.lambao.animike.di

import com.lambao.animike.BuildConfig
import com.lambao.animike.data.remote.JikanApi
import com.lambao.animike.data.remote.interceptor.RateLimitInterceptor
import com.lambao.animike.data.remote.interceptor.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

private const val JIKAN_BASE_URL = "https://api.jikan.moe/v4/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // Retry đứng trước rate-limit trong chain: mỗi lần retry cũng đi
            // qua RateLimitInterceptor nên vẫn được giãn cách đúng 400ms.
            .addInterceptor(RetryInterceptor())
            .addInterceptor(RateLimitInterceptor())

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(JIKAN_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideJikanApi(retrofit: Retrofit): JikanApi = retrofit.create(JikanApi::class.java)
}
