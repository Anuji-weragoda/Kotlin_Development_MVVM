package com.example.androidapp.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.androidapp.data.local.BluetoothPreferences
import com.example.androidapp.data.model.BluetoothDevice
import com.example.androidapp.data.model.ConnectionState
import com.example.androidapp.data.model.ConnectionStatus
import com.example.androidapp.data.model.DeviceType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*

class BluetoothRepository(
    private val context: Context,
    private val preferences: BluetoothPreferences
) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private val _connectionStatus = MutableStateFlow(
        ConnectionStatus(ConnectionState.DISCONNECTED, null)
    )
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    @Volatile
    private var activeScanCallback: ScanCallback? = null
    @Volatile
    private var activeScannerRef: BluetoothLeScanner? = null

    // --------------------- Permissions ---------------------
    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                missing.add(Manifest.permission.BLUETOOTH_SCAN)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                missing.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                missing.add(Manifest.permission.BLUETOOTH)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
                missing.add(Manifest.permission.BLUETOOTH_ADMIN)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return missing
    }

    // --------------------- Bluetooth Enable/Disable ---------------------
    @SuppressLint("MissingPermission")
    fun enableBluetooth() {
        bluetoothAdapter?.enable()
    }

    @SuppressLint("MissingPermission")
    fun disableBluetooth() {
        bluetoothAdapter?.disable()
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun isScannerAvailable(): Boolean = bluetoothAdapter?.bluetoothLeScanner != null

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // --------------------- BLE Scanning ---------------------
    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            activeScannerRef?.stopScan(activeScanCallback)
        } catch (t: Throwable) {
            android.util.Log.w("BluetoothRepository", "stopScan threw: ${t.message}")
        } finally {
            activeScanCallback = null
            activeScannerRef = null
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 10000): Flow<List<BluetoothDevice>> = callbackFlow {
        android.util.Log.d("BluetoothRepo", "startScan called with duration=${durationMs}ms")

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            android.util.Log.e("BluetoothRepo", "Scanner is null - adapter exists: ${bluetoothAdapter != null}")
            close(IllegalStateException("BLE scanner not available"))
            return@callbackFlow
        }

        val missing = getMissingPermissions()
        if (missing.isNotEmpty()) {
            android.util.Log.e("BluetoothRepo", "Missing permissions: $missing")
            close(IllegalStateException("Missing permissions: $missing"))
            return@callbackFlow
        }

        if (!isBluetoothEnabled()) {
            android.util.Log.e("BluetoothRepo", "Bluetooth is disabled")
            close(IllegalStateException("Bluetooth is disabled"))
            return@callbackFlow
        }

        // CRITICAL: Check if location is enabled (required for BLE scanning)
        if (!isLocationEnabled()) {
            android.util.Log.e("BluetoothRepo", "Location services are disabled - BLE scanning requires location to be enabled")
            close(IllegalStateException("Location services must be enabled for BLE scanning"))
            return@callbackFlow
        }

        android.util.Log.d("BluetoothRepo", "All pre-checks passed. Starting BLE scan...")

        // Stop previous scan if active
        activeScannerRef?.stopScan(activeScanCallback)
        activeScannerRef = scanner

        val devices = mutableMapOf<String, BluetoothDevice>()

        // Emit initial empty list to signal scan started
        trySend(emptyList()).isSuccess
        android.util.Log.d("BluetoothRepo", "Emitted initial empty device list")

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                android.util.Log.d("BluetoothRepo", "Device found: name=${result?.device?.name ?: "Unknown"}, address=${result?.device?.address}, rssi=${result?.rssi}")
                result?.let {
                    val btDevice = BluetoothDevice(
                        name = it.device.name ?: "Unknown",
                        address = it.device.address,
                        rssi = it.rssi,
                        deviceType = DeviceType.BLE,
                        services = it.scanRecord?.serviceUuids?.mapNotNull { pu -> pu?.uuid?.toString() } ?: emptyList()
                    )
                    devices[btDevice.address] = btDevice
                    _discoveredDevices.value = devices.values.toList()
                    trySend(devices.values.toList()).isSuccess
                }
            }

            override fun onScanFailed(errorCode: Int) {
                val errorMsg = when (errorCode) {
                    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                    ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                    else -> "Unknown error code: $errorCode"
                }
                android.util.Log.e("BluetoothRepo", "Scan failed: $errorMsg")
                close(Exception("Scan failed with error: $errorCode ($errorMsg)"))
            }
        }

        activeScanCallback = scanCallback

        android.util.Log.d("BluetoothRepo", "Starting BLE scan with LOW_LATENCY mode...")

        try {
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
            android.util.Log.d("BluetoothRepo", "BLE scan started successfully")
        } catch (se: SecurityException) {
            android.util.Log.e("BluetoothRepo", "Security exception starting scan: ${se.message}")
            close(se)
            return@callbackFlow
        } catch (e: Exception) {
            android.util.Log.e("BluetoothRepo", "Exception starting scan: ${e.message}")
            close(e)
            return@callbackFlow
        }

        kotlinx.coroutines.delay(durationMs)
        android.util.Log.d("BluetoothRepo", "Scan duration ${durationMs}ms completed. Found ${devices.size} BLE devices")

        try {
            scanner.stopScan(scanCallback)
            android.util.Log.d("BluetoothRepo", "Scan stopped")
        } catch (_: Throwable) {}

        // Include system paired devices
        val pairedDevices = getSystemPairedDevices()
        android.util.Log.d("BluetoothRepo", "Found ${pairedDevices.size} paired devices")

        val allDevices = devices.values.toMutableList()
        allDevices.addAll(pairedDevices.filter { !devices.containsKey(it.address) })
        _discoveredDevices.value = allDevices

        android.util.Log.d("BluetoothRepo", "Total devices (BLE + paired): ${allDevices.size}")
        trySend(allDevices).isSuccess

        awaitClose {
            android.util.Log.d("BluetoothRepo", "Scan flow closed")
            try { scanner.stopScan(scanCallback) } catch (_: Throwable) {}
            if (activeScanCallback === scanCallback) {
                activeScanCallback = null
                activeScannerRef = null
            }
        }
    }

    // --------------------- Device Connection ---------------------
    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String): Flow<ConnectionStatus> = callbackFlow {
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        if (device == null) {
            trySend(ConnectionStatus(ConnectionState.ERROR, null, "Device not found"))
            close()
            return@callbackFlow
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _connectionStatus.value = ConnectionStatus(
                            ConnectionState.CONNECTED,
                            BluetoothDevice(device.name, device.address, 0, true)
                        )
                        gatt?.discoverServices()
                        trySend(_connectionStatus.value)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _connectionStatus.value = ConnectionStatus(ConnectionState.DISCONNECTED, null)
                        trySend(_connectionStatus.value)
                        close()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) { }
            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) { }
            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) { }
        }

        _connectionStatus.value = ConnectionStatus(ConnectionState.CONNECTING, null)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        awaitClose {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionStatus.value = ConnectionStatus(ConnectionState.DISCONNECTED, null)
    }

    @SuppressLint("MissingPermission")
    fun writeData(serviceUuid: UUID, characteristicUuid: UUID, data: ByteArray): Boolean {
        val service = bluetoothGatt?.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUuid) ?: return false
        characteristic.value = data
        return bluetoothGatt?.writeCharacteristic(characteristic) ?: false
    }

    @SuppressLint("MissingPermission")
    fun readData(serviceUuid: UUID, characteristicUuid: UUID): Boolean {
        val service = bluetoothGatt?.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUuid) ?: return false
        return bluetoothGatt?.readCharacteristic(characteristic) ?: false
    }

    // --------------------- Paired Devices ---------------------
    fun savePairedDevice(device: BluetoothDevice) {
        preferences.savePairedDevice(device)
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return preferences.getPairedDevices()
    }

    @SuppressLint("MissingPermission")
    fun getSystemPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.map {
                BluetoothDevice(
                    name = it.name,
                    address = it.address,
                    rssi = 0,
                    deviceType = DeviceType.CLASSIC,
                    isConnected = it.bondState == android.bluetooth.BluetoothDevice.BOND_BONDED
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
