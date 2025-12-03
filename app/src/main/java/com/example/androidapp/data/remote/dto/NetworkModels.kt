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

// ==================== PAYMENT DTOs ====================

// Payment request - matches your backend AdyenPaymentRequest
data class PaymentRequest(
    @SerializedName("amount") val amount: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("merchantAccount") val merchantAccount: String? = null,
    @SerializedName("reference") val reference: String? = null,
    @SerializedName("returnUrl") val returnUrl: String? = null
)

// Payment response - matches your backend ApiResponse structure
data class PaymentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: AdyenPaymentData? = null
)

// Adyen payment data - matches your backend AdyenPaymentResponse
data class AdyenPaymentData(
    @SerializedName("pspReference") val pspReference: String? = null,
    @SerializedName("resultCode") val resultCode: String? = null,
    @SerializedName("amount") val amount: PaymentAmount? = null,
    @SerializedName("merchantReference") val merchantReference: String? = null,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("refusalReason") val refusalReason: String? = null,
    @SerializedName("action") val action: Map<String, Any>? = null, // For 3DS or redirect actions
    @SerializedName("additionalData") val additionalData: Map<String, String>? = null
)

// Payment amount
data class PaymentAmount(
    @SerializedName("currency") val currency: String,
    @SerializedName("value") val value: Long // Adyen uses minor units (cents)
)