package com.example.androidapp

import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import timber.log.Timber
import com.example.androidapp.data.repository.AdyenPaymentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ChannelManager {

    private var flutterEngine: FlutterEngine? = null
    private var dashboardChannel: MethodChannel? = null
    private var paymentChannel: MethodChannel? = null
    private var userSession: Map<String, String>? = null

    // Add payment repository instance
    private var paymentRepository: AdyenPaymentRepository? = null

    private const val DASHBOARD_CHANNEL = "com.example.flutter/dashboard"
    private const val PAYMENT_CHANNEL = "com.example.app/adyen"

    // Call this from MainActivity to inject dependencies
    fun setPaymentRepository(repository: AdyenPaymentRepository) {
        paymentRepository = repository
    }

    fun setup(engine: FlutterEngine) {
        flutterEngine = engine

        // Dashboard Channel
        dashboardChannel = MethodChannel(engine.dartExecutor.binaryMessenger, DASHBOARD_CHANNEL)
        dashboardChannel?.setMethodCallHandler { call, result ->
            Timber.d("Dashboard channel handler called: ${call.method}")
            when (call.method) {
                "getUserSession" -> {
                    if (!userSession.isNullOrEmpty()) {
                        result.success(userSession)
                    } else {
                        result.error("NO_SESSION", "User session not found", null)
                    }
                }
                else -> result.notImplemented()
            }
        }

        // Payment Channel
        paymentChannel = MethodChannel(engine.dartExecutor.binaryMessenger, PAYMENT_CHANNEL)
        paymentChannel?.setMethodCallHandler { call, result ->
            Timber.d("Payment channel handler called: ${call.method}")
            when (call.method) {
                "startPayment" -> {
                    val amount = call.argument<String>("amount") ?: "0.0"
                    val currency = call.argument<String>("currency") ?: "USD"

                    Timber.d("Processing payment: $amount $currency")

                    if (paymentRepository == null) {
                        result.error("NO_REPOSITORY", "Payment repository not initialized", null)
                        return@setMethodCallHandler
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val paymentResult = withContext(Dispatchers.IO) {
                                paymentRepository!!.startPayment(amount, currency)
                            }

                            paymentResult.fold(
                                onSuccess = { paymentData ->
                                    Timber.d("Payment result: $paymentData")

                                    // Use nullable Any for values to be safe when marshaling across the MethodChannel
                                    val resultMap = mutableMapOf<String, Any?>(
                                        "success" to paymentData.success,
                                        "message" to paymentData.message,
                                        "transactionId" to (paymentData.transactionId ?: ""),
                                        "resultCode" to paymentData.resultCode,
                                        "requiresAction" to paymentData.requiresAction
                                    )

                                    // Convert actionData into a Java HashMap<String, String> so the platform channel receives a plain map
                                    paymentData.actionData?.let { actionData ->
                                        val safeMap = java.util.HashMap<String, String>()
                                        actionData.forEach { (k, v) ->
                                            // v is expected to be a String; convert defensively if needed
                                            safeMap[k] = v
                                        }

                                        resultMap["actionData"] = safeMap
                                    }


                                    result.success(resultMap)

                                },
                                onFailure = { error ->
                                    Timber.e("Payment failed: ${error.message}")
                                    result.error(
                                        "PAYMENT_FAILED",
                                        error.message ?: "Unknown error",
                                        null
                                    )
                                }
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Payment exception")
                            result.error("PAYMENT_ERROR", e.message ?: "Unknown error", null)
                        }
                    }
                }
                else -> result.notImplemented()
            }
        }
    } // <-- This closing brace was missing!

    fun setUserSession(email: String, userId: String, token: String) {
        userSession = mapOf(
            "email" to email,
            "userId" to userId,
            "token" to token
        )
        Timber.d("User session set: $email, $userId")
        dashboardChannel?.invokeMethod("updateUserSession", userSession)
    }

    fun getUserSession(): Map<String, String>? = userSession

    fun clearUserSession() {
        userSession = null
        Timber.d("User session cleared")
    }
}