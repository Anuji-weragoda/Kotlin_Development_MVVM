package com.example.androidapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.androidapp.ChannelManager
import com.example.androidapp.databinding.ActivityMainBinding
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.FlutterEngineCache

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var flutterEngine: FlutterEngine

    private val ENGINE_ID = "main"

    private val REQUEST_RUNTIME_PERMISSIONS = 4201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Initialize FlutterEngine ---
        flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )

        // --- Setup ChannelManager (no need to instantiate) ---
        ChannelManager.setup(flutterEngine, this)

        // --- Cache FlutterEngine ---
        FlutterEngineCache.getInstance().put(ENGINE_ID, flutterEngine)

        // Ensure we have the runtime permissions needed for Bluetooth & Wi‑Fi scanning.
        checkAndRequestRuntimePermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        ChannelManager.dispose()
    }

    // Forward permission results to ChannelManager so native handlers can react
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ChannelManager.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // If this is our combined runtime request, also forward to ChannelManager just in case
        if (requestCode == REQUEST_RUNTIME_PERMISSIONS) {
            ChannelManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // Build a compact set of runtime permissions we need and request any that are missing.
    private fun checkAndRequestRuntimePermissions() {
        val required = mutableListOf<String>()

        // Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                required.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                required.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            // Location is also needed for accurate BLE scanning even on Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (!required.contains(Manifest.permission.ACCESS_FINE_LOCATION)) required.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                required.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Wi‑Fi scanning permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                required.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        // Always add location permissions - they are required for Wi-Fi scanning on ALL Android versions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!required.contains(Manifest.permission.ACCESS_FINE_LOCATION)) required.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!required.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) required.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (required.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, required.toTypedArray(), REQUEST_RUNTIME_PERMISSIONS)
        }
    }
}
