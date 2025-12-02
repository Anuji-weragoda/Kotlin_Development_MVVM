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

    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 10000): Flow<List<BluetoothDevice>> = callbackFlow {
        val devices = mutableMapOf<String, BluetoothDevice>()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    val device = BluetoothDevice(
                        name = it.device.name,
                        address = it.device.address,
                        rssi = it.rssi,
                        deviceType = DeviceType.BLE
                    )
                    devices[device.address] = device
                    trySend(devices.values.toList())
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("Scan failed with error: $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner?.startScan(null, settings, scanCallback)

        kotlinx.coroutines.delay(durationMs)
        bluetoothLeScanner?.stopScan(scanCallback)

        awaitClose {
            bluetoothLeScanner?.stopScan(scanCallback)
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