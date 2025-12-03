package com.example.androidapp

import android.Manifest
import android.app.Activity
import android.content.Context
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

    // Whether Dart client is ready to receive callbacks
    @Volatile
    private var dartClientReady: Boolean = false

    // Buffer to hold last networks payload until Dart is ready
    private val pendingNetworks = mutableListOf<List<Map<String, Any>>>()

    // Diagnostics - track last scan/start status so Dart can query it
    @Volatile
    private var lastScanStarted: Boolean = false
    @Volatile
    private var lastStartScanReturned: Boolean? = null
    @Volatile
    private var lastCachedResultsCount: Int = -1
    @Volatile
    private var lastOnReceiveResultsCount: Int = -1

    init {
        channel.setMethodCallHandler(this)
        Timber.d("WifiHandler registered on channel")
        observeConnectionStatus()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        // Handle clientReady handshake from Dart
        if (call.method == "clientReady") {
            Timber.d("WifiHandler: clientReady received from Dart: args=${call.arguments}")
            dartClientReady = true
            // Flush any pending networks
            try {
                pendingNetworks.forEach { networksList ->
                    try {
                        channel.invokeMethod("onNetworksFound", networksList)
                    } catch (invokeEx: Exception) {
                        Timber.e(invokeEx, "WifiHandler: flush invokeMethod failed: ${invokeEx.message}")
                    }
                }
            } catch (t: Throwable) {
                Timber.w(t, "WifiHandler: error flushing pending networks")
            } finally {
                pendingNetworks.clear()
            }
            result.success(null)
            return
        }

        // Diagnostic: allow Dart to query internal handler state
        if (call.method == "diagnosticGetState") {
            try {
                val state = mapOf(
                    "dartClientReady" to dartClientReady,
                    "lastScanStarted" to lastScanStarted,
                    "lastStartScanReturned" to lastStartScanReturned,
                    "lastCachedResultsCount" to lastCachedResultsCount,
                    "lastOnReceiveResultsCount" to lastOnReceiveResultsCount
                )
                result.success(state)
            } catch (t: Throwable) {
                Timber.w(t, "WifiHandler: diagnosticGetState failed")
                result.success(null)
            }
            return
        }

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
                    }
                    // Always add location permissions - required for Wi-Fi scanning on all Android versions
                    perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)

                    Timber.d("Requesting Wi-Fi permissions: $perms")
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
                Timber.d("scanNetworks() started - requesting Wi-Fi network scan")
                // Mark diagnostics: a scan is starting
                lastScanStarted = true
                withTimeoutOrNull(10000L) {
                    repository.scanNetworks().collectLatest { networks ->
                        Timber.d("scanNetworks() received ${'$'}{networks.size} networks")
                        // Update diagnostics when receiver yields results
                        lastOnReceiveResultsCount = networks.size
                        val networksList = networks.map { network ->
                            Timber.d("  - Found network: ${'$'}{network.ssid} (level=${'$'}{network.level}, secured=${'$'}{network.isSecured})")
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
                        sendNetworksToFlutter(networksList)
                    }
                } ?: run {
                    Timber.w("scanNetworks() timed out after 10 seconds")
                    channel.invokeMethod("onNetworksFound", emptyList<Map<String, Any>>())
                }
                result.success(null)
            } catch (e: Exception) {
                Timber.e(e, "scanNetworks() error: ${'$'}{e.message}")
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

    private fun sendNetworksToFlutter(networksList: List<Map<String, Any>>) {
        if (dartClientReady) {
            try {
                channel.invokeMethod("onNetworksFound", networksList)
            } catch (invokeEx: Exception) {
                Timber.e(invokeEx, "WifiHandler: invokeMethod failed when sending networks: ${'$'}{invokeEx.message}")
            }
        } else {
            Timber.d("WifiHandler: Dart client not ready - buffering ${'$'}{networksList.size} networks")
            pendingNetworks.clear()
            pendingNetworks.add(networksList)
        }
    }
}
