package com.example.androidapp.data.repository

import com.example.androidapp.data.local.TokenManager
import com.example.androidapp.data.remote.RetrofitClient
import com.example.androidapp.data.remote.dto.AuthResponse
import com.example.androidapp.data.remote.dto.LoginRequest
import com.example.androidapp.data.remote.dto.SignupRequest
import com.example.androidapp.data.remote.dto.ConfirmSignupRequest
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

    // Login
    suspend fun login(email: String, password: String): Flow<Resource<AuthResponse>> = flow {
        try {
            emit(Resource.Loading)

            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful) {
                val authData = response.body()
                if (authData != null) {
                    tokenManager.saveTokens(
                        authData.accessToken ?: "",
                        authData.refreshToken ?: "")
                    tokenManager.saveUserInfo(
                        authData.userAttributes?.sub ?: "",
                        authData.userAttributes?.email ?: "",
                        ""
                    )
                    emit(Resource.Success(authData))
                    Timber.d("Login successful: ${authData.userAttributes ?.email ?: ""}")
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

    // Signup (only create user, do NOT save tokens yet)
    suspend fun signup(email: String, password: String, fullName: String): Flow<Resource<AuthResponse>> = flow {
        try {
            emit(Resource.Loading)

            val response = apiService.signup(SignupRequest(email, password, fullName))

            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    // Do NOT save tokens yet because user is not confirmed
                    emit(Resource.Success(data))
                } else {
                    emit(Resource.Error("Empty signup response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(errorBody ?: "Signup failed", response.code().toString()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error", "NETWORK_ERROR"))
            Timber.e(e, "Signup exception")
        }
    }

    suspend fun confirmSignup(email: String, code: String): Boolean {
        return try {
            val response = apiService.confirmSignup(ConfirmSignupRequest(email, code))
            response.isSuccessful
        } catch (e: Exception) {
            Timber.e(e, "Confirm signup exception")
            false
        }
    }

    // Logout
    suspend fun logout(): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading)

            val response = apiService.logout()

            if (response.isSuccessful || response.code() == 302) {
                // Clear local tokens
                tokenManager.clearTokens()
                emit(Resource.Success(Unit))
                Timber.d("Logout successful")
            } else {
                // Clear tokens even if backend fails
                tokenManager.clearTokens()
                emit(Resource.Success(Unit))
                Timber.w("Logout backend call failed with code: ${response.code()}, but cleared local tokens")
            }
        } catch (e: Exception) {
            // Clear tokens even if network fails
            tokenManager.clearTokens()
            emit(Resource.Success(Unit))
            Timber.e(e, "Logout exception, but cleared local tokens")
        }
    }
}
