package com.nhan.lifeos.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserSession(
    val isGuestMode: Boolean = false,
    val userEmail: String = "",
    val displayName: String = "",
    val userId: String = "",
    val idToken: String = "",
    val refreshToken: String = "",
    val lastSyncedAt: Long = 0L,
    val activeWorkspace: String = "Personal"
) {
    val isLoggedIn: Boolean get() = isGuestMode || userEmail.isNotBlank()
    val canSyncOnline: Boolean get() = !isGuestMode && userId.isNotBlank() && idToken.isNotBlank()
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val USER_ID = stringPreferencesKey("user_id")
        val ID_TOKEN = stringPreferencesKey("id_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
        val ACTIVE_WORKSPACE = stringPreferencesKey("active_workspace")
    }

    val userSessionFlow: Flow<UserSession> = context.dataStore.data.map { preferences ->
        val isGuest = preferences[PreferencesKeys.IS_GUEST_MODE] ?: false
        val email = preferences[PreferencesKeys.USER_EMAIL] ?: ""
        val name = preferences[PreferencesKeys.DISPLAY_NAME] ?: ""
        val uid = preferences[PreferencesKeys.USER_ID] ?: ""
        val idToken = preferences[PreferencesKeys.ID_TOKEN] ?: ""
        val refreshToken = preferences[PreferencesKeys.REFRESH_TOKEN] ?: ""
        val lastSynced = preferences[PreferencesKeys.LAST_SYNCED_AT] ?: 0L
        val workspace = preferences[PreferencesKeys.ACTIVE_WORKSPACE] ?: "Personal"
        UserSession(
            isGuestMode = isGuest,
            userEmail = email,
            displayName = name,
            userId = uid,
            idToken = idToken,
            refreshToken = refreshToken,
            lastSyncedAt = lastSynced,
            activeWorkspace = workspace
        )
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST_MODE] = isGuest
            if (isGuest) {
                preferences[PreferencesKeys.DISPLAY_NAME] = "Người dùng Khách"
                preferences[PreferencesKeys.USER_EMAIL] = "local@lifeos.app"
                preferences[PreferencesKeys.USER_ID] = "guest"
                preferences[PreferencesKeys.ID_TOKEN] = ""
                preferences[PreferencesKeys.REFRESH_TOKEN] = ""
            }
        }
    }

    suspend fun setUserSession(
        email: String,
        displayName: String,
        userId: String = "",
        idToken: String = "",
        refreshToken: String = ""
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST_MODE] = false
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.DISPLAY_NAME] = displayName
            if (userId.isNotBlank()) preferences[PreferencesKeys.USER_ID] = userId
            if (idToken.isNotBlank()) preferences[PreferencesKeys.ID_TOKEN] = idToken
            if (refreshToken.isNotBlank()) preferences[PreferencesKeys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun updateTokens(idToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ID_TOKEN] = idToken
            if (refreshToken.isNotBlank()) {
                preferences[PreferencesKeys.REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun updateLastSyncedAt(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNCED_AT] = timestamp
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST_MODE] = false
            preferences[PreferencesKeys.USER_EMAIL] = ""
            preferences[PreferencesKeys.DISPLAY_NAME] = ""
            preferences[PreferencesKeys.USER_ID] = ""
            preferences[PreferencesKeys.ID_TOKEN] = ""
            preferences[PreferencesKeys.REFRESH_TOKEN] = ""
            preferences[PreferencesKeys.LAST_SYNCED_AT] = 0L
        }
    }
}
