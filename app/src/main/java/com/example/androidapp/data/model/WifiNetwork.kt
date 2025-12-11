package com.example.androidapp.data.model

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val capabilities: String,
    val level: Int,
    val frequency: Int,
    val isSecured: Boolean = true,
    val isSaved: Boolean = false,
    val isConnected: Boolean = false
)

data class SavedWifiNetwork(
    val ssid: String,
    val password: String,
    val priority: Int = 0,
    val autoConnect: Boolean = true,
    val savedTimestamp: Long = System.currentTimeMillis()
)

data class WifiConnectionStatus(
    val state: WifiState,
    val network: WifiNetwork?,
    val ipAddress: String? = null,
    val message: String? = null
)

enum class WifiState {
    DISABLED,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    SUSPENDED,
    DISCONNECTING,
    ERROR
}