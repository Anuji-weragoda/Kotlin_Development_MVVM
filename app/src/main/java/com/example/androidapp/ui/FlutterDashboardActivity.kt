package com.example.androidapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.androidapp.AuthApplication
import com.example.androidapp.ChannelManager
import kotlinx.coroutines.launch
import timber.log.Timber

class FlutterDashboardActivity : FlutterActivity() {

    private val ENGINE_ID = "main"
    private val CHANNEL_NAME = "com.example.flutter/dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("FlutterDashboardActivity onCreate called")

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

            // Push session to Flutter
            getFlutterEngine()?.let { engine ->
                setupMethodChannels(engine, email, userId, token)
            }
        } else {
            Timber.w("Missing user data in intent extras")
        }
    }

    private fun setupMethodChannels(engine: FlutterEngine, email: String, userId: String, token: String) {
        val channel = MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL_NAME)

        // Get FCM token
        val fcmToken = (application as AuthApplication).getFCMToken()

        // Send initial user session
        val userSession = mutableMapOf(
            "email" to email,
            "userId" to userId,
            "accessToken" to token
        )

        // Add FCM token if available
        fcmToken?.let {
            userSession["fcmToken"] = it
            Timber.d("FCM token added to user session: ${it.take(20)}...")
        }

        channel.invokeMethod("updateUserSession", userSession)
        Timber.d("User session sent to Flutter via MethodChannel")

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

                // Get TokenManager and AuthRepository from Application
                val tokenManager = (application as AuthApplication).tokenManager
                val authRepository = com.example.androidapp.data.repository.AuthRepository(tokenManager)

                // Call backend logout and clear tokens
                authRepository.logout().collect { resource ->
                    when (resource) {
                        is com.example.androidapp.data.repository.Resource.Success -> {
                            Timber.d("Logout completed successfully")
                        }
                        is com.example.androidapp.data.repository.Resource.Error -> {
                            Timber.w("Logout warning: ${resource.message}")
                        }
                        is com.example.androidapp.data.repository.Resource.Loading -> {
                            Timber.d("Logging out...")
                        }
                    }
                }

                // Clear ChannelManager session
                ChannelManager.clearUserSession()

                Timber.d("User logged out successfully, navigating to MainActivity")

                // Navigate back to MainActivity (which contains LoginFragment)
                val intent = Intent(this@FlutterDashboardActivity, MainActivity::class.java).apply {
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


        ChannelManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()

        ChannelManager.setCurrentActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        ChannelManager.setCurrentActivity(null)
    }


    override fun getCachedEngineId(): String = ENGINE_ID


}
