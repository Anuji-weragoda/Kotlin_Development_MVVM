package com.example.androidapp.data.repository

import com.example.androidapp.data.remote.WebSocketService
import okhttp3.WebSocketListener

class WebSocketRepositoryImpl(
    private val service: WebSocketService
) {

    fun connect(listener: WebSocketListener) {
        service.connect(listener)
    }

    fun sendMessage(msg: String) {
        service.send(msg)
    }

    fun close() {
        service.close()
    }
}
