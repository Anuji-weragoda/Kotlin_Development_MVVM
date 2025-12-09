package com.example.androidapp

import android.app.Application
import timber.log.Timber
import com.example.androidapp.data.local.TokenManager
import com.example.androidapp.data.remote.RetrofitClient
import com.example.androidapp.data.repository.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

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

    fun logEvent(eventName: String, params: android.os.Bundle?) {
        firebaseAnalytics.logEvent(eventName, params)
        Timber.d("Firebase Analytics event logged: $eventName")
    }
}
