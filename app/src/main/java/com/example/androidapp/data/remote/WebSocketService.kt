package com.example.androidapp.data.remote

import okhttp3.*

class WebSocketService(
    private val url: String = "wss://ws.postman-echo.com/raw"

) {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect(listener: WebSocketListener) {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, listener)
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    fun close() {
        webSocket?.close(1000, "Closing")
    }
}
