package com.gamejoint.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.UserLoginRequest
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    data class Unverified(val email: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginState>(LoginState.Idle)
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    fun login(usernameOrEmail: String, pass: String) {
        _uiState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val request = UserLoginRequest(
                    usernameOrEmail = usernameOrEmail,
                    password = pass,
                    cfTurnstileResponse = "mobile-bypass"
                )

                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.login(request).execute()
                }

                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        _uiState.value = LoginState.Success(token)
                    } else {
                        _uiState.value = LoginState.Error("Server returned an empty token.")
                    }
                } else {
                    val errorString = response.errorBody()?.string() ?: ""

                    if (errorString.contains("not verified", ignoreCase = true)) {
                        _uiState.value = LoginState.Unverified(usernameOrEmail)
                    } else {
                        // --- NEW: Parse the exact Spring Boot error message! ---
                        var displayMessage = "Login failed (Error ${response.code()})"
                        try {
                            // If it's a JSON response, extract the "message" field
                            if (errorString.startsWith("{")) {
                                val json = JSONObject(errorString)
                                if (json.has("message")) {
                                    displayMessage = json.getString("message")
                                } else if (json.has("error")) {
                                    displayMessage = json.getString("error")
                                }
                            } else if (errorString.isNotBlank()) {
                                // If the backend sent plain text, just use that
                                displayMessage = errorString
                            }
                        } catch (e: Exception) {
                            if (errorString.isNotBlank()) displayMessage = errorString
                        }

                        _uiState.value = LoginState.Error(displayMessage)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }
}   