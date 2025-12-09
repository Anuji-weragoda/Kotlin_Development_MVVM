package com.example.androidapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.androidapp.AuthApplication
import com.example.androidapp.ChannelManager
import com.example.androidapp.databinding.ActivityMainBinding
import com.example.androidapp.data.remote.RetrofitClient
import com.example.androidapp.data.repository.AdyenPaymentRepository
import com.example.androidapp.utils.AnalyticsHelper
import com.example.androidapp.utils.PermissionHelper
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

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Initialize FlutterEngine
        flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault())

        // Setup ChannelManager
        ChannelManager.setup(flutterEngine, this)

        // Cache the engine for reuse
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
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Notification permission granted")
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Timber.d("Notification permission denied")
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (!ChannelManager.onActivityResult(requestCode, resultCode, data)) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}