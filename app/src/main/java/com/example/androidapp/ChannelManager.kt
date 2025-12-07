package com.example.androidapp

import android.app.Activity
import android.content.Intent
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import timber.log.Timber
import com.example.androidapp.ui.payment.PaymentActivity
import java.lang.ref.WeakReference

object ChannelManager : PluginRegistry.ActivityResultListener {

    private var flutterEngine: FlutterEngine? = null
    private var dashboardChannel: MethodChannel? = null
    private var paymentChannel: MethodChannel? = null
    private var userSession: Map<String, String>? = null
    private var pendingPaymentResult: MethodChannel.Result? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    private const val DASHBOARD_CHANNEL = "com.example.flutter/dashboard"
    private const val PAYMENT_CHANNEL = "com.example.app/adyen"
    private const val PAYMENT_REQUEST_CODE = 1001


    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = if (activity != null) WeakReference(activity) else null
        Timber.d("Current activity set to: ${activity?.javaClass?.simpleName}")
    }

    fun setup(engine: FlutterEngine, activity: Activity) {
        flutterEngine = engine
        currentActivityRef = WeakReference(activity)

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

                    Timber.d("Starting payment flow: $amount $currency")

                    val activityToUse = currentActivityRef?.get()
                    if (activityToUse == null) {
                        result.error("NO_ACTIVITY", "No activity available to launch payment", null)
                        return@setMethodCallHandler
                    }


                    pendingPaymentResult = result


                    val intent = Intent(activityToUse, PaymentActivity::class.java).apply {
                        putExtra(PaymentActivity.EXTRA_AMOUNT, amount)
                        putExtra(PaymentActivity.EXTRA_CURRENCY, currency)
                    }
                    activityToUse.startActivityForResult(intent, PAYMENT_REQUEST_CODE)
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == PAYMENT_REQUEST_CODE) {
            val result = pendingPaymentResult
            pendingPaymentResult = null

            if (result == null) {
                Timber.e("No pending payment result callback")
                return false
            }

            when (data?.getStringExtra("result")) {
                PaymentActivity.RESULT_PAYMENT_SUCCESS -> {
                    val resultMap = mapOf(
                        "success" to true,
                        "message" to (data.getStringExtra("message") ?: "Payment successful"),
                        "transactionId" to (data.getStringExtra("transactionId") ?: ""),
                        "resultCode" to (data.getStringExtra("resultCode") ?: ""),
                        "requiresAction" to false
                    )
                    result.success(resultMap)
                }
                PaymentActivity.RESULT_PAYMENT_CANCELLED -> {
                    result.error(
                        "PAYMENT_CANCELLED",
                        "Payment cancelled by user",
                        null
                    )
                }
                PaymentActivity.RESULT_PAYMENT_FAILURE -> {
                    result.error(
                        "PAYMENT_FAILED",
                        data.getStringExtra("message") ?: "Payment failed",
                        null
                    )
                }
                else -> {
                    result.error(
                        "UNKNOWN_ERROR",
                        "Unknown payment result",
                        null
                    )
                }
            }
            return true
        }
        return false
    }

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