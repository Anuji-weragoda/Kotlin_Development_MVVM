package com.example.androidapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.androidapp.data.model.BluetoothDevice
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BluetoothPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "bluetooth_prefs"
        private const val KEY_PAIRED_DEVICES = "paired_devices"
        private const val KEY_LAST_CONNECTED = "last_connected"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_SCAN_DURATION = "scan_duration"
    }

    fun savePairedDevice(device: BluetoothDevice) {
        val devices = getPairedDevices().toMutableList()
        devices.removeIf { it.address == device.address }
        devices.add(device)

        prefs.edit()
            .putString(KEY_PAIRED_DEVICES, gson.toJson(devices))
            .apply()
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        val json = prefs.getString(KEY_PAIRED_DEVICES, null) ?: return emptyList()
        val type = object : TypeToken<List<BluetoothDevice>>() {}.type
        return gson.fromJson(json, type)
    }

    fun removePairedDevice(address: String) {
        val devices = getPairedDevices().toMutableList()
        devices.removeIf { it.address == address }

        prefs.edit()
            .putString(KEY_PAIRED_DEVICES, gson.toJson(devices))
            .apply()
    }

    fun saveLastConnectedDevice(device: BluetoothDevice) {
        prefs.edit()
            .putString(KEY_LAST_CONNECTED, gson.toJson(device))
            .apply()
    }

    fun getLastConnectedDevice(): BluetoothDevice? {
        val json = prefs.getString(KEY_LAST_CONNECTED, null) ?: return null
        return gson.fromJson(json, BluetoothDevice::class.java)
    }

    var autoReconnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECONNECT, value).apply()

    var scanDuration: Long
        get() = prefs.getLong(KEY_SCAN_DURATION, 10000L)
        set(value) = prefs.edit().putLong(KEY_SCAN_DURATION, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}