package com.gamejoint.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamejoint.app.data.local.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToVerification: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOAuthComplete: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val uriHandler = LocalUriHandler.current // NEW: To open the web link

    // Form State
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    // NEW: Terms Checkbox state
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    // Calendar Dialog State
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // --- LUMINANCE DETECTION ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val appBgColor = if (isLightMode) MaterialTheme.colorScheme.background else Color(0xFF181818)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    // --- AUTO-NAVIGATION ON SUCCESS ---
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is RegisterState.Success -> {
                onNavigateToVerification(state.email)
            }
            is RegisterState.OAuthSuccess -> {
                sessionManager.saveToken(state.token)
                onNavigateToHome()
            }
            is RegisterState.OAuthIncomplete -> {
                onNavigateToOAuthComplete(state.email, state.providerToken)
            }
            else -> {}
        }
    }

    // --- MATERIAL 3 DATE PICKER DIALOG ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC")) // Fixed Timezone Bug
                                .toLocalDate()
                            dob = selectedDate.toString()
                            localError = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = Color(0xFF27AE60))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor)
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create an Account",
            style = MaterialTheme.typography.headlineMedium,
            color = primaryTextColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; localError = null },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = primaryTextColor, unfocusedTextColor = primaryTextColor,
                focusedLabelColor = Color(0xFF27AE60), unfocusedLabelColor = secondaryTextColor,
                focusedBorderColor = Color(0xFF27AE60), unfocusedBorderColor = secondaryTextColor,
                cursorColor = Color(0xFF27AE60)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text("Email Address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = primaryTextColor, unfocusedTextColor = primaryTextColor,
                focusedLabelColor = Color(0xFF27AE60), unfocusedLabelColor = secondaryTextColor,
                focusedBorderColor = Color(0xFF27AE60), unfocusedBorderColor = secondaryTextColor,
                cursorColor = Color(0xFF27AE60)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date of Birth") },
            placeholder = { Text("Tap to select from calendar") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = Color(0xFF27AE60))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = primaryTextColor, disabledBorderColor = secondaryTextColor,
                disabledLabelColor = secondaryTextColor, disabledPlaceholderColor = secondaryTextColor,
                disabledTrailingIconColor = Color(0xFF27AE60)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; localError = null },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = primaryTextColor, unfocusedTextColor = primaryTextColor,
                focusedLabelColor = Color(0xFF27AE60), unfocusedLabelColor = secondaryTextColor,
                focusedBorderColor = Color(0xFF27AE60), unfocusedBorderColor = secondaryTextColor,
                cursorColor = Color(0xFF27AE60)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; localError = null },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = primaryTextColor, unfocusedTextColor = primaryTextColor,
                focusedLabelColor = Color(0xFF27AE60), unfocusedLabelColor = secondaryTextColor,
                focusedBorderColor = Color(0xFF27AE60), unfocusedBorderColor = secondaryTextColor,
                cursorColor = Color(0xFF27AE60)
            )
        )

        // --- NEW: Terms & Privacy Checkbox ---
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it; localError = null },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF27AE60), uncheckedColor = secondaryTextColor)
            )
            Text(text = "I agree to the ", color = primaryTextColor, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Privacy Policy",
                color = Color(0xFF27AE60),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable {
                    // Open your web link here
                    uriHandler.openUri("https://game-joint.net/privacy")
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val displayError = localError ?: if (uiState is RegisterState.Error) {
            (uiState as RegisterState.Error).message
        } else null

        displayError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (uiState) {
            is RegisterState.Loading, is RegisterState.Success, is RegisterState.OAuthSuccess, is RegisterState.OAuthIncomplete -> {
                CircularProgressIndicator(color = Color(0xFF27AE60))
            }
            else -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                    onClick = {
                        // Check terms first
                        if (!acceptedTerms) {
                            localError = "You must agree to the Privacy Policy to register."
                            return@Button
                        }
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
                            localError = "Please select a valid date from the calendar."
                            return@Button
                        }

                        localError = null
                        viewModel.register(username.trim(), email.trim(), password, dob)
                    }
                ) {
                    Text("Register Now", color = Color.White)
                }

                // --- GOOGLE SIGN-UP BUTTON ---
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "OR", color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLightMode) Color(0xFFF2F2F2) else Color(0xFF333333),
                        contentColor = primaryTextColor
                    ),
                    onClick = {
                        // Check terms for Google too!
                        if (!acceptedTerms) {
                            localError = "You must agree to the Privacy Policy to continue with Google."
                            return@Button
                        }

                        coroutineScope.launch {
                            localError = null
                            val token = GoogleAuthHelper.signInWithGoogle(context)
                            if (token != null) {
                                viewModel.oauthRegister("GOOGLE", token)
                            } else {
                                localError = "Google Sign-In was cancelled or failed."
                            }
                        }
                    }
                ) {
                    Text("Continue with Google")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Already have an account? Login here",
            color = Color(0xFF27AE60),
            modifier = Modifier.clickable { onNavigateToLogin() }
        )
    }
}