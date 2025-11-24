package com.example.androidapp.data.remote.api

import com.example.androidapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // Login
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/signup")
   suspend fun signup(
        @Body request:SignupRequest
    ): Response<AuthResponse>

    @POST("/auth/confirm")
    suspend fun confirmSignup(@Body request: ConfirmSignupRequest): Response<Unit>
    // Refresh token
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    // Health check
    @GET("health")
    suspend fun healthCheck(): Response<String>
}
