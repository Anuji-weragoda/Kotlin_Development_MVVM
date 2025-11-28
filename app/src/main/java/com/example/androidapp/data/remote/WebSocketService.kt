package com.example.androidapp.data.remote

import okhttp3.*

class WebSocketService(
    private val url: String
) {

    private var webSocket: WebSocket? = null

    fun connect(listener: WebSocketListener) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, listener)
    }

    fun send(message: String) {
        webSocket?.send(message)
    }

    fun close() {
        webSocket?.close(1000, "Closing")
    }
}
