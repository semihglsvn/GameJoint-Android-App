package com.gamejoint.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamejoint.app.data.local.SessionManager
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthCompleteScreen(
    email: String, // Displayed to the user
    providerToken: String, // Kept in memory to send to the backend
    viewModel: OAuthCompleteViewModel = viewModel(),
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val uriHandler = LocalUriHandler.current // NEW: To open the web link

    // Form State (Upgraded to rememberSaveable)
    var username by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    // NEW: Terms Checkbox state
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

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
        if (uiState is OAuthCompleteState.Success) {
            sessionManager.saveToken((uiState as OAuthCompleteState.Success).token)
            onNavigateToHome()
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
            text = "Complete Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = primaryTextColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Signing in as $email",
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; localError = null },
            label = { Text("Choose a Username") },
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
                    uriHandler.openUri("https://game-joint.net/privacy")
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val displayError = localError ?: if (uiState is OAuthCompleteState.Error) {
            (uiState as OAuthCompleteState.Error).message
        } else null

        displayError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState is OAuthCompleteState.Loading || uiState is OAuthCompleteState.Success) {
            CircularProgressIndicator(color = Color(0xFF27AE60))
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                onClick = {
                    // Check terms first
                    if (!acceptedTerms) {
                        localError = "You must agree to the Privacy Policy to register."
                        return@Button
                    }
                    if (username.isBlank() || dob.isBlank()) {
                        localError = "Both fields are required."
                        return@Button
                    }
                    localError = null
                    viewModel.completeOAuth(
                        provider = "GOOGLE",
                        providerToken = providerToken,
                        username = username.trim(), // Automatically cleans trailing spaces
                        dob = dob
                    )
                }
            ) {
                Text("Finish Registration", color = Color.White)
            }
        }
    }
}