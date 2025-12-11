package com.example.androidapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.androidapp.AuthApplication
import com.example.androidapp.ChannelManager
import com.example.androidapp.databinding.ActivityMainBinding
import com.example.androidapp.data.remote.RetrofitClient
import com.example.androidapp.data.repository.AdyenPaymentRepository
import com.example.androidapp.ui.util.AnalyticsHelper
import com.example.androidapp.ui.util.PermissionHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.FlutterEngineCache
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var flutterEngine: FlutterEngine

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val ENGINE_ID = "main"

    private val REQUEST_RUNTIME_PERMISSIONS = 4201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get Firebase Analytics instance
        firebaseAnalytics = (application as AuthApplication).firebaseAnalytics

        // Check Google Play Services availability
        checkGooglePlayServices()

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Initialize FlutterEngine ---
        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Initialize FlutterEngine
        flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )

        // Setup ChannelManager
        ChannelManager.setup(flutterEngine, this)
        // --- Setup ChannelManager (no need to instantiate) ---
        ChannelManager.setup(flutterEngine, this)

        // --- Cache FlutterEngine ---
        FlutterEngineCache.getInstance().put(ENGINE_ID, flutterEngine)

        // Log screen view
        AnalyticsHelper.logScreenView(firebaseAnalytics, "MainActivity")

        // Log FCM token
        val fcmToken = (application as AuthApplication).getFCMToken()
        Timber.d("Current FCM Token: $fcmToken")

        // Add test crash button for Firebase Crashlytics testing
        addTestCrashButton()
    }

    private fun addTestCrashButton() {
        // Creates a button that mimics a crash when pressed
        val crashButton = Button(this)
        crashButton.text = "Test Crash"
        crashButton.setTextSize(16f)
        crashButton.setPadding(48, 32, 48, 32)
        crashButton.setBackgroundColor(0xFFFF5252.toInt()) // Red color
        crashButton.setTextColor(0xFFFFFFFF.toInt()) // White text

        crashButton.setOnClickListener {
            Timber.w("Test crash button clicked - forcing crash!")
            Toast.makeText(this, "Crashing in 3...2...1...", Toast.LENGTH_SHORT).show()
            throw RuntimeException("Test Crash") // Force a crash
        }

        // Use FrameLayout.LayoutParams with BOTTOM and CENTER_HORIZONTAL gravity
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 100 // 100px from bottom
        }

        addContentView(crashButton, params)
        Timber.d("Test crash button added to MainActivity at bottom center")
    }

    private fun requestNotificationPermission() {
        if (!PermissionHelper.hasNotificationPermission(this)) {
            PermissionHelper.requestNotificationPermission(this)
        }
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Timber.d("Google Play Services is available and up to date")
            }
            ConnectionResult.SERVICE_MISSING -> {
                Timber.e("Google Play Services is missing - FCM will not work!")
                Toast.makeText(this, "Google Play Services is missing. FCM notifications won't work.", Toast.LENGTH_LONG).show()
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Timber.w("Google Play Services needs update")
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
                }
            }
            ConnectionResult.SERVICE_DISABLED -> {
                Timber.e("Google Play Services is disabled")
                Toast.makeText(this, "Google Play Services is disabled. Please enable it.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Timber.e("Google Play Services error: $resultCode")
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
                }
            }
        }

        // Ensure we have the runtime permissions needed for Bluetooth & Wi‑Fi scanning.
        checkAndRequestRuntimePermissions()
    }

    override fun onDestroy() {
        super.onDestroy()

    }


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
