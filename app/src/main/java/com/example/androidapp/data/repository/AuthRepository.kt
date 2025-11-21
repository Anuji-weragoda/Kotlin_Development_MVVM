package com.example.androidapp.data.repository

import com.example.androidapp.data.local.TokenManager
import com.example.androidapp.data.remote.RetrofitClient
import com.example.androidapp.data.remote.dto.AuthResponse
import com.example.androidapp.data.remote.dto.LoginRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: String? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

class AuthRepository(private val tokenManager: TokenManager) {

    private val apiService = RetrofitClient.getApiService()

    suspend fun login(email: String, password: String): Flow<Resource<AuthResponse>> = flow {
        try {
            emit(Resource.Loading)

            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful) {
                val authData = response.body() // directly AuthResponse now
                if (authData != null) {
                    tokenManager.saveTokens(authData.accessToken, authData.refreshToken)
                    tokenManager.saveUserInfo(
                        authData.userAttributes.sub,
                        authData.userAttributes.email,
                        "" // fullName placeholder
                    )

                    emit(Resource.Success(authData))
                    Timber.d("Login successful: ${authData.userAttributes.email}")
                } else {
                    emit(Resource.Error("Login failed: empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(errorBody ?: "Login failed", response.code().toString()))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error", "NETWORK_ERROR"))
            Timber.e(e, "Login exception")
        }
    }
}
