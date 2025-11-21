package com.example.androidapp.data.remote.interceptor

import com.example.androidapp.data.local.TokenManager
import com.example.androidapp.data.remote.api.ApiService
import com.example.androidapp.data.remote.dto.RefreshTokenRequest
import com.example.androidapp.data.remote.dto.AuthResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val apiService: ApiService
) : Authenticator {

    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val HEADER_RETRY_COUNT = "X-Retry-Count"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val retryCount = response.request.header(HEADER_RETRY_COUNT)?.toIntOrNull() ?: 0

        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.e("Max retry count reached. Token refresh failed.")
            return null
        }

        val refreshToken = runBlocking { tokenManager.getRefreshToken().first() }

        if (refreshToken.isNullOrEmpty()) {
            Timber.e("No refresh token available")
            runBlocking { tokenManager.clearTokens() }
            return null
        }

        synchronized(this) {
            val currentToken = runBlocking { tokenManager.getAccessToken().first() }
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentToken != requestToken) {
                Timber.d("Token already refreshed, retrying with new token")
                return buildRetryRequest(response.request, currentToken, retryCount)
            }

            try {
                val refreshResponse: retrofit2.Response<AuthResponse> = runBlocking {
                    apiService.refreshToken(RefreshTokenRequest(refreshToken))
                }

                if (refreshResponse.isSuccessful) {
                    val authResponse: AuthResponse? = refreshResponse.body()

                    if (authResponse != null) {
                        runBlocking {
                            tokenManager.saveTokens(
                                authResponse.accessToken,
                                authResponse.refreshToken
                            )
                        }

                        Timber.d("Token refreshed successfully")
                        return buildRetryRequest(response.request, authResponse.accessToken, retryCount)
                    }
                }

                Timber.e("Token refresh failed")
                runBlocking { tokenManager.clearTokens() }
                return null

            } catch (e: Exception) {
                Timber.e(e, "Exception during token refresh")
                runBlocking { tokenManager.clearTokens() }
                return null
            }
        }
    }

    private fun buildRetryRequest(
        originalRequest: Request,
        newToken: String?,
        currentRetryCount: Int
    ): Request? {
        if (newToken.isNullOrEmpty()) return null
        return originalRequest.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header(HEADER_RETRY_COUNT, (currentRetryCount + 1).toString())
            .build()
    }
}
