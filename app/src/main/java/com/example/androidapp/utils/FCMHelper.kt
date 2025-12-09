package com.example.androidapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.example.androidapp.R
import com.example.androidapp.ui.MainActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

/**
 * Helper class for Firebase Cloud Messaging operations and notifications
 */
object FCMHelper {

    // Notification Channel IDs
    const val CHANNEL_ID_GENERAL = "general_channel"
    const val CHANNEL_ID_PAYMENT = "payment_channel"
    const val CHANNEL_ID_PROMOTION = "promotion_channel"

    /**
     * Create notification channels for Android 8.0+
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // General channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            // Payment channel
            val paymentChannel = NotificationChannel(
                CHANNEL_ID_PAYMENT,
                "Payment Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Payment related notifications"
            }

            // Promotion channel
            val promotionChannel = NotificationChannel(
                CHANNEL_ID_PROMOTION,
                "Promotion Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Promotional notifications"
            }

            notificationManager.createNotificationChannel(generalChannel)
            notificationManager.createNotificationChannel(paymentChannel)
            notificationManager.createNotificationChannel(promotionChannel)
        }
    }

    /**
     * Show a local notification
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_ID_GENERAL
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Subscribe to a topic
     */
    fun subscribeToTopic(topic: String, onComplete: ((Boolean) -> Unit)? = null) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("Subscribed to topic: $topic")
                    onComplete?.invoke(true)
                } else {
                    Timber.e(task.exception, "Failed to subscribe to topic: $topic")
                    onComplete?.invoke(false)
                }
            }
    }

    /**
     * Unsubscribe from a topic
     */
    fun unsubscribeFromTopic(topic: String, onComplete: ((Boolean) -> Unit)? = null) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("Unsubscribed from topic: $topic")
                    onComplete?.invoke(true)
                } else {
                    Timber.e(task.exception, "Failed to unsubscribe from topic: $topic")
                    onComplete?.invoke(false)
                }
            }
    }

    /**
     * Get current FCM token
     */
    fun getToken(onComplete: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.w(task.exception, "Fetching FCM registration token failed")
                onComplete(null)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Timber.d("FCM Token: $token")
            onComplete(token)
        }
    }

    /**
     * Log notification received event to Firebase Analytics
     */
    fun logNotificationReceived(
        firebaseAnalytics: FirebaseAnalytics,
        notificationType: String,
        notificationId: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("notification_type", notificationType)
            notificationId?.let { putString("notification_id", it) }
            putLong("timestamp", System.currentTimeMillis())
        }
        firebaseAnalytics.logEvent("notification_received", bundle)
        Timber.d("Logged notification_received event: $notificationType")
    }

    /**
     * Log notification opened event to Firebase Analytics
     */
    fun logNotificationOpened(
        firebaseAnalytics: FirebaseAnalytics,
        notificationType: String,
        notificationId: String? = null
    ) {
        val bundle = Bundle().apply {
            putString("notification_type", notificationType)
            notificationId?.let { putString("notification_id", it) }
            putLong("timestamp", System.currentTimeMillis())
        }
        firebaseAnalytics.logEvent("notification_opened", bundle)
        Timber.d("Logged notification_opened event: $notificationType")
    }

    /**
     * Delete FCM token
     */
    fun deleteToken(onComplete: ((Boolean) -> Unit)? = null) {
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.d("FCM token deleted")
                    onComplete?.invoke(true)
                } else {
                    Timber.e(task.exception, "Failed to delete FCM token")
                    onComplete?.invoke(false)
                }
            }
    }

    /**
     * Common notification topics
     */
    object Topics {
        const val ALL_USERS = "all_users"
        const val PROMOTIONS = "promotions"
        const val PAYMENTS = "payments"
        const val UPDATES = "updates"
        const val NEWS = "news"
    }

    /**
     * Notification types
     */
    object NotificationTypes {
        const val GENERAL = "general"
        const val PAYMENT_STATUS = "payment_status"
        const val PROMOTION = "promotion"
        const val UPDATE = "update"
        const val NEWS = "news"
        const val ALERT = "alert"
    }
}

