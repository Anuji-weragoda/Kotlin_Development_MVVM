package com.example.androidapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.androidapp.data.model.SavedWifiNetwork
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class WifiPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "wifi_prefs"
        private const val KEY_SAVED_NETWORKS = "saved_networks"
        private const val KEY_CURRENT_NETWORK = "current_network"
        private const val KEY_AUTO_CONNECT = "auto_connect"
    }

    private val encryptionHelper = EncryptionHelper(context)

    fun saveNetwork(network: SavedWifiNetwork) {
        val networks = getSavedNetworks().toMutableList()
        networks.removeIf { it.ssid == network.ssid }

        // Encrypt password before saving
        val encryptedNetwork = network.copy(
            password = encryptionHelper.encrypt(network.password)
        )
        networks.add(encryptedNetwork)

        prefs.edit {
            putString(KEY_SAVED_NETWORKS, gson.toJson(networks))
        }
    }

    fun getSavedNetworks(): List<SavedWifiNetwork> {
        val json = prefs.getString(KEY_SAVED_NETWORKS, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedWifiNetwork>>() {}.type
        val networks: List<SavedWifiNetwork> = gson.fromJson(json, type)

        // Decrypt passwords when retrieving
        return networks.map { network ->
            network.copy(password = encryptionHelper.decrypt(network.password))
        }
    }

    fun getNetworkPassword(ssid: String): String? {
        return getSavedNetworks().find { it.ssid == ssid }?.password
    }

    fun removeNetwork(ssid: String) {
        val networks = getSavedNetworks().toMutableList()
        networks.removeIf { it.ssid == ssid }

        prefs.edit {
            putString(KEY_SAVED_NETWORKS, gson.toJson(networks.map {
                it.copy(password = encryptionHelper.encrypt(it.password))
            }))
        }
    }

    fun isNetworkSaved(ssid: String): Boolean {
        return getSavedNetworks().any { it.ssid == ssid }
    }

    fun saveCurrentNetwork(ssid: String) {
        prefs.edit {
            putString(KEY_CURRENT_NETWORK, ssid)
        }
    }

    fun getCurrentNetwork(): String? {
        return prefs.getString(KEY_CURRENT_NETWORK, null)
    }

    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_CONNECT, value) }

    fun clearAll() {
        prefs.edit { clear() }
    }
}


private class EncryptionHelper(private val context: Context) {

    private val prefs = context.getSharedPreferences("encryption_prefs", Context.MODE_PRIVATE)
    private val secretKey: SecretKey by lazy { getOrCreateKey() }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ENCRYPTED_KEY = "encrypted_key"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return plainText

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combine IV and encrypted data
            val combined = iv + encryptedBytes
            return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            // Fallback to plain text if encryption fails
            return plainText
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return encryptedText

        try {
            val combined = android.util.Base64.decode(encryptedText, android.util.Base64.DEFAULT)

            // Extract IV and encrypted data
            val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
            val encryptedBytes = combined.sliceArray(GCM_IV_LENGTH until combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback to encrypted text if decryption fails (might be plain text)
            return encryptedText
        }
    }

    private fun getOrCreateKey(): SecretKey {
        // Try to load existing key
        val existingKey = prefs.getString(KEY_ENCRYPTED_KEY, null)
        if (existingKey != null) {
            try {
                val keyBytes = android.util.Base64.decode(existingKey, android.util.Base64.DEFAULT)
                return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            } catch (e: Exception) {
                // If loading fails, create new key
            }
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val key = keyGenerator.generateKey()

        // Save key
        val encodedKey = android.util.Base64.encodeToString(key.encoded, android.util.Base64.DEFAULT)
        prefs.edit {
            putString(KEY_ENCRYPTED_KEY, encodedKey)
        }

        return key
    }
}