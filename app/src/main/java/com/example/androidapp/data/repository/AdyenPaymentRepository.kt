package com.example.androidapp.data.repository

import android.content.Context
import com.adyen.checkout.components.core.CheckoutConfiguration
import com.adyen.checkout.core.Environment
import com.adyen.checkout.dropin.DropInResult
import com.adyen.checkout.sessions.core.CheckoutSession
import com.adyen.checkout.sessions.core.CheckoutSessionProvider
import com.adyen.checkout.sessions.core.CheckoutSessionResult
import com.adyen.checkout.sessions.core.SessionModel
import com.example.androidapp.data.remote.api.ApiService
import com.example.androidapp.data.remote.dto.PaymentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AdyenPaymentRepository(
    private val apiService: ApiService,
    private val context: Context
) {
    private val merchantAccount = "EyepaxECOM"
    val clientKey = "test_VRXGF4MLT5D7LPDMNP56PLDU5UWBHUSW"
    val environment = Environment.TEST

    suspend fun createPaymentSession(amount: String, currency: String): Result<SessionData> =
        withContext(Dispatchers.IO) {
            try {
                val request = PaymentRequest(
                    amount = amount,
                    currency = currency,
                    merchantAccount = merchantAccount,
                    reference = "TXN_${System.currentTimeMillis()}",
                    returnUrl = "adyencheckout://com.example.androidapp"
                )

                val response = apiService.initiatePayment(request)

                if (response.isSuccessful) {
                    val paymentResponse = response.body()
                    if (paymentResponse != null && paymentResponse.success && paymentResponse.data?.rawResponse != null) {
                        val raw = paymentResponse.data.rawResponse

                        val sessionData = SessionData(
                            id = raw.id ?: "",
                            sessionData = raw.sessionData ?: "",
                            amount = amount,
                            currency = currency
                        )

                        Result.success(sessionData)
                    } else {
                        Result.failure(Exception(paymentResponse?.message ?: "Session creation failed"))
                    }
                } else {
                    Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating payment session")
                Result.failure(e)
            }
        }

    suspend fun initializeCheckoutSession(sessionData: SessionData): Result<CheckoutSession> =
        withContext(Dispatchers.IO) {
            try {
                // Create SessionModel from the session data
                val sessionModel = SessionModel(
                    id = sessionData.id,
                    sessionData = sessionData.sessionData
                )

                // Create checkout configuration
                val checkoutConfiguration = CheckoutConfiguration(
                    environment = environment,
                    clientKey = clientKey
                )

                // Create checkout session
                val result = CheckoutSessionProvider.createSession(
                    sessionModel = sessionModel,
                    configuration = checkoutConfiguration
                )

                when (result) {
                    is CheckoutSessionResult.Success -> {
                        Result.success(result.checkoutSession)
                    }
                    is CheckoutSessionResult.Error -> {
                        Result.failure(result.exception)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing checkout session")
                Result.failure(e)
            }
        }

    fun handleDropInResult(result: DropInResult): PaymentResult {
        return when (result) {
            is DropInResult.Finished -> {
                PaymentResult(
                    success = true,
                    message = "Payment completed",
                    transactionId = result.result,
                    resultCode = "AUTHORISED",
                    requiresAction = false
                )
            }
            is DropInResult.CancelledByUser -> {
                PaymentResult(
                    success = false,
                    message = "Payment cancelled by user",
                    transactionId = null,
                    resultCode = "CANCELLED",
                    requiresAction = false
                )
            }
            is DropInResult.Error -> {
                PaymentResult(
                    success = false,
                    message = result.reason ?: "Payment error",
                    transactionId = null,
                    resultCode = "ERROR",
                    requiresAction = false
                )
            }
        }
    }
}

data class SessionData(
    val id: String,
    val sessionData: String,
    val amount: String,
    val currency: String
)

data class PaymentResult(
    val success: Boolean,
    val message: String,
    val transactionId: String?,
    val resultCode: String,
    val requiresAction: Boolean = false,
    val actionData: Map<String, String>? = null
)