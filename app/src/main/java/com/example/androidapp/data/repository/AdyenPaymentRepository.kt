package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.api.ApiService
import com.example.androidapp.data.remote.dto.PaymentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdyenPaymentRepository(private val apiService: ApiService) {

    private val merchantAccount = "EyepaxECOM"

    suspend fun startPayment(amount: String, currency: String): Result<PaymentResult> =
        withContext(Dispatchers.IO) {
            try {
                val request = PaymentRequest(
                    amount = amount,
                    currency = currency,
                    merchantAccount = merchantAccount,
                    reference = "TXN_${System.currentTimeMillis()}",
                    returnUrl = "yourapp://payment-result"
                )

                val response = apiService.initiatePayment(request)

                if (response.isSuccessful) {
                    val paymentResponse = response.body()
                    if (paymentResponse != null && paymentResponse.success && paymentResponse.data?.rawResponse != null) {
                        val raw = paymentResponse.data.rawResponse
                        val sessionId = raw?.id ?: ""
                        val sessionData = raw?.sessionData ?: ""

                        val result = PaymentResult(
                            success = true,
                            message = "Session created",
                            transactionId = sessionId,
                            resultCode = "SESSION",
                            requiresAction = false,
                            actionData = mapOf(
                                "id" to sessionId,
                                "sessionData" to sessionData
                            )
                        )

                        Result.success(result)
                    } else {
                        val result = PaymentResult(
                            success = false,
                            message = paymentResponse?.message ?: "Unknown error",
                            transactionId = null,
                            resultCode = "FAILED",
                            requiresAction = false,
                            actionData = null
                        )
                        Result.success(result)
                    }
                } else {
                    Result.failure(Exception("Payment API error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

data class PaymentResult(
    val success: Boolean,
    val message: String,
    val transactionId: String?,
    val resultCode: String,
    val requiresAction: Boolean = false,
    val actionData: Map<String, String>? = null
)
