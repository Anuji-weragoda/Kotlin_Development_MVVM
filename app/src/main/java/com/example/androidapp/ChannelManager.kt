package com.example.androidapp

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import timber.log.Timber

object ChannelManager {

    private var flutterEngine: FlutterEngine? = null
    private var dashboardChannel: MethodChannel? = null
    private var bluetoothChannel: MethodChannel? = null
    private var wifiChannel: MethodChannel? = null

    @Suppress("StaticFieldLeak")
    private var bluetoothHandler: BluetoothHandler? = null
    @Suppress("StaticFieldLeak")
    private var wifiHandler: WifiHandler? = null

    private var userSession: Map<String, String>? = null

    private const val DASHBOARD_CHANNEL = "com.example.flutter/dashboard"
    private const val BLUETOOTH_CHANNEL = "com.example.androidapp/bluetooth"
    private const val WIFI_CHANNEL = "com.example.androidapp/wifi"


    fun setup(engine: FlutterEngine, context: Context) {
        flutterEngine = engine
        val appContext = context.applicationContext // Safe context

        // --- Dashboard channel ---
        dashboardChannel = MethodChannel(engine.dartExecutor.binaryMessenger, DASHBOARD_CHANNEL)
        dashboardChannel?.setMethodCallHandler { call, result ->
            Timber.d("ChannelManager handler called: ${call.method}")
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


        bluetoothChannel = MethodChannel(engine.dartExecutor.binaryMessenger, BLUETOOTH_CHANNEL)
        bluetoothHandler = BluetoothHandler(appContext, bluetoothChannel!!)


        wifiChannel = MethodChannel(engine.dartExecutor.binaryMessenger, WIFI_CHANNEL)
        wifiHandler = WifiHandler(appContext, wifiChannel!!)
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
    }
}
