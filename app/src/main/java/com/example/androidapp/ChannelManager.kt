package com.example.androidapp

import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

object ChannelManager {
    private const val CHANNEL = "com.example.flutter/channel"

    fun setup(flutterEngine: FlutterEngine) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getDataFromAndroid" -> {
                    val data = "Hello from Android!"
                    result.success(data)
                }
                else -> result.notImplemented()
            }
        }
    }
}
