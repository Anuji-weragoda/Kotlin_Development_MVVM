package com.example.androidapp.data.remote.interceptor

import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

object LoggingInterceptorConfig {


    fun create(isDebug: Boolean = true): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            // Use Timber for logging (better than Log.d)
            Timber.tag("OkHttp").d(message)
        }.apply {
            // Set level based on build type
            level = if (isDebug) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
}

