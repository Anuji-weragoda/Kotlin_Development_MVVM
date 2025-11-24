package com.example.androidapp.data.remote.interceptor

import com.example.androidapp.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber


class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    companion object {
        // Endpoints that don't need authentication
        private val UNAUTHENTICATED_ENDPOINTS = listOf(
            "/auth/login",
            "/auth/signup",
            "/auth/forgot-password",
            "/auth/confirm-password",
            "/auth/refresh",
            "/health"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Check if this endpoint needs authentication
        val needsAuth = UNAUTHENTICATED_ENDPOINTS.none { path.contains(it) }

        if (!needsAuth) {
            // No auth needed, proceed with original request
            Timber.d("Request to $path - No auth required")
            return chain.proceed(originalRequest)
        }

        // Get access token from TokenManager
        // Using runBlocking because Interceptor is synchronous
        val accessToken = runBlocking {
            tokenManager.getAccessToken().first()
        }

        if (accessToken.isNullOrEmpty()) {
            Timber.w("No access token available for $path")
            return chain.proceed(originalRequest)
        }

        // Add Authorization header with Bearer token
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        Timber.d("Request to $path - Auth header added")
        return chain.proceed(authenticatedRequest)
    }
}