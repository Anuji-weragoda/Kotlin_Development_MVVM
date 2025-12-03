package com.example.androidapp.data.repository

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.androidapp.data.local.WifiPreferences
import com.example.androidapp.data.model.SavedWifiNetwork
import com.example.androidapp.data.model.WifiConnectionStatus
import com.example.androidapp.data.model.WifiNetwork
import com.example.androidapp.data.model.WifiState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WifiRepository(
    private val context: Context,
    private val preferences: WifiPreferences
) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connectionStatus =
        MutableStateFlow(WifiConnectionStatus(WifiState.DISCONNECTED, null))
    val connectionStatus: StateFlow<WifiConnectionStatus> = _connectionStatus.asStateFlow()

    // Added tag for diagnostics
    private val TAG = "WifiRepository"


    fun hasPermissions(): Boolean {
        // On Android 13+ prefer the NEARBY_WIFI_DEVICES permission. For older OSes a location
        // permission (fine or coarse) is required for Wi‑Fi scanning results.
        val nearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else false

        val fine = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // If NEARBY_WIFI_DEVICES is present (Android 13+) accept it; otherwise accept location perms.
        return nearby || fine || coarse
    }

    private fun isLocationEnabled(): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                // for older devices, fall back to checking providers
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "isLocationEnabled: error checking location state: ${e.message}")
            true
        }
    }


    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    @Suppress("DEPRECATION")
    fun enableWifi() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            wifiManager.isWifiEnabled = true
        }
    }

    @Suppress("DEPRECATION")
    fun disableWifi() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            wifiManager.isWifiEnabled = false
        }
    }


    fun scanNetworks(): Flow<List<WifiNetwork>> = callbackFlow {
        Log.d(TAG, "scanNetworks() called. SDK=${Build.VERSION.SDK_INT}, hasPermissions=${hasPermissions()}")
        if (!hasPermissions()) {
            // Don't close immediately here — the caller/handler should request permissions and
            // scanning can either retry or fall back to saved networks. Log and continue so
            // the platform channel can deliver an empty result or saved networks as appropriate.
            Log.w(TAG, "Missing required runtime permissions for scanning Wi‑Fi — results may be empty until permissions are granted")
        }

        // On many Android versions, location services must be enabled for Wi‑Fi scanning to return results.
        val locationOn = isLocationEnabled()
        Log.d(TAG, "location services enabled=$locationOn")

        if (!locationOn && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // On Android < 12, location being off commonly prevents scan results.
            Log.w(TAG, "Location services are disabled; scan results may be empty")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android Q+ active scanning is restricted but still possible for foreground apps
            // with the proper permissions. Attempt an active scan when permissions exist; if not,
            // fall back to returning saved networks only.
            if (!hasPermissions()) {
                val savedNetworks = getSavedNetworks().map {
                    WifiNetwork(
                        ssid = it.ssid,
                        bssid = "",
                        capabilities = "",
                        level = 0,
                        frequency = 0,
                        isSecured = true,
                        isSaved = true
                    )
                }
                trySend(savedNetworks)
                close()
            } else {
                // Use the same broadcast-receiver based scanning approach as legacy devices.
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        try {
                            @Suppress("DEPRECATION")
                            val results = wifiManager.scanResults
                            Log.d(TAG, "onReceive: scan results count=${results.size}")
                            val networks = results.map { scanResult ->
                                WifiNetwork(
                                    ssid = scanResult.SSID,
                                    bssid = scanResult.BSSID,
                                    capabilities = scanResult.capabilities,
                                    level = scanResult.level,
                                    frequency = scanResult.frequency,
                                    isSecured = !scanResult.capabilities.contains("OPEN"),
                                    isSaved = preferences.isNetworkSaved(scanResult.SSID)
                                )
                            }
                            trySend(networks)
                        } catch (e: SecurityException) {
                            Log.w(TAG, "onReceive: SecurityException while accessing scanResults: ${e.message}")
                            trySend(emptyList())
                        } catch (e: Exception) {
                            Log.e(TAG, "onReceive: unexpected error while processing scan results", e)
                            trySend(emptyList())
                        }
                    }
                }

                val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                context.registerReceiver(receiver, intentFilter)

                try {
                    @Suppress("DEPRECATION")
                    val started = wifiManager.startScan()
                    Log.d(TAG, "startScan() called, returned=$started")
                } catch (e: SecurityException) {
                    Log.w(TAG, "startScan() failed with SecurityException: ${e.message}")
                    trySend(emptyList())
                } catch (e: Exception) {
                    Log.e(TAG, "startScan() threw unexpected exception", e)
                    trySend(emptyList())
                }

                awaitClose { try { context.unregisterReceiver(receiver) } catch (_: Exception) {} }
            }
        } else {
            // Legacy scanning for Android 9 and below
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        @Suppress("DEPRECATION")
                        val results = wifiManager.scanResults
                        Log.d(TAG, "onReceive (legacy): scan results count=${results.size}")
                        val networks = results.map { scanResult ->
                            WifiNetwork(
                                ssid = scanResult.SSID,
                                bssid = scanResult.BSSID,
                                capabilities = scanResult.capabilities,
                                level = scanResult.level,
                                frequency = scanResult.frequency,
                                isSecured = !scanResult.capabilities.contains("OPEN"),
                                isSaved = preferences.isNetworkSaved(scanResult.SSID)
                            )
                        }
                        trySend(networks)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "onReceive (legacy): SecurityException: ${e.message}")
                        trySend(emptyList())
                    } catch (e: Exception) {
                        Log.e(TAG, "onReceive (legacy): unexpected error", e)
                        trySend(emptyList())
                    }
                }
            }

            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(receiver, intentFilter)

            try {
                @Suppress("DEPRECATION")
                val started = wifiManager.startScan()
                Log.d(TAG, "startScan() (legacy) called, returned=$started")
            } catch (e: SecurityException) {
                Log.w(TAG, "startScan() (legacy) SecurityException: ${e.message}")
                trySend(emptyList())
            } catch (e: Exception) {
                Log.e(TAG, "startScan() (legacy) unexpected exception", e)
                trySend(emptyList())
            }

            awaitClose { context.unregisterReceiver(receiver) }
        }
    }


    suspend fun connectToNetwork(ssid: String, password: String): Result<WifiConnectionStatus> =
        suspendCancellableCoroutine { continuation ->

            if (!hasPermissions()) {
                _connectionStatus.value =
                    WifiConnectionStatus(WifiState.ERROR, null, message = "Permission denied")
                continuation.resume(Result.failure(SecurityException("Missing permission")))
                return@suspendCancellableCoroutine
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val specifier = WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build()

                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifier)
                    .build()

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _connectionStatus.value = WifiConnectionStatus(
                            WifiState.CONNECTED,
                            WifiNetwork(ssid, "", "", 0, 0, isConnected = true)
                        )
                        continuation.resume(Result.success(_connectionStatus.value))
                    }

                    override fun onUnavailable() {
                        _connectionStatus.value = WifiConnectionStatus(
                            WifiState.ERROR, null, message = "Connection unavailable"
                        )
                        continuation.resume(Result.failure(Exception("Connection unavailable")))
                    }

                    override fun onLost(network: Network) {
                        _connectionStatus.value =
                            WifiConnectionStatus(WifiState.DISCONNECTED, null)
                    }
                }

                try {
                    _connectionStatus.value = WifiConnectionStatus(WifiState.CONNECTING, null)
                    connectivityManager.requestNetwork(request, callback)
                } catch (e: SecurityException) {
                    _connectionStatus.value = WifiConnectionStatus(
                        WifiState.ERROR, null, message = "Permission denied"
                    )
                    continuation.resume(Result.failure(e))
                }

                continuation.invokeOnCancellation {
                    try {
                        connectivityManager.unregisterNetworkCallback(callback)
                    } catch (_: Exception) {}
                }

            } else {
                // Android 9 and below
                val configuredNetwork = try {
                    @Suppress("DEPRECATION")
                    wifiManager.configuredNetworks.firstOrNull { it.SSID == "\"$ssid\"" }
                } catch (e: SecurityException) {
                    null
                }

                if (configuredNetwork != null) {
                    try {
                        @Suppress("DEPRECATION")
                        wifiManager.enableNetwork(configuredNetwork.networkId, true)
                        _connectionStatus.value = WifiConnectionStatus(
                            WifiState.CONNECTED,
                            WifiNetwork(ssid, "", "", 0, 0, isConnected = true)
                        )
                        continuation.resume(Result.success(_connectionStatus.value))
                    } catch (e: SecurityException) {
                        _connectionStatus.value =
                            WifiConnectionStatus(WifiState.ERROR, null, message = "Permission denied")
                        continuation.resume(Result.failure(e))
                    }
                } else {
                    _connectionStatus.value =
                        WifiConnectionStatus(WifiState.ERROR, null, message = "Network not configured")
                    continuation.resume(Result.failure(Exception("Network not configured")))
                }
            }
        }


    fun getCurrentNetwork(): WifiNetwork? {
        if (!hasPermissions()) return null

        return try {
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo.ssid
            if (ssid != "<unknown ssid>" && ssid != "0x") {
                WifiNetwork(
                    ssid = ssid.removeSurrounding("\""),
                    bssid = wifiInfo.bssid,
                    capabilities = "",
                    level = wifiInfo.rssi,
                    frequency = wifiInfo.frequency,
                    isConnected = true,
                    isSaved = preferences.isNetworkSaved(ssid.removeSurrounding("\""))
                )
            } else null
        } catch (e: SecurityException) {
            null
        }
    }


    fun getSignalStrength(): Int {
        if (!hasPermissions()) return 0

        return try {
            @Suppress("DEPRECATION")
            val rssi = wifiManager.connectionInfo.rssi
            when {
                rssi <= -100 -> 0
                rssi >= -50 -> 5
                else -> ((rssi + 100) * 5 / 50)
            }
        } catch (e: SecurityException) {
            0
        }
    }


    fun saveNetwork(network: SavedWifiNetwork) = preferences.saveNetwork(network)
    fun getSavedNetworks(): List<SavedWifiNetwork> = preferences.getSavedNetworks()
    fun removeNetwork(ssid: String) = preferences.removeNetwork(ssid)
}
