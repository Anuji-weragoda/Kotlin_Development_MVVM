package com.example.androidapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ==================== AUTH DTOs ====================

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


data class PaymentRequest(
    @SerializedName("amount") val amount: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("merchantAccount") val merchantAccount: String? = null,
    @SerializedName("reference") val reference: String? = null,
    @SerializedName("returnUrl") val returnUrl: String? = null
)


data class PaymentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: AdyenSessionData? = null
)


data class AdyenSessionData(
    @SerializedName("rawResponse") val rawResponse: RawResponse? = null
)

data class RawResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("sessionData") val sessionData: String?
)
