package com.gamejoint.app.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToVerification: (String) -> Unit // NEW: Passes the email to the OTP screen!
) {
    val uiState by viewModel.uiState.collectAsState()

    // Form State
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var localError by remember { mutableStateOf<String?>(null) }

    // --- AUTO-NAVIGATION ON SUCCESS ---
    LaunchedEffect(uiState) {
        if (uiState is RegisterState.Success) {
            val registeredEmail = (uiState as RegisterState.Success).email
            onNavigateToVerification(registeredEmail)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create an Account", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; localError = null },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text("Email Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it; localError = null },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            placeholder = { Text("e.g. 2000-05-15") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; localError = null },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; localError = null },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        val displayError = localError ?: if (uiState is RegisterState.Error) {
            (uiState as RegisterState.Error).message
        } else null

        displayError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (uiState) {
            is RegisterState.Loading, is RegisterState.Success -> {
                // Keep the spinner spinning while we navigate away
                CircularProgressIndicator(color = Color(0xFF27AE60))
            }
            else -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                    onClick = {
                        if (username.isBlank() || email.isBlank() || password.isBlank() || dob.isBlank()) {
                            localError = "All fields are required."
                            return@Button
                        }
                        if (password != confirmPassword) {
                            localError = "Passwords do not match."
                            return@Button
                        }
                        if (password.length < 6) {
                            localError = "Password must be at least 6 characters."
                            return@Button
                        }
                        try {
                            val parsedDate = LocalDate.parse(dob)
                            val currentYear = LocalDate.now().year
                            if (parsedDate.year < 1900) {
                                localError = "Vampires are not allowed. Year must be after 1900."
                                return@Button
                            }
                            if (parsedDate.year >= currentYear) {
                                localError = "Time travelers are not allowed. Invalid birth year."
                                return@Button
                            }
                        } catch (e: DateTimeParseException) {
                            localError = "Invalid Date Format. Please use YYYY-MM-DD exactly."
                            return@Button
                        }

                        localError = null
                        viewModel.register(username, email, password, dob)
                    }
                ) {
                    Text("Register Now")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Already have an account? Login here",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onNavigateToLogin() }
        )
    }
}