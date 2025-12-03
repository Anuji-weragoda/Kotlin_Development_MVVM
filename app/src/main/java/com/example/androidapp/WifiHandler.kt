package com.example.androidapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.example.androidapp.data.local.WifiPreferences
import com.example.androidapp.data.model.SavedWifiNetwork
import com.example.androidapp.data.repository.WifiRepository
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

private const val REQUEST_WIFI_PERMISSIONS = 8765

class WifiHandler(
    private val context: Context,
    private val channel: MethodChannel
) : MethodChannel.MethodCallHandler {

    private val preferences = WifiPreferences(context)
    private val repository = WifiRepository(context, preferences)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        channel.setMethodCallHandler(this)
        Timber.d("WifiHandler registered on channel")
        observeConnectionStatus()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "isWifiEnabled" -> {
                result.success(repository.isWifiEnabled())
            }

            "enableWifi" -> {
                repository.enableWifi()
                result.success(null)
            }

            "disableWifi" -> {
                repository.disableWifi()
                result.success(null)
            }

            // Accept both 'hasWifiPermissions' (Dart constant) and legacy 'hasPermissions'
            "hasWifiPermissions", "hasPermissions" -> {
                result.success(repository.hasPermissions())
            }

            "requestPermissions" -> {
                // Try to request the appropriate permissions from the Activity if available.
                val activity = if (context is Activity) context else null
                if (activity == null) {
                    result.error("NO_ACTIVITY", "Cannot request permissions without Activity", null)
                } else {
                    val perms = mutableListOf<String>()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                    } else {
                        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    ActivityCompat.requestPermissions(activity, perms.toTypedArray(), REQUEST_WIFI_PERMISSIONS)
                    result.success(null)
                }
            }

            "scanNetworks" -> {
                scanNetworks(result)
            }

            "connectToNetwork" -> {
                val ssid = call.argument<String>("ssid")
                val password = call.argument<String>("password")

                if (ssid != null && password != null) {
                    connectToNetwork(ssid, password, result)
                } else {
                    result.error("INVALID_ARGUMENT", "SSID and password are required", null)
                }
            }

            "getCurrentNetwork" -> {
                val network = repository.getCurrentNetwork()
                val networkMap = network?.let {
                    mapOf(
                        "ssid" to it.ssid,
                        "bssid" to it.bssid,
                        "level" to it.level,
                        "isConnected" to it.isConnected,
                        "isSaved" to it.isSaved
                    )
                }
                result.success(networkMap)
            }

            "saveNetwork" -> {
                val ssid = call.argument<String>("ssid")
                val password = call.argument<String>("password")
                val autoConnect = call.argument<Boolean>("autoConnect") ?: true

                if (ssid != null && password != null) {
                    val network = SavedWifiNetwork(
                        ssid = ssid,
                        password = password,
                        autoConnect = autoConnect
                    )
                    repository.saveNetwork(network)
                    result.success(null)
                } else {
                    result.error("INVALID_ARGUMENT", "SSID and password are required", null)
                }
            }

            "getSavedNetworks" -> {
                val networks = repository.getSavedNetworks()
                val networksList = networks.map { network ->
                    mapOf(
                        "ssid" to network.ssid,
                        "autoConnect" to network.autoConnect,
                        "savedTimestamp" to network.savedTimestamp
                    )
                }
                result.success(networksList)
            }

            "removeNetwork" -> {
                val ssid = call.argument<String>("ssid")
                if (ssid != null) {
                    repository.removeNetwork(ssid)
                    result.success(null)
                } else {
                    result.error("INVALID_ARGUMENT", "SSID is required", null)
                }
            }

            "getSignalStrength" -> {
                val strength = repository.getSignalStrength() // no arguments
                result.success(strength)
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    private fun scanNetworks(result: MethodChannel.Result) {
        scope.launch {
            try {
                repository.scanNetworks().collectLatest { networks ->
                    val networksList = networks.map { network ->
                        mapOf(
                            "ssid" to network.ssid,
                            "bssid" to network.bssid,
                            "capabilities" to network.capabilities,
                            "level" to network.level,
                            "frequency" to network.frequency,
                            "isSecured" to network.isSecured,
                            "isSaved" to network.isSaved,
                            "isConnected" to network.isConnected
                        )
                    }
                    channel.invokeMethod("onNetworksFound", networksList)
                }
                result.success(null)
            } catch (e: Exception) {
                result.error("SCAN_ERROR", e.message, null)
            }
        }
    }

    private fun connectToNetwork(ssid: String, password: String, result: MethodChannel.Result) {
        scope.launch {
            try {
                val connectionResult = repository.connectToNetwork(ssid, password)
                connectionResult.onSuccess { status ->
                    val statusMap = mapOf(
                        "state" to status.state.name,
                        "network" to status.network?.let {
                            mapOf(
                                "ssid" to it.ssid,
                                "isConnected" to it.isConnected
                            )
                        },
                        "ipAddress" to status.ipAddress,
                        "message" to status.message
                    )
                    result.success(statusMap)
                }
                connectionResult.onFailure { error ->
                    result.error("CONNECTION_ERROR", error.message, null)
                }
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
                    "network" to status.network?.let {
                        mapOf(
                            "ssid" to it.ssid,
                            "isConnected" to it.isConnected
                        )
                    },
                    "ipAddress" to status.ipAddress,
                    "message" to status.message
                )
                channel.invokeMethod("onConnectionStatusChanged", statusMap)
            }
        }
    }

    // Called by Activity/ChannelManager when permission results arrive
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != REQUEST_WIFI_PERMISSIONS) return

        try {
            val hasPerms = repository.hasPermissions()
            channel.invokeMethod("onPermissionResult", mapOf("hasPermissions" to hasPerms))
        } catch (t: Throwable) {
            Timber.w(t, "WifiHandler: onRequestPermissionsResult failed")
        }
    }

    fun dispose() {
        scope.cancel()
    }
}
