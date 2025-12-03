package com.example.androidapp.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
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
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private val _connectionStatus = MutableStateFlow(
        ConnectionStatus(ConnectionState.DISCONNECTED, null)
    )
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    // Track currently active scan so it can be stopped externally
    @Volatile
    private var activeScanCallback: ScanCallback? = null
    @Volatile
    private var activeScannerRef: BluetoothLeScanner? = null

    // Stop any active scan immediately
    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            activeScannerRef?.stopScan(activeScanCallback)
            android.util.Log.d("BluetoothRepository", "stopScan: stopped active scanner")
        } catch (t: Throwable) {
            android.util.Log.w("BluetoothRepository", "stopScan threw: ${t.message}")
            // ignore
        } finally {
            activeScanCallback = null
            activeScannerRef = null
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun enableBluetooth() {
        bluetoothAdapter?.enable()
    }

    @SuppressLint("MissingPermission")
    fun disableBluetooth() {
        bluetoothAdapter?.disable()
    }

    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    // Return a list of permissions that are not currently granted (for debugging/requests)
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.BLUETOOTH_SCAN)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.BLUETOOTH)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.BLUETOOTH_ADMIN)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return missing
    }

    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 10000): Flow<List<BluetoothDevice>> = callbackFlow {
        // Validate preconditions: permissions and Bluetooth availability
        val missing = getMissingPermissions()
        if (missing.isNotEmpty()) {
            android.util.Log.w("BluetoothRepository", "startScan: missing permissions: $missing")
            close(IllegalStateException("Missing permissions: $missing"))
            return@callbackFlow
        }

        if (!isBluetoothEnabled()) {
            android.util.Log.w("BluetoothRepository", "startScan: Bluetooth is disabled")
            close(IllegalStateException("Bluetooth is disabled"))
            return@callbackFlow
        }

        // Re-evaluate scanner at call-time in case adapter state changed
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            android.util.Log.e("BluetoothRepository", "startScan: bluetoothLeScanner is null (adapter: $bluetoothAdapter)")
            close(IllegalStateException("BLE scanner not available"))
            return@callbackFlow
        }

        android.util.Log.d("BluetoothRepository", "startScan: scanner available, starting scan for $durationMs ms")

        val devices = mutableMapOf<String, BluetoothDevice>()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    // Extract advertised service UUIDs if present
                    val services = it.scanRecord?.serviceUuids?.mapNotNull { pu -> pu?.uuid?.toString() } ?: emptyList()

                    val device = BluetoothDevice(
                        name = it.device.name,
                        address = it.device.address,
                        rssi = it.rssi,
                        deviceType = DeviceType.BLE,
                        services = services
                    )

                    devices[device.address] = device

                    // Update internal discovered devices state so UI observing this flow sees changes
                    _discoveredDevices.value = devices.values.toList()

                    // Log discovery for debugging
                    android.util.Log.d("BluetoothRepository", "onScanResult: addr=${device.address}, name=${device.name}, rssi=${device.rssi}, services=${device.services}")

                    val sendResult = trySend(devices.values.toList())
                    android.util.Log.d("BluetoothRepository", "trySend called, success=${sendResult.isSuccess}")
                    if (!sendResult.isSuccess) {
                        android.util.Log.w("BluetoothRepository", "trySend failed for device list: ${sendResult} ")
                    } else {
                        android.util.Log.d("BluetoothRepository", "trySend success, total devices=${devices.size}")
                    }
                } ?: run {
                    android.util.Log.w("BluetoothRepository", "onScanResult: result was null")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                android.util.Log.e("BluetoothRepository", "onScanFailed: error=$errorCode")
                close(Exception("Scan failed with error: $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            // register active scan
            activeScannerRef = scanner
            activeScanCallback = scanCallback

            android.util.Log.d("BluetoothRepository", "calling bluetoothLeScanner.startScan(...) with filters=null and settings=$settings")
            scanner.startScan(null, settings, scanCallback)
            android.util.Log.d("BluetoothRepository", "startScan: scanner.startScan returned")
        } catch (se: SecurityException) {
            android.util.Log.e("BluetoothRepository", "startScan SecurityException: ${se.message}")
            close(se)
            return@callbackFlow
        } catch (t: Throwable) {
            android.util.Log.e("BluetoothRepository", "startScan failed: ${t.message}")
            close(t)
            return@callbackFlow
        }

        // Run scan for durationMs, then stop and close the flow with final results
        kotlinx.coroutines.delay(durationMs)
        try {
            android.util.Log.d("BluetoothRepository", "duration elapsed, stopping scanner")
            scanner.stopScan(scanCallback)
        } catch (t: Throwable) {
            android.util.Log.w("BluetoothRepository", "stopScan threw: ${t.message}")
        }

        // Send final list and update internal state
        _discoveredDevices.value = devices.values.toList()
        val finalSend = trySend(devices.values.toList())
        android.util.Log.d("BluetoothRepository", "final trySend success=${finalSend.isSuccess}")
        if (!finalSend.isSuccess) {
            android.util.Log.w("BluetoothRepository", "final trySend failed: $finalSend")
        }

        // Close the flow to signal completion
        close()

        // clear active references and stop if caller cancels
        awaitClose {
            android.util.Log.d("BluetoothRepository", "awaitClose called, cleaning up scan callback and scanner reference")
            try {
                scanner.stopScan(scanCallback)
            } catch (t: Throwable) {
                android.util.Log.w("BluetoothRepository", "awaitClose stopScan threw: ${t.message}")
            } finally {
                if (activeScanCallback === scanCallback) {
                    activeScanCallback = null
                    activeScannerRef = null
                }
            }
        }
    }

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
                        _connectionStatus.value = ConnectionStatus(
                            ConnectionState.DISCONNECTED,
                            null
                        )
                        trySend(_connectionStatus.value)
                        close()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Services discovered successfully
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Handle characteristic read
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Handle characteristic write
                }
            }
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

    fun savePairedDevice(device: BluetoothDevice) {
        preferences.savePairedDevice(device)
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return preferences.getPairedDevices()
    }
}