package com.example.androidapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// Login request
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

// Refresh token request
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

// Auth response
data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("idToken") val idToken: String? = null,
    @SerializedName("tokenType") val tokenType: String = "Bearer",
    @SerializedName("expiresIn") val expiresIn: Long,
    @SerializedName("userAttributes") val userAttributes: UserAttributes
)

// User attributes inside AuthResponse
data class UserAttributes(
    @SerializedName("sub") val sub: String,
    @SerializedName("email") val email: String,
    @SerializedName("email_verified") val emailVerified: String // keep as String because server returns "true" or "false"
)
