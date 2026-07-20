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

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()

    // UPDATE: Success now holds the token to be saved by the UI layer
    data class Success(val token: String) : LoginState()

    // NEW: If the API blocks them for being unverified, we capture the email
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
                    cfTurnstileResponse = "mobile-bypass" // Use the Turnstile bypass!
                )

                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.login(request).execute()
                }

                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    val token = tokenResponse?.token
                    if (token != null) {
                        // Pass the token to the UI so DataStore can save it
                        _uiState.value = LoginState.Success(token)
                    } else {
                        _uiState.value = LoginState.Error("Server returned empty token.")
                    }
                } else {
                    // Check if the backend specifically blocked them for being unverified
                    val errorBody = response.errorBody()?.string() ?: ""
                    if (errorBody.contains("not verified", ignoreCase = true)) {
                        _uiState.value = LoginState.Unverified(usernameOrEmail)
                    } else {
                        _uiState.value = LoginState.Error("Login failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }
}