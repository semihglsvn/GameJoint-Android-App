package com.gamejoint.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()

    // UPDATE: Success now holds the email so we can instantly pass it to the OTP UI!
    data class Success(val email: String) : RegisterState()

    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

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
                    // We can leave this as a dummy string, our AuthInterceptor bypasses Turnstile completely!
                    cfTurnstileResponse = "mobile-bypass"
                )

                val response = withContext(Dispatchers.IO) {
                    ApiClient.authService.register(request).execute()
                }

                if (response.isSuccessful) {
                    _uiState.value = RegisterState.Success(email)
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    val errorMessage = try {
                        // Parse the JSON message sent by Spring Boot's GlobalExceptionHandler
                        val jsonObject = Gson().fromJson(errorBodyString, JsonObject::class.java)
                        jsonObject.get("message")?.asString ?: "Registration failed (${response.code()})"
                    } catch (e: Exception) {
                        "Registration failed: ${response.code()}"
                    }

                    _uiState.value = RegisterState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = RegisterState.Error("Network Error: ${e.localizedMessage}")
            }
        }
    }
}