package com.example.androidapp

import android.app.Application
import timber.log.Timber
import com.example.androidapp.data.local.TokenManager
import com.example.androidapp.data.remote.RetrofitClient
import com.example.androidapp.data.repository.AuthRepository

class AuthApplication : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()

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
    }
}
