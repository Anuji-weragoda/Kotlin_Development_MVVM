package com.example.androidapp.data.repository

import android.util.Log
import com.example.androidapp.data.remote.api.ApiService
import com.example.androidapp.data.remote.dto.PaymentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdyenPaymentRepository(private val apiService: ApiService) {

    private val merchantAccount = "Eyepax"

    suspend fun startPayment(
        amount: String,
        currency: String
    ): Result<PaymentResult> = withContext(Dispatchers.IO) {
        Log.d(TAG, "============ PAYMENT STARTED ============")
        Log.d(TAG, "Amount: $amount $currency")
        Log.d(TAG, "MerchantAccount: $merchantAccount")

        try {
            val request = PaymentRequest(
                amount = amount,
                currency = currency,
                merchantAccount = merchantAccount,
                reference = "TXN_${System.currentTimeMillis()}", // Generate unique reference
                returnUrl = "yourapp://payment-result" // For redirect flows
            )

            val response = apiService.initiatePayment(request)

            if (response.isSuccessful && response.body() != null) {
                val paymentResponse = response.body()!!

                if (paymentResponse.success && paymentResponse.data != null) {
                    val data = paymentResponse.data
                    val resultCode = data.resultCode ?: "UNKNOWN"
                    val pspReference = data.pspReference ?: "UNKNOWN"

                    Log.d(TAG, "Payment successful")
                    Log.d(TAG, "PSP Reference: $pspReference")
                    Log.d(TAG, "Result Code: $resultCode")
                    Log.d(TAG, "============ PAYMENT ENDED ============")

                    val result = PaymentResult(
                        success = true,
                        message = "Payment $resultCode",
                        transactionId = pspReference,
                        resultCode = resultCode,
                        requiresAction = data.action != null,
                        actionData = data.action
                    )

                    Result.success(result)
                } else {
                    Log.e(TAG, "Payment failed: ${paymentResponse.message}")
                    val result = PaymentResult(
                        success = false,
                        message = paymentResponse.message,
                        transactionId = null,
                        resultCode = "FAILED"
                    )
                    Result.success(result) // Still return success Result with failed PaymentResult
                }
            } else {
                val errorMsg = "Payment API error: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Payment exception", e)
            Log.d(TAG, "============ PAYMENT FAILED ============")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AdyenPaymentRepo"
    }
}

// Result model for easier handling
data class PaymentResult(
    val success: Boolean,
    val message: String,
    val transactionId: String?,
    val resultCode: String,
    val requiresAction: Boolean = false,
    val actionData: Map<String, Any>? = null
)