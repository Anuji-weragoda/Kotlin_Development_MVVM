package com.example.androidapp.data.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.androidapp.BuildConfig
import com.example.androidapp.data.local.TokenManager
import com.example.androidapp.data.remote.api.ApiService
import com.example.androidapp.data.remote.interceptor.AuthInterceptor
import com.example.androidapp.data.remote.interceptor.LoggingInterceptorConfig
import com.example.androidapp.data.remote.interceptor.NetworkInterceptor
import com.example.androidapp.data.remote.interceptor.TokenAuthenticator
object RetrofitClient {
    private lateinit var tokenManager: TokenManager
    private var apiService: ApiService? = null

    fun initialize(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    fun getApiService(): ApiService {
        if (apiService == null) {
            apiService = createApiService()
        }
        return apiService!!
    }
    private fun createApiService(): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(createGson()))
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // Timeouts
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

            // Interceptors
            .addInterceptor(NetworkInterceptor())
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(LoggingInterceptorConfig.create(BuildConfig.DEBUG))

            // Authenticator for token refresh
            .authenticator(TokenAuthenticator(tokenManager, createTempApiService()))

            // Connection pool
            .retryOnConnectionFailure(true)

            .build()
    }

    private fun createTempApiService(): ApiService {
        val tempClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(NetworkInterceptor())
            .addInterceptor(LoggingInterceptorConfig.create(BuildConfig.DEBUG))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(tempClient)
            .addConverterFactory(GsonConverterFactory.create(createGson()))
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun createGson() = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    fun reset() {
        apiService = null
    }
}