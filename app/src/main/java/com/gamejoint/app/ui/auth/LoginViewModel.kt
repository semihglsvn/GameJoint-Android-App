package com.gamejoint.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.OAuthAuthResponse
import com.gamejoint.app.data.model.OAuthLoginRequest
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
    data class OAuthIncomplete(val email: String, val providerToken: String) : LoginState() // NEW
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
                        _uiState.value = LoginState.Error(parseBackendError(errorString, response.code()))
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }

    // NEW: OAuth Login Flow
    fun oauthLogin(provider: String, token: String) {
        _uiState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val request = OAuthLoginRequest(
                    provider = provider,
                    providerToken = token,
                    cfTurnstileResponse = "mobile-bypass"
                )

                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.oauthLogin(request).execute()
                }

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        if (authResponse.isNewUser) {
                            // Backend requires profile completion
                            _uiState.value = LoginState.OAuthIncomplete(
                                email = authResponse.email ?: "",
                                providerToken = token
                            )
                        } else {
                            // User exists, logged in successfully
                            _uiState.value = LoginState.Success(authResponse.jwtToken ?: "")
                        }
                    } else {
                        _uiState.value = LoginState.Error("Server returned an empty response.")
                    }
                } else {
                    val errorString = response.errorBody()?.string() ?: ""

                    // Handle banned/suspended state during OAuth login too
                    if (errorString.contains("suspended", ignoreCase = true)) {
                        _uiState.value = LoginState.Error(parseBackendError(errorString, response.code()))
                    } else {
                        _uiState.value = LoginState.Error(parseBackendError(errorString, response.code()))
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }

    // Extracted the error parser to keep things DRY
    private fun parseBackendError(errorString: String, statusCode: Int): String {
        var displayMessage = "Request failed (Error $statusCode)"
        try {
            if (errorString.startsWith("{")) {
                val json = JSONObject(errorString)
                if (json.has("message")) {
                    displayMessage = json.getString("message")
                } else if (json.has("error")) {
                    displayMessage = json.getString("error")
                }
            } else if (errorString.isNotBlank()) {
                displayMessage = errorString
            }
        } catch (e: Exception) {
            if (errorString.isNotBlank()) displayMessage = errorString
        }
        return displayMessage
    }
}