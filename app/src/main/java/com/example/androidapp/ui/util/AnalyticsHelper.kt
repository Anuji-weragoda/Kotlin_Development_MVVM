package com.example.androidapp.ui.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {

    // Common event names
    object Events {
        const val USER_LOGIN = "user_login"
        const val USER_LOGOUT = "user_logout"
        const val USER_SIGNUP = "user_signup"
        const val SCREEN_VIEW = "screen_view"
        const val BUTTON_CLICK = "button_click"
        const val PAYMENT_INITIATED = "payment_initiated"
        const val PAYMENT_SUCCESS = "payment_success"
        const val PAYMENT_FAILED = "payment_failed"
        const val FLUTTER_DASHBOARD_OPENED = "flutter_dashboard_opened"
    }

    // Common parameter names
    object Params {
        const val SCREEN_NAME = "screen_name"
        const val BUTTON_NAME = "button_name"
        const val USER_ID = "user_id"
        const val AMOUNT = "amount"
        const val CURRENCY = "currency"
        const val ERROR_MESSAGE = "error_message"
        const val SUCCESS = "success"
    }

    /**
     * Log screen view event
     */
    fun logScreenView(analytics: FirebaseAnalytics, screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    /**
     * Log user login event
     */
    fun logLogin(analytics: FirebaseAnalytics, method: String = "email") {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    /**
     * Log user signup event
     */
    fun logSignUp(analytics: FirebaseAnalytics, method: String = "email") {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    /**
     * Log button click event
     */
    fun logButtonClick(analytics: FirebaseAnalytics, buttonName: String, screenName: String? = null) {
        val bundle = Bundle().apply {
            putString(Params.BUTTON_NAME, buttonName)
            screenName?.let { putString(Params.SCREEN_NAME, it) }
        }
        analytics.logEvent(Events.BUTTON_CLICK, bundle)
    }

    /**
     * Log custom event with parameters
     */
    fun logCustomEvent(analytics: FirebaseAnalytics, eventName: String, params: Map<String, Any>? = null) {
        val bundle = params?.let {
            Bundle().apply {
                it.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Boolean -> putBoolean(key, value)
                    }
                }
            }
        }
        analytics.logEvent(eventName, bundle)
        // //
    }
}