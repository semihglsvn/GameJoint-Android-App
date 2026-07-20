package com.gamejoint.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.OtpVerifyRequest
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class VerifyState {
    object Idle : VerifyState()
    object Loading : VerifyState()
    object Success : VerifyState() // For Account Verification
    data class CodeValid(val verifiedOtp: String) : VerifyState() // For Password Reset (unlocks new password fields)
    data class Error(val message: String) : VerifyState()
}

class VerificationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<VerifyState>(VerifyState.Idle)
    val uiState: StateFlow<VerifyState> = _uiState.asStateFlow()

    fun verifyAccount(email: String, otp: String) {
        _uiState.value = VerifyState.Loading

        viewModelScope.launch {
            try {
                val request = OtpVerifyRequest(identifier = email, otp = otp)

                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.verifyAccountOtp(request).execute()
                }

                if (response.isSuccessful) {
                    _uiState.value = VerifyState.Success
                } else {
                    // Try to grab the error message from the backend, otherwise show default
                    _uiState.value = VerifyState.Error("Invalid or expired code. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = VerifyState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }

    // For password resets, we don't hit the backend until they type the NEW password.
    // So when they type 6 digits, we just validate the length and flip the state.
    fun validateResetCode(otp: String) {
        if (otp.length == 6) {
            _uiState.value = VerifyState.CodeValid(otp)
        } else {
            _uiState.value = VerifyState.Error("Code must be 6 digits.")
        }
    }

    fun resendCode(email: String, isPasswordReset: Boolean) {
        viewModelScope.launch {
            try {
                val body = mapOf(if (isPasswordReset) "email" to email else "identifier" to email)

                withContext(Dispatchers.IO) {
                    if (isPasswordReset) {
                        ApiClient.authService.forgotPassword(body).execute()
                    } else {
                        ApiClient.authService.resendVerification(body).execute()
                    }
                }
                // We don't change state here to avoid interrupting the user,
                // but a toast/snackbar in the UI could say "Sent!"
            } catch (e: Exception) {
                // Silently fail resends to prevent UI jarring, or log it
            }
        }
    }
}