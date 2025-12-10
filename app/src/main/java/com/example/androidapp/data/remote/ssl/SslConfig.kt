package com.example.androidapp.data.remote.ssl

import android.util.Log
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object SslConfig {

    private const val TAG = "SslConfig"


    fun createSecureOkHttpClient(): OkHttpClient {
        Log.d(TAG, "Creating OkHttpClient with Certificate Pinning for restcountries.com")

        val certificatePinner = CertificatePinner.Builder()
            // TESTING MODE
             //.add("restcountries.com", "sha256/WRONGPINAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") // Wrong pin - app should FAIL

            //  Correct pins for restcountries.com
           .add("restcountries.com", "sha256/mQmO4iuGN8Fhs4hpB6USJDkwWiPqXwh+CWXHIPT+GQo=") // Leaf certificate
           .add("restcountries.com", "sha256/iFvwVyJSxnQdyaUvUERIf+8qk7gRze3612JMwoO3zdU=") // Intermediate CA
            .build()

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor(loggingInterceptor)
            .build()
    }
}

