package com.example.androidapp.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class NetworkInterceptor : okhttp3.Interceptor {

    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        // Add custom headers
        val modifiedRequest = originalRequest.newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Platform", "Android")
            .addHeader("App-Version", "1.0.0") // From BuildConfig
            .build()

        return chain.proceed(modifiedRequest)
    }
}