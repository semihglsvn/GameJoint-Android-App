package com.gamejoint.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ForgotState {
    object Idle : ForgotState()
    object Loading : ForgotState()
    data class Success(val email: String) : ForgotState()
    data class Error(val message: String) : ForgotState()
}

class ForgotViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotState>(ForgotState.Idle)
    val uiState: StateFlow<ForgotState> = _uiState.asStateFlow()

    fun requestPasswordReset(email: String) {
        _uiState.value = ForgotState.Loading

        viewModelScope.launch {
            try {
                val requestBody = mapOf("email" to email)

                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.forgotPassword(requestBody).execute()
                }

                if (response.isSuccessful) {
                    // Pass the email into the success state
                    _uiState.value = ForgotState.Success(email)
                } else {
                    _uiState.value = ForgotState.Error("Reset request failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = ForgotState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }
}