package com.example.androidapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.androidapp.AuthApplication
import com.example.androidapp.ChannelManager
import com.example.androidapp.ui.auth.login.LoginFragment
import kotlinx.coroutines.launch
import timber.log.Timber

class FlutterDashboardActivity : FlutterActivity() {

    private val TAG = "FlutterDashboard"
    private val ENGINE_ID = "main"
    private val CHANNEL_NAME = "com.example.flutter/dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("FlutterDashboardActivity onCreate called")
        Log.d(TAG, "FlutterDashboardActivity onCreate called")

        // Set this activity as the current activity for payment launches
        ChannelManager.setCurrentActivity(this)

        // Extract user data from Intent
        val email = intent.getStringExtra("email")
        val userId = intent.getStringExtra("userId")
        val token = intent.getStringExtra("token")

        if (!email.isNullOrEmpty() && !userId.isNullOrEmpty() && !token.isNullOrEmpty()) {
            // Save session in your ChannelManager
            ChannelManager.setUserSession(email, userId, token)
            Timber.d("User session set in ChannelManager")
            Log.d(TAG, "User session stored in ChannelManager")

            // Push session to Flutter
            getFlutterEngine()?.let { engine ->
                setupMethodChannels(engine, email, userId, token)
            }
        } else {
            Timber.w("Missing user data in intent extras")
            Log.w(TAG, "Missing user data in intent extras")
        }
    }

    private fun setupMethodChannels(engine: FlutterEngine, email: String, userId: String, token: String) {
        val channel = MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL_NAME)

        // Send initial user session
        val userSession = mapOf(
            "email" to email,
            "userId" to userId,
            "accessToken" to token
        )
        channel.invokeMethod("updateUserSession", userSession)
        Timber.d("User session sent to Flutter via MethodChannel")
        Log.d(TAG, "User session sent to Flutter via MethodChannel")

        // Set up method call handler for Flutter -> Native calls
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "logout" -> {
                    handleLogout()
                    result.success(true)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun handleLogout() {
        lifecycleScope.launch {
            try {
                Timber.d("Logout requested from Flutter")

                // Get TokenManager from Application
                val tokenManager = (application as AuthApplication).tokenManager

                // Clear all tokens and user data
                tokenManager.clearTokens()

                // Clear ChannelManager session
                ChannelManager.clearUserSession()

                Timber.d("User logged out successfully, navigating to MainActivity")

                // Navigate back to MainActivity (which contains LoginFragment)
                val intent = Intent(this@FlutterDashboardActivity, com.example.androidapp.ui.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Timber.e(e, "Error during logout")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("FlutterDashboardActivity onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        // Forward result to ChannelManager
        ChannelManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        // Ensure this activity is set as current when resumed
        ChannelManager.setCurrentActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the activity reference when destroyed
        ChannelManager.setCurrentActivity(null)
    }

    // Return cached FlutterEngine
    override fun getCachedEngineId(): String = ENGINE_ID

    // No need to override configureFlutterEngine when using cached engine
}
