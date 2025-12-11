package com.example.androidapp

import android.app.Application
import android.os.StrictMode
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

        // Enable StrictMode in debug builds to detect blocking operations
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        // Initialize Timber first (lightweight)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize critical components synchronously
        tokenManager = TokenManager(applicationContext)
        RetrofitClient.initialize(tokenManager)
        authRepository = AuthRepository(tokenManager)

        // Initialize Firebase synchronously (required for app startup)
        FirebaseApp.initializeApp(this)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)

        Timber.d("AuthApplication core initialized")

        // Initialize non-critical Firebase features in background
        Thread {
            try {
                initializeFCM()
                FCMHelper.createNotificationChannels(this)

                // Log app started event
                logEvent("app_started", null)

                Timber.d("Firebase Analytics and FCM initialized")
            } catch (e: Exception) {
                Timber.e(e, "Error initializing Firebase features")
            }
        }.start()
    }

    private fun initializeFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.w(task.exception, "Fetching FCM registration token failed")
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Timber.d("FCM Token: $token")

            // Save token to SharedPreferences
            val sharedPreferences = getSharedPreferences("FCM_PREFS", MODE_PRIVATE)
            sharedPreferences.edit().putString("FCM_TOKEN", token).apply()

            // TODO: Send token to your backend server

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
