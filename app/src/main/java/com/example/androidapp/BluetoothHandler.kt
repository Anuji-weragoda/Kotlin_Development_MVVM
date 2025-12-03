package com.example.androidapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.example.androidapp.data.local.BluetoothPreferences
import com.example.androidapp.data.repository.BluetoothRepository
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.*

private const val REQUEST_BT_PERMISSIONS = 9876

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

            "requestPermissions" -> {
                // Try to request the appropriate permissions from the Activity if available.
                val activity = if (context is Activity) context else null
                if (activity == null) {
                    result.error("NO_ACTIVITY", "Cannot request permissions without Activity", null)
                } else {
                    val perms = mutableListOf<String>()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        perms.add(Manifest.permission.BLUETOOTH_SCAN)
                        perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        perms.add(Manifest.permission.BLUETOOTH)
                        perms.add(Manifest.permission.BLUETOOTH_ADMIN)
                        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    ActivityCompat.requestPermissions(activity, perms.toTypedArray(), REQUEST_BT_PERMISSIONS)
                    result.success(null)
                }
            }

            "startScan" -> {
                // MethodChannel may marshal numbers as Int or Long; read as Number and convert
                val durationNumber = call.argument<Number?>("duration")
                val duration = durationNumber?.toLong() ?: 10000L

                // QUICK DEBUG LOG: confirm call reached Android and duration
                android.util.Log.d("BluetoothHandler", "startScan invoked from Flutter: duration=$duration")

                // Quick pre-check so Flutter gets immediate feedback if permissions/Bluetooth are missing
                val missing = repository.getMissingPermissions()
                if (missing.isNotEmpty()) {
                    android.util.Log.w("BluetoothHandler", "startScan: missing permissions before starting scan: $missing")
                    result.error("MISSING_PERMISSIONS", "Missing permissions: $missing", null)
                    return
                }

                if (!repository.isBluetoothEnabled()) {
                    android.util.Log.w("BluetoothHandler", "startScan: Bluetooth is disabled")
                    result.error("BLUETOOTH_DISABLED", "Bluetooth is disabled", null)
                    return
                }

                // Start scanning in background and immediately acknowledge the method call
                startScanning(duration, result)
            }

            "stopScan" -> {
                repository.stopScan()
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

    // Called by Activity/ChannelManager when permission results arrive
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != REQUEST_BT_PERMISSIONS) return

        // Notify Flutter of updated permission state so it can retry scans if needed
        try {
            val hasPerms = repository.hasPermissions()
            channel.invokeMethod("onPermissionResult", mapOf("hasPermissions" to hasPerms))
        } catch (t: Throwable) {
            android.util.Log.w("BluetoothHandler", "onRequestPermissionsResult failed: ${t.message}")
        }
    }

    private fun startScanning(duration: Long, result: MethodChannel.Result) {
        // Launch collection in the background. We must *immediately* reply to the
        // MethodChannel call (result.success) so Flutter's invokeMethod completes and
        // doesn't hang/timeout. The scanning flow will still emit device lists via
        // the channel.invokeMethod("onDevicesFound", ...) from the coroutine below.
        scope.launch {
            try {
                repository.startScan(duration).collectLatest { devices ->
                    val devicesList = devices.map { device ->
                        mapOf(
                            "name" to device.name,
                            "address" to device.address,
                            "rssi" to device.rssi,
                            "isConnected" to device.isConnected,
                            "deviceType" to (device.deviceType?.name ?: "UNKNOWN"),
                            "services" to (device.services ?: emptyList<String>())
                        )
                    }

                    // Log for debugging
                    android.util.Log.d("BluetoothHandler", "Invoking onDevicesFound with ${devicesList.size} devices")

                    // Dump the full devicesList content for debugging (shows the exact map/list sent to Flutter)
                    try {
                        android.util.Log.d("BluetoothHandler", "devicesList payload: ${devicesList}")
                    } catch (logEx: Exception) {
                        android.util.Log.e("BluetoothHandler", "Failed to stringify devicesList: ${logEx.message}")
                    }

                    try {
                        channel.invokeMethod("onDevicesFound", devicesList)
                    } catch (invokeEx: Exception) {
                        android.util.Log.e("BluetoothHandler", "invokeMethod failed: ${invokeEx.message}")
                    }
                }
            } catch (e: Exception) {
                // If collection fails we can't use result (it was already acknowledged)
                android.util.Log.e("BluetoothHandler", "startScanning failed in coroutine: ${e.message}")
            }
        }

        // Immediately acknowledge the platform method call so Flutter doesn't block.
        result.success(null)
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