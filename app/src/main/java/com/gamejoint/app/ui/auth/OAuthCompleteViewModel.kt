package com.gamejoint.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.OAuthRegistrationCompleteRequest
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class OAuthCompleteState {
    object Idle : OAuthCompleteState()
    object Loading : OAuthCompleteState()
    data class Success(val token: String) : OAuthCompleteState()
    data class Error(val message: String) : OAuthCompleteState()
}

class OAuthCompleteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<OAuthCompleteState>(OAuthCompleteState.Idle)
    val uiState: StateFlow<OAuthCompleteState> = _uiState.asStateFlow()

    fun completeOAuth(provider: String, providerToken: String, username: String, dob: String) {
        _uiState.value = OAuthCompleteState.Loading

        viewModelScope.launch {
            try {
                val request = OAuthRegistrationCompleteRequest(
                    provider = provider,
                    providerToken = providerToken,
                    username = username,
                    dob = dob,
                    cfTurnstileResponse = "mobile-bypass"
                )

                val response = withContext(Dispatchers.IO) {
                    // Make sure authService points to AuthControllerApi
                    ApiClient.authService.completeOAuthRegistration(request).execute()
                }

                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        _uiState.value = OAuthCompleteState.Success(token)
                    } else {
                        _uiState.value = OAuthCompleteState.Error("Server returned an empty token.")
                    }
                } else {
                    val errorString = response.errorBody()?.string() ?: ""
                    _uiState.value = OAuthCompleteState.Error(parseBackendError(errorString, response.code()))
                }
            } catch (e: Exception) {
                _uiState.value = OAuthCompleteState.Error("Network Error: ${e.localizedMessage}")
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