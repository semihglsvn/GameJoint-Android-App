package com.gamejoint.app.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

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

    /**
     * Decodes the JWT token to extract the username safely.
     * Returns null if the token is invalid, empty, or malformed.
     */
    fun getUsernameFromToken(token: String?): String? {
        if (token.isNullOrEmpty()) return null

        return try {
            // JWTs are split into 3 parts: Header.Payload.Signature. We want the Payload (index 1).
            val payload = String(Base64.decode(token.split(".")[1], Base64.URL_SAFE))
            val json = JSONObject(payload)

            // Check for 'sub' first, fallback to 'username'
            if (json.has("sub")) {
                json.getString("sub")
            } else {
                json.optString("username", null)
            }
        } catch (e: Exception) {
            null // Safely fail instead of crashing the app
        }
    }
}