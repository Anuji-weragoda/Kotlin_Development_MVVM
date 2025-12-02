package com.example.androidapp

import android.content.Context
import com.example.androidapp.data.local.BluetoothPreferences
import com.example.androidapp.data.repository.BluetoothRepository
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.*

class BluetoothHandler(
    private val context: Context,
    private val channel: MethodChannel
) : MethodChannel.MethodCallHandler {

    private val preferences = BluetoothPreferences(context)
    private val repository = BluetoothRepository(context, preferences)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        channel.setMethodCallHandler(this)
        observeConnectionStatus()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "isBluetoothEnabled" -> {
                result.success(repository.isBluetoothEnabled())
            }

            "enableBluetooth" -> {
                repository.enableBluetooth()
                result.success(null)
            }

            "disableBluetooth" -> {
                repository.disableBluetooth()
                result.success(null)
            }

            "hasPermissions" -> {
                result.success(repository.hasPermissions())
            }

            "startScan" -> {
                // MethodChannel may marshal numbers as Int or Long; read as Number and convert
                val durationNumber = call.argument<Number?>("duration")
                val duration = durationNumber?.toLong() ?: 10000L
                startScanning(duration, result)
            }

            "stopScan" -> {

                result.success(null)
            }

            "connectToDevice" -> {
                val address = call.argument<String>("address")
                if (address != null) {
                    connectToDevice(address, result)
                } else {
                    result.error("INVALID_ARGUMENT", "Device address is required", null)
                }
            }

            "disconnect" -> {
                repository.disconnect()
                result.success(null)
            }

            "writeData" -> {
                val serviceUuid = call.argument<String>("serviceUuid")
                val characteristicUuid = call.argument<String>("characteristicUuid")
                val data = call.argument<ByteArray>("data")

                if (serviceUuid != null && characteristicUuid != null && data != null) {
                    val success = repository.writeData(
                        UUID.fromString(serviceUuid),
                        UUID.fromString(characteristicUuid),
                        data
                    )
                    result.success(success)
                } else {
                    result.error("INVALID_ARGUMENT", "Missing required arguments", null)
                }
            }

            "readData" -> {
                val serviceUuid = call.argument<String>("serviceUuid")
                val characteristicUuid = call.argument<String>("characteristicUuid")

                if (serviceUuid != null && characteristicUuid != null) {
                    val success = repository.readData(
                        UUID.fromString(serviceUuid),
                        UUID.fromString(characteristicUuid)
                    )
                    result.success(success)
                } else {
                    result.error("INVALID_ARGUMENT", "Missing required arguments", null)
                }
            }

            "getPairedDevices" -> {
                val devices = repository.getPairedDevices()
                val devicesList = devices.map { device ->
                    mapOf(
                        "name" to device.name,
                        "address" to device.address,
                        "rssi" to device.rssi,
                        "isConnected" to device.isConnected
                    )
                }
                result.success(devicesList)
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    private fun startScanning(duration: Long, result: MethodChannel.Result) {
        scope.launch {
            try {
                repository.startScan(duration).collectLatest { devices ->
                    val devicesList = devices.map { device ->
                        mapOf(
                            "name" to device.name,
                            "address" to device.address,
                            "rssi" to device.rssi,
                            "isConnected" to device.isConnected
                        )
                    }

                    channel.invokeMethod("onDevicesFound", devicesList)
                }
                result.success(null)
            } catch (e: Exception) {
                result.error("SCAN_ERROR", e.message, null)
            }
        }
    }

    private fun connectToDevice(address: String, result: MethodChannel.Result) {
        scope.launch {
            try {
                repository.connectToDevice(address).collectLatest { status ->
                    val statusMap = mapOf(
                        "state" to status.state.name,
                        "device" to status.device?.let {
                            mapOf(
                                "name" to it.name,
                                "address" to it.address,
                                "isConnected" to it.isConnected
                            )
                        },
                        "message" to status.message
                    )

                    channel.invokeMethod("onConnectionStatusChanged", statusMap)
                }
                result.success(null)
            } catch (e: Exception) {
                result.error("CONNECTION_ERROR", e.message, null)
            }
        }
    }

    private fun observeConnectionStatus() {
        scope.launch {
            repository.connectionStatus.collectLatest { status ->
                val statusMap = mapOf(
                    "state" to status.state.name,
                    "device" to status.device?.let {
                        mapOf(
                            "name" to it.name,
                            "address" to it.address,
                            "isConnected" to it.isConnected
                        )
                    },
                    "message" to status.message
                )

                channel.invokeMethod("onConnectionStatusChanged", statusMap)
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }
}