package com.example.androidapp.ui

import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import com.example.androidapp.ChannelManager
import timber.log.Timber

class FlutterDashboardActivity : FlutterActivity() {

    private val TAG = "FlutterDashboard"
    private val ENGINE_ID = "main" // Use cached engine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("FlutterDashboardActivity onCreate called")
        Log.d(TAG, "FlutterDashboardActivity onCreate called")

        // Extract user data from Intent
        val email = intent.getStringExtra("email")
        val userId = intent.getStringExtra("userId")
        val token = intent.getStringExtra("token")

        if (!email.isNullOrEmpty() && !userId.isNullOrEmpty() && !token.isNullOrEmpty()) {
            ChannelManager.setUserSession(email, userId, token)
            Timber.d("User session set in ChannelManager")
            Log.d(TAG, "User session stored in ChannelManager")
        } else {
            Timber.w("Missing user data in intent extras")
        }
    }

    // Return cached FlutterEngine
    override fun getCachedEngineId(): String = ENGINE_ID

    // No need to override configureFlutterEngine when using cached engine
}
