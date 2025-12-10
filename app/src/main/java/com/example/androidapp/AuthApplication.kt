package com.example.androidapp

import android.app.Application
import com.google.android.gms.tasks.OnCompleteListener
import timber.log.Timber
import com.example.androidapp.data.local.TokenManager
import com.example.androidapp.data.remote.RetrofitClient
import com.example.androidapp.data.repository.AuthRepository
import com.example.androidapp.ui.util.FCMHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging

class AuthApplication : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var firebaseAnalytics: FirebaseAnalytics
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Enable Analytics collection
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)

        // Initialize Firebase Cloud Messaging
        initializeFCM()

        // Create notification channels
        FCMHelper.createNotificationChannels(this)

        // Initialize Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize TokenManager
        tokenManager = TokenManager(applicationContext)

        // Initialize RetrofitClient
        RetrofitClient.initialize(tokenManager)

        // Initialize AuthRepository
        authRepository = AuthRepository(tokenManager)

        Timber.d("AuthApplication initialized")
        Timber.d("Firebase Analytics initialized")

        // Log app open event
        logEvent("app_started", null)
    }

    private fun initializeFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.w("Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Timber.d("FCM Token: $token")

            // Save token to SharedPreferences
            val sharedPreferences = getSharedPreferences("FCM_PREFS", MODE_PRIVATE)
            sharedPreferences.edit().putString("FCM_TOKEN", token).apply()

            // TODO: Send token to your backend server
            // You can implement this later when you have a backend endpoint
        })

        // Subscribe to a topic (optional)
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to all_users topic"
                if (!task.isSuccessful) {
                    msg = "Subscribe to all_users topic failed"
                }
                Timber.d(msg)
            }
    }

    fun logEvent(eventName: String, params: android.os.Bundle?) {
        firebaseAnalytics.logEvent(eventName, params)
        Timber.d("Firebase Analytics event logged: $eventName")
    }

    fun getFCMToken(): String? {
        val sharedPreferences = getSharedPreferences("FCM_PREFS", MODE_PRIVATE)
        return sharedPreferences.getString("FCM_TOKEN", null)
    }
}
