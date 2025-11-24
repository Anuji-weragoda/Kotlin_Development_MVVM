package com.example.androidapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// Login request
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class SignupRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("fullName") val fullName: String
)

data class ConfirmSignupRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String
)

// Refresh token request
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

// Auth response
data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("idToken") val idToken: String? = null,
    @SerializedName("tokenType") val tokenType: String? = "Bearer",
    @SerializedName("expiresIn") val expiresIn: Long? = null,
    @SerializedName("userAttributes") val userAttributes: UserAttributes? = null,
    @SerializedName("userSub") val userSub: String? = null,
    @SerializedName("userConfirmed") val userConfirmed: Boolean? = null,
    @SerializedName("message") val message: String? = null
)

// User attributes inside AuthResponse
data class UserAttributes(
    @SerializedName("sub") val sub: String,
    @SerializedName("email") val email: String,
    @SerializedName("email_verified") val emailVerified: String // keep as String because server returns "true" or "false"
)
