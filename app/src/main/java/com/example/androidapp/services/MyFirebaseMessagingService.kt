package com.example.androidapp.services

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.androidapp.AuthApplication
import com.example.androidapp.R
import com.example.androidapp.ui.MainActivity
import com.example.androidapp.ui.util.FCMHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Timber.d("FCM Message received from: ${remoteMessage.from}")

        val firebaseAnalytics = (application as AuthApplication).firebaseAnalytics

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Timber.d("Message Notification Body: ${it.body}")

            // Log notification received to Analytics
            FCMHelper.logNotificationReceived(
                firebaseAnalytics,
                remoteMessage.data["type"] ?: FCMHelper.NotificationTypes.GENERAL,
                remoteMessage.messageId
            )

            sendNotification(it.title, it.body, remoteMessage.data["type"])
        }

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Message data payload: ${remoteMessage.data}")
            handleDataPayload(remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("Refreshed FCM token: $token")
        sendRegistrationToServer(token)
    }

    private fun handleDataPayload(data: Map<String, String>) {

        val title = data["title"]
        val body = data["body"]
        val type = data["type"]

        val firebaseAnalytics = (application as AuthApplication).firebaseAnalytics

        // Log notification received to Analytics
        FCMHelper.logNotificationReceived(
            firebaseAnalytics,
            type ?: FCMHelper.NotificationTypes.GENERAL
        )

        when (type) {
            "payment_status" -> {
                // Handle payment notification
                sendNotification(title, body, type)
            }
            "promotion" -> {
                // Handle promotion notification
                sendNotification(title, body, type)
            }
            else -> {
                sendNotification(title, body, type)
            }
        }
    }

    private fun sendNotification(title: String?, messageBody: String?, type: String? = null) {
        Timber.d("sendNotification called - Title: $title, Body: $messageBody, Type: $type")

        // Check if app is in foreground
        val isAppInForeground = isAppInForeground()
        Timber.d("Is app in foreground: $isAppInForeground")

        // If app is in foreground, show an in-app notification (Toast)
        if (isAppInForeground) {
            showInAppNotification(title, messageBody)
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.e("POST_NOTIFICATIONS permission not granted! Cannot show notification.")
                return
            } else {
                Timber.d("POST_NOTIFICATIONS permission is granted")
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.default_notification_channel_id)
        Timber.d("Using channel ID: $channelId")

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title ?: "Notification")
            .setContentText(messageBody ?: "")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                Timber.d("Creating notification channel: $channelId")
                val channel = NotificationChannel(
                    channelId,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for general app notifications"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
                Timber.d("Notification channel created successfully")
            } else {
                Timber.d("Notification channel already exists")
            }
        }

        // Check if notifications are enabled
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Timber.e("Notifications are disabled for this app!")
            return
        }

        val notificationId = System.currentTimeMillis().toInt()
        Timber.d("Posting notification with ID: $notificationId")

        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
            Timber.d(" Notification posted successfully! Swipe down notification shade to see it.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to post notification")
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false

        for (processInfo in runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (processInfo.processName == packageName) {
                    return true
                }
            }
        }
        return false
    }

    private fun showInAppNotification(title: String?, message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                this,
                " $title: $message",
                Toast.LENGTH_LONG
            ).show()
            Timber.d("In-app notification shown: $title - $message")
        }
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server
        Timber.d("Sending token to server: $token")

        val sharedPreferences = getSharedPreferences("FCM_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("FCM_TOKEN", token).apply()
    }

    companion object {
        private const val TAG = "FCMService"
    }
}

