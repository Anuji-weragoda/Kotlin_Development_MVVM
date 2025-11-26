package com.example.androidapp

import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import timber.log.Timber

object ChannelManager {

    private var flutterEngine: FlutterEngine? = null
    private var methodChannel: MethodChannel? = null
    private var userSession: Map<String, String>? = null

    private const val CHANNEL = "com.example.flutter/dashboard"

    // Initialize MethodChannel once
    fun setup(engine: FlutterEngine) {
        flutterEngine = engine

        methodChannel = MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel?.setMethodCallHandler { call, result ->
            Timber.d("ChannelManager handler called: ${call.method}")
            when (call.method) {
                "getUserSession" -> {
                    if (userSession != null && userSession!!.isNotEmpty()) {
                        result.success(userSession)
                    } else {
                        result.error("NO_SESSION", "User session not found", null)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    fun setUserSession(email: String, userId: String, token: String) {
        userSession = mapOf(
            "email" to email,
            "userId" to userId,
            "token" to token
        )
        Timber.d("User session set: $email, $userId")

        // Immediately notify Flutter if MethodChannel is ready
        methodChannel?.invokeMethod("updateUserSession", userSession)
    }

    fun getUserSession(): Map<String, String>? = userSession

    fun clearUserSession() {
        userSession = null
        Timber.d("User session cleared")
    }
}
