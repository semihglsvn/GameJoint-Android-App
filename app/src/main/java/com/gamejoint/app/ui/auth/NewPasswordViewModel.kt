package com.gamejoint.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.OtpPasswordResetRequest
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class NewPasswordState {
    object Idle : NewPasswordState()
    object Loading : NewPasswordState()
    object Success : NewPasswordState()
    data class Error(val message: String) : NewPasswordState()
}

class NewPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NewPasswordState>(NewPasswordState.Idle)
    val uiState: StateFlow<NewPasswordState> = _uiState.asStateFlow()

    fun resetPassword(email: String, otp: String, newPassword: String) {
        _uiState.value = NewPasswordState.Loading

        viewModelScope.launch {
            try {
                val request = OtpPasswordResetRequest(
                    email = email,
                    otp = otp,
                    newPassword = newPassword
                )

                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.resetPasswordOtp(request).execute()
                }

                if (response.isSuccessful) {
                    _uiState.value = NewPasswordState.Success
                } else {
                    _uiState.value = NewPasswordState.Error("Failed to reset password. The session may have expired.")
                }
            } catch (e: Exception) {
                _uiState.value = NewPasswordState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }
}