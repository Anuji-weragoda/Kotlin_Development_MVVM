package com.example.androidapp.data.repository

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
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


    fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.NEARBY_WIFI_DEVICES
                        ) == PackageManager.PERMISSION_GRANTED)
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
        if (!hasPermissions()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ does not allow active scanning, show saved networks only
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
            // Legacy scanning for Android 9 and below
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        @Suppress("DEPRECATION")
                        val results = wifiManager.scanResults
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
                        trySend(emptyList())
                    }
                }
            }

            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(receiver, intentFilter)

            try {
                @Suppress("DEPRECATION")
                wifiManager.startScan()
            } catch (e: SecurityException) {
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
