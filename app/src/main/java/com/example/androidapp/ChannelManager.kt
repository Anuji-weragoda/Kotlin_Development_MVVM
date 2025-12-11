package com.example.androidapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import timber.log.Timber
import com.example.androidapp.ui.payment.PaymentActivity
import java.lang.ref.WeakReference

object ChannelManager : PluginRegistry.ActivityResultListener {

    private var flutterEngine: FlutterEngine? = null
    private var dashboardChannel: MethodChannel? = null
    private var bluetoothChannel: MethodChannel? = null
    private var wifiChannel: MethodChannel? = null
    private var paymentChannel: MethodChannel? = null

    @Suppress("StaticFieldLeak")
    private var bluetoothHandler: BluetoothHandler? = null
    @Suppress("StaticFieldLeak")
    private var wifiHandler: WifiHandler? = null

    private var userSession: Map<String, String>? = null
    private var pendingPaymentResult: MethodChannel.Result? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    private const val DASHBOARD_CHANNEL = "com.example.flutter/dashboard"
    private const val PAYMENT_CHANNEL = "com.example.app/adyen"
    private const val PAYMENT_REQUEST_CODE = 1001
    private const val BLUETOOTH_CHANNEL = "com.example.androidapp/bluetooth"
    private const val WIFI_CHANNEL = "com.example.androidapp/wifi"
    private const val CACHED_ENGINE_ID = "main"


    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = if (activity != null) WeakReference(activity) else null
        Timber.d("Current activity set to: ${activity?.javaClass?.simpleName}")
    }

    fun setup(engine: FlutterEngine, context: Context) {
        flutterEngine = engine

        // Register engine in cache so FlutterActivity that expects cachedEngineId "main" can find it
        try {
            FlutterEngineCache.getInstance().put(CACHED_ENGINE_ID, engine)
            Timber.d("FlutterEngine cached with id='$CACHED_ENGINE_ID'")
        } catch (t: Throwable) {
            Timber.w(t, "Failed to put FlutterEngine into FlutterEngineCache")
        }

        Timber.d("ChannelManager.setup called; engineHash=%s, context=%s", engine.hashCode(), context.javaClass.simpleName)
        val appContext = context.applicationContext // Safe context

        // If context is an Activity, store it and use it
        val activity = if (context is Activity) {
            currentActivityRef = WeakReference(context)
            context
        } else null

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

        // Bluetooth Channel
        bluetoothChannel = MethodChannel(engine.dartExecutor.binaryMessenger, BLUETOOTH_CHANNEL)
        bluetoothHandler = if (activity != null) {
            BluetoothHandler(activity, bluetoothChannel!!)
        } else {
            BluetoothHandler(appContext, bluetoothChannel!!)
        }

        // WiFi Channel
        wifiChannel = MethodChannel(engine.dartExecutor.binaryMessenger, WIFI_CHANNEL)
        wifiHandler = if (activity != null) {
            WifiHandler(activity, wifiChannel!!)
        } else {
            WifiHandler(appContext, wifiChannel!!)
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

    // Forward Activity permission results to handlers so they can notify Flutter
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        try {
            bluetoothHandler?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } catch (t: Throwable) {
            Timber.w(t, "ChannelManager: bluetoothHandler onRequestPermissionsResult threw")
        }
        try {
            wifiHandler?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } catch (t: Throwable) {
            Timber.w(t, "ChannelManager: wifiHandler onRequestPermissionsResult threw")
        }
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


    fun dispose() {
        bluetoothHandler?.dispose()
        wifiHandler?.dispose()
        bluetoothHandler = null
        wifiHandler = null

        // Remove engine from cache so FlutterActivity won't find a stale engine
        try {
            FlutterEngineCache.getInstance().remove(CACHED_ENGINE_ID)
            Timber.d("FlutterEngine removed from cache id='$CACHED_ENGINE_ID'")
        } catch (t: Throwable) {
            Timber.w(t, "Failed to remove FlutterEngine from FlutterEngineCache")
        }

        flutterEngine = null
    }
}
