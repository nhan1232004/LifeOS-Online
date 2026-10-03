package com.nhan.lifeos.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserSession(
    val isGuestMode: Boolean = false,
    val userEmail: String = "",
    val displayName: String = "",
    val activeWorkspace: String = "Personal"
) {
    val isLoggedIn: Boolean get() = isGuestMode || userEmail.isNotBlank()
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val ACTIVE_WORKSPACE = stringPreferencesKey("active_workspace")
    }

    val userSessionFlow: Flow<UserSession> = context.dataStore.data.map { preferences ->
        val isGuest = preferences[PreferencesKeys.IS_GUEST_MODE] ?: false
        val email = preferences[PreferencesKeys.USER_EMAIL] ?: ""
        val name = preferences[PreferencesKeys.DISPLAY_NAME] ?: ""
        val workspace = preferences[PreferencesKeys.ACTIVE_WORKSPACE] ?: "Personal"
        UserSession(
            isGuestMode = isGuest,
            userEmail = email,
            displayName = name,
            activeWorkspace = workspace
        )
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST_MODE] = isGuest
            if (isGuest) {
                preferences[PreferencesKeys.DISPLAY_NAME] = "Người dùng Khách"
                preferences[PreferencesKeys.USER_EMAIL] = "local@lifeos.app"
            }
        }
    }

    suspend fun setUserSession(email: String, displayName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST_MODE] = false
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.DISPLAY_NAME] = displayName
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST_MODE] = false
            preferences[PreferencesKeys.USER_EMAIL] = ""
            preferences[PreferencesKeys.DISPLAY_NAME] = ""
        }
    }
}
