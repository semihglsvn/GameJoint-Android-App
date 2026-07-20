package com.gamejoint.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NewPasswordScreen(
    email: String,
    otpCode: String,
    viewModel: NewPasswordViewModel = viewModel(),
    onNavigateToLogin: () -> Unit // Called when reset is completely successful
) {
    val uiState by viewModel.uiState.collectAsState()

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // --- AUTO-NAVIGATION ON SUCCESS ---
    LaunchedEffect(uiState) {
        if (uiState is NewPasswordState.Success) {
            // Give them a split second to see the success state if you want, or immediately navigate
            onNavigateToLogin()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create New Password", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your identity has been verified. Please enter a strong new password below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; localError = null },
            label = { Text("New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; localError = null },
            label = { Text("Confirm New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- ERROR DISPLAY ---
        val displayError = localError ?: if (uiState is NewPasswordState.Error) {
            (uiState as NewPasswordState.Error).message
        } else null

        displayError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- BUTTON LOGIC ---
        when (uiState) {
            is NewPasswordState.Loading -> {
                CircularProgressIndicator(color = Color(0xFF27AE60))
            }
            is NewPasswordState.Success -> {
                Text(
                    text = "Password reset successfully!",
                    color = Color(0xFF27AE60)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onNavigateToLogin) {
                    Text("Return to Login")
                }
            }
            else -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                    onClick = {
                        // Frontend Validation Gauntlet
                        if (newPassword.isBlank() || confirmPassword.isBlank()) {
                            localError = "Please fill out all fields."
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            localError = "Password must be at least 6 characters."
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            localError = "Passwords do not match."
                            return@Button
                        }

                        // If it passes, execute the reset!
                        localError = null
                        viewModel.resetPassword(email = email, otp = otpCode, newPassword = newPassword)
                    }
                ) {
                    Text("Save & Login")
                }
            }
        }
    }
}