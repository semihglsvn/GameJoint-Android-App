package com.gamejoint.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creates a single, safe instance of DataStore tied to the Application Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {

    companion object {
        // The exact key used to save and retrieve the JWT string
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    /**
     * Reads the token from storage securely.
     * Returns a Flow, which plugs perfectly into Jetpack Compose states.
     */
    val jwtTokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[JWT_TOKEN_KEY]
        }

    /**
     * Saves the token after a successful login or registration.
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[JWT_TOKEN_KEY] = token
        }
    }

    /**
     * Deletes the token (Used when the user logs out).
     */
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN_KEY)
        }
    }
}