package com.gamejoint.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.OAuthLoginRequest
import com.gamejoint.app.data.model.UserRegistrationRequest
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val email: String) : RegisterState()
    data class OAuthSuccess(val token: String) : RegisterState()
    data class OAuthIncomplete(val email: String, val providerToken: String) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    // --- NEW: Required to prevent the Android back-button navigation loop trap ---
    fun resetState() {
        _uiState.value = RegisterState.Idle
    }

    fun register(username: String, email: String, pass: String, dobString: String) {
        _uiState.value = RegisterState.Loading

        viewModelScope.launch {
            try {
                val parsedDob = try {
                    LocalDate.parse(dobString)
                } catch (e: DateTimeParseException) {
                    _uiState.value = RegisterState.Error("Invalid Date Format. Please use YYYY-MM-DD.")
                    return@launch
                }

                val request = UserRegistrationRequest(
                    username = username,
                    email = email,
                    password = pass,
                    dob = parsedDob.toString(),
                    cfTurnstileResponse = "mobile-bypass"
                )

                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.register(request).execute()
                }

                if (response.isSuccessful) {
                    _uiState.value = RegisterState.Success(email)
                } else {
                    val errorBodyString = response.errorBody()?.string() ?: ""
                    val errorMessage = parseBackendError(errorBodyString, response.code())
                    _uiState.value = RegisterState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = RegisterState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }

    fun oauthRegister(provider: String, token: String) {
        _uiState.value = RegisterState.Loading

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
                            _uiState.value = RegisterState.OAuthIncomplete(
                                email = authResponse.email ?: "",
                                providerToken = token
                            )
                        } else {
                            _uiState.value = RegisterState.OAuthSuccess(authResponse.jwtToken ?: "")
                        }
                    } else {
                        _uiState.value = RegisterState.Error("Server returned an empty response.")
                    }
                } else {
                    val errorString = response.errorBody()?.string() ?: ""
                    _uiState.value = RegisterState.Error(parseBackendError(errorString, response.code()))
                }
            } catch (e: Exception) {
                _uiState.value = RegisterState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }

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