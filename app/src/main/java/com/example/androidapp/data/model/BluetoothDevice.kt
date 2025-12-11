package com.example.androidapp.data.model

data class BluetoothDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val isConnected: Boolean = false,
    val bondState: Int = 0,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val services: List<String> = emptyList()
)

enum class DeviceType {
    CLASSIC,
    BLE,
    DUAL,
    UNKNOWN
}

data class ConnectionStatus(
    val state: ConnectionState,
    val device: BluetoothDevice?,
    val message: String? = null
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}