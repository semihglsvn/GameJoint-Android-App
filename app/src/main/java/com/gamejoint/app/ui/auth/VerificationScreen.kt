package com.gamejoint.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VerificationScreen(
    email: String,
    isPasswordReset: Boolean = false,
    viewModel: VerificationViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToNewPassword: (String, String) -> Unit // Passes (email, otp) for the final reset step
) {
    val uiState by viewModel.uiState.collectAsState()

    // We store the 6 digits as a single string, but draw it as 6 boxes
    var otpValue by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // Auto-fire the network request the moment they type the 6th digit
    LaunchedEffect(otpValue) {
        if (otpValue.length == 6) {
            localError = null
            if (isPasswordReset) {
                viewModel.validateResetCode(otpValue)
            } else {
                viewModel.verifyAccount(email, otpValue)
            }
        }
    }

    // Handle Backend Success States
    LaunchedEffect(uiState) {
        when (uiState) {
            is VerifyState.Success -> onNavigateToLogin() // Account verified! Back to login.
            is VerifyState.CodeValid -> {
                // Password reset code is valid length, push them to the new password screen!
                val validOtp = (uiState as VerifyState.CodeValid).verifiedOtp
                onNavigateToNewPassword(email, validOtp)
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isPasswordReset) "Reset Password" else "Verify Account",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We sent a 6-digit code to:\n$email",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- THE 6 OTP BOXES ---
        BasicTextField(
            value = otpValue,
            onValueChange = {
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    otpValue = it
                    if (uiState is VerifyState.Error) {
                        localError = null // Clear error when they start typing again
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        val char = when {
                            index >= otpValue.length -> ""
                            else -> otpValue[index].toString()
                        }

                        val isFocused = index == otpValue.length
                        val borderColor = if (uiState is VerifyState.Error) MaterialTheme.colorScheme.error
                        else if (isFocused) Color(0xFF27AE60)
                        else Color.Gray

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error Handling
        val displayError = localError ?: if (uiState is VerifyState.Error) {
            (uiState as VerifyState.Error).message
        } else null

        displayError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState is VerifyState.Loading) {
            CircularProgressIndicator(color = Color(0xFF27AE60))
        } else {
            // Resend Code Button
            Text(
                text = "Didn't receive a code? Resend",
                color = Color(0xFF27AE60),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { viewModel.resendCode(email, isPasswordReset) }
            )
        }
    }
}