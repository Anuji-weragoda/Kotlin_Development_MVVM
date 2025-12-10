package com.example.androidapp.data.remote.ssl

import android.util.Log
import com.example.androidapp.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object SslConfig {

    private const val TAG = "SslConfig"


    private const val ENABLE_SSL_PINNING = true

    fun createSecureOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        builder.addInterceptor(loggingInterceptor)


        if (ENABLE_SSL_PINNING) {
            Log.d(TAG, "SSL Pinning ENABLED")

            val certificatePinner = CertificatePinner.Builder()
                // TESTING MODE - Uncomment to test SSL pinning failure
                // .add("restcountries.com", "sha256/WRONGPINAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") // Wrong pin - app should FAIL

                // Correct pins for restcountries.com
                .add("restcountries.com", "sha256/mQmO4iuGN8Fhs4hpB6USJDkwWiPqXwh+CWXHIPT+GQo=") // Leaf certificate
                .add("restcountries.com", "sha256/iFvwVyJSxnQdyaUvUERIf+8qk7gRze3612JMwoO3zdU=") // Intermediate CA
                .build()

            builder.certificatePinner(certificatePinner)
        } else {
            Log.w(TAG, " SSL Pinning DISABLED - App is vulnerable to MITM attacks (HTTP Toolkit testing mode)")
        }

        return builder.build()
    }
}

