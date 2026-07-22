package com.gamejoint.app.ui.settings

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.SettingsHelper
import com.gamejoint.app.data.model.AccountDeleteRequest
import com.gamejoint.app.data.model.EmailChangeRequest
import com.gamejoint.app.data.model.PasswordChangeRequest
import com.gamejoint.app.data.model.UserProfileResponse
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(SettingsHelper.PREFS_NAME, Context.MODE_PRIVATE)

    // --- API STATES ---
    private val _profileData = MutableStateFlow<UserProfileResponse?>(null)
    val profileData = _profileData.asStateFlow()

    val isLoading = MutableStateFlow(true)
    val isActionLoading = MutableStateFlow(false)
    val otpSent = MutableStateFlow(false)

    private val _feedbackMessage = MutableSharedFlow<String>()
    val feedbackMessage = _feedbackMessage.asSharedFlow()

    // --- LOCAL PREFERENCE STATES ---
    val currentTheme = MutableStateFlow(prefs.getInt(SettingsHelper.KEY_THEME, 0))
    val currentCacheMb = MutableStateFlow(prefs.getLong(SettingsHelper.KEY_CACHE_MB, 50L))

    init {
        loadProfile()
    }

    // ==========================================
    // LOCAL SETTINGS LOGIC
    // ==========================================

    fun updateTheme(themeId: Int) {
        currentTheme.value = themeId
        prefs.edit { putInt(SettingsHelper.KEY_THEME, themeId) }
    }

    fun updateCacheSize(megabytes: Long) {
        currentCacheMb.value = megabytes
        prefs.edit { putLong(SettingsHelper.KEY_CACHE_MB, megabytes) }
    }

    // ==========================================
    // NETWORK LOGIC
    // ==========================================

    fun loadProfile() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) { ApiClient.userService.getProfile().execute() }
                if (response.isSuccessful) {
                    _profileData.value = response.body()
                } else {
                    _feedbackMessage.emit("Failed to load profile data.")
                }
            } catch (e: Exception) {
                _feedbackMessage.emit("Network Error.")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun requestOtp() {
        viewModelScope.launch {
            isActionLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) { ApiClient.userService.requestSettingsOtp().execute() }
                if (response.isSuccessful) {
                    otpSent.value = true
                    _feedbackMessage.emit("Security code sent to your email!")
                } else {
                    val err = response.errorBody()?.string() ?: "Failed to send code."
                    if (err.contains("Too many")) {
                        _feedbackMessage.emit("Please wait a few minutes before requesting another code.")
                    } else {
                        _feedbackMessage.emit("Action blocked by server.")
                    }
                }
            } catch (e: Exception) {
                _feedbackMessage.emit("Network Error. Check your connection.")
            } finally {
                isActionLoading.value = false
            }
        }
    }

    // Provide a callback (onSuccess) so the UI knows when to force a logout
    fun changeEmail(otp: String, newEmail: String, onSuccess: () -> Unit) {
        if (otp.isBlank() || newEmail.isBlank()) return
        executeSecureAction(onSuccess) {
            ApiClient.userService.changeEmail(EmailChangeRequest(otpCode = otp, newEmail = newEmail)).execute()
        }
    }

    fun changePassword(otp: String, newPassword: String, onSuccess: () -> Unit) {
        if (otp.isBlank() || newPassword.isBlank()) return
        executeSecureAction(onSuccess) {
            ApiClient.userService.changePassword(PasswordChangeRequest(otpCode = otp, newPassword = newPassword)).execute()
        }
    }

    fun deleteAccount(otp: String, onSuccess: () -> Unit) {
        if (otp.isBlank()) return
        executeSecureAction(onSuccess) {
            ApiClient.userService.deleteAccount(AccountDeleteRequest(otpCode = otp)).execute()
        }
    }

    private fun executeSecureAction(onSuccess: () -> Unit, apiCall: () -> retrofit2.Response<Map<String, String>>) {
        viewModelScope.launch {
            isActionLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) { apiCall() }
                if (response.isSuccessful) {
                    _feedbackMessage.emit(response.body()?.get("message") ?: "Success!")
                    otpSent.value = false // Reset OTP state
                    onSuccess()
                } else {
                    val err = response.errorBody()?.string() ?: "Action failed."
                    if (err.contains("Invalid")) {
                        _feedbackMessage.emit("Invalid or expired OTP code.")
                    } else {
                        _feedbackMessage.emit(err)
                    }
                }
            } catch (e: Exception) {
                _feedbackMessage.emit("Network Error.")
            } finally {
                isActionLoading.value = false
            }
        }
    }

    fun resetOtpState() {
        otpSent.value = false
    }
}