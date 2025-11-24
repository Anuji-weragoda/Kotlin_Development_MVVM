package com.example.androidapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_FULLNAME_KEY = stringPreferencesKey("user_fullname")
    }


    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[ACCESS_TOKEN_KEY] = accessToken
                preferences[REFRESH_TOKEN_KEY] = refreshToken
            }
            Timber.d("Tokens saved successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error saving tokens")
            throw e
        }
    }

    suspend fun saveUserInfo(userId: String, email: String, fullName: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = userId
                preferences[USER_EMAIL_KEY] = email
                preferences[USER_FULLNAME_KEY] = fullName
            }
            Timber.d("User info saved successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error saving user info")
            throw e
        }
    }


    fun getAccessToken(): Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }
    fun getRefreshToken(): Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }
    fun getUserId(): Flow<String?> = context.dataStore.data.map { it[USER_ID_KEY] }
    fun getUserEmail(): Flow<String?> = context.dataStore.data.map { it[USER_EMAIL_KEY] }
    fun getUserFullName(): Flow<String?> = context.dataStore.data.map { it[USER_FULLNAME_KEY] }


    fun isLoggedIn(): Flow<Boolean> = context.dataStore.data.map { !it[ACCESS_TOKEN_KEY].isNullOrEmpty() }


    suspend fun clearTokens() {
        try {
            context.dataStore.edit { it.clear() }
            Timber.d("All tokens and user info cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing tokens")
        }
    }


    suspend fun clearAuthTokens() {
        try {
            context.dataStore.edit {
                it.remove(ACCESS_TOKEN_KEY)
                it.remove(REFRESH_TOKEN_KEY)
            }
            Timber.d("Access and refresh tokens cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing auth tokens")
        }
    }
}
