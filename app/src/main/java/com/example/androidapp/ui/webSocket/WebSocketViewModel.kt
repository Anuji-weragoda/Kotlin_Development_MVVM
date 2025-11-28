package com.example.androidapp.ui.webSocket

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import com.example.androidapp.data.repository.WebSocketRepositoryImpl

class WebSocketViewModel(
    private val repository: WebSocketRepositoryImpl
) : ViewModel() {

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> get() = _connectionState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.postValue(ConnectionState.CONNECTED)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            _message.postValue(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.postValue(ConnectionState.DISCONNECTED)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.postValue(ConnectionState.DISCONNECTED)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionState.postValue(ConnectionState.ERROR)
            _error.postValue(t.message ?: "Connection failed")
        }
    }

    fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        repository.connect(listener)
    }

    fun disconnect() {
        repository.close()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun send(msg: String) {
        repository.sendMessage(msg)
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}

enum class ConnectionState {
    CONNECTING, CONNECTED, DISCONNECTED, ERROR
}