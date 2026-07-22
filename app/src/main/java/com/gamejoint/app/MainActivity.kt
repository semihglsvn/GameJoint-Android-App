package com.gamejoint.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gamejoint.app.data.network.ApiClient
import com.gamejoint.app.ui.MainScaffold
import com.gamejoint.app.ui.auth.ForgotScreen
import com.gamejoint.app.ui.auth.LoginScreen
import com.gamejoint.app.ui.auth.NewPasswordScreen
import com.gamejoint.app.ui.auth.RegisterScreen
import com.gamejoint.app.ui.auth.VerificationScreen
import com.gamejoint.app.ui.game.GameDetailScreen
import com.gamejoint.app.ui.home.HomeScreen
import com.gamejoint.app.ui.search.SearchScreen
import com.gamejoint.app.ui.settings.SettingsScreen
import com.gamejoint.app.ui.theme.Gamejoint_appTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

// A simple helper to manage our settings across the app
object SettingsHelper {
    const val PREFS_NAME = "gamejoint_settings"
    const val KEY_CACHE_MB = "disk_cache_mb"
    const val KEY_THEME = "app_theme" // 0 = System, 1 = Light, 2 = Dark
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Read settings from SharedPreferences
        val prefs = getSharedPreferences(SettingsHelper.PREFS_NAME, Context.MODE_PRIVATE)
        val cacheSizeMb = prefs.getLong(SettingsHelper.KEY_CACHE_MB, 50L) // Default 50MB!

        // 2. Initialize the strict Coil Cache architecture with the dynamic variable
        val imageLoader = coil.ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(cacheSizeMb * 1024 * 1024) // Dynamic size here!
                    .build()
            }
            .crossfade(true)
            .build()

        try {
            coil.Coil.setImageLoader(imageLoader)
        } catch (e: Exception) {
        }

        setContent {
            // Theme observer so it updates instantly when changed in Settings
            var themeMode by remember { mutableIntStateOf(prefs.getInt(SettingsHelper.KEY_THEME, 0)) }

            val isDark = when (themeMode) {
                1 -> false // Force Light
                2 -> true  // Force Dark
                else -> isSystemInDarkTheme() // System Default
            }

            Gamejoint_appTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameJointNavigationApp(
                        onThemeChanged = { newTheme -> themeMode = newTheme }
                    )
                }
            }
        }
    }
}

@Composable
fun GameJointNavigationApp(onThemeChanged: (Int) -> Unit) {
    var isApiReady by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.gamejoint.app.data.local.SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val currentToken by sessionManager.jwtTokenFlow.collectAsState(initial = null)
    val isUserLoggedIn = !currentToken.isNullOrEmpty()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var attempts = 0
            var success = false
            var fetchedUrl = ""

            // Gist retry loop to survive initial wake-up lag
            while (attempts < 5 && !success) {
                try {
                    val gistUrl = "https://gist.githubusercontent.com/semihglsvn/2a19ca1c724e0af67545b22c78f4a9dc/raw/gamejoint_config.txt"
                    val request = Request.Builder().url(gistUrl).build()
                    val response = OkHttpClient().newCall(request).execute()

                    val responseBody = response.body?.string()?.trim()
                    if (!responseBody.isNullOrEmpty() && responseBody.startsWith("http")) {
                        fetchedUrl = responseBody
                        success = true
                    }
                } catch (e: Exception) {
                    attempts++
                    delay(2000)
                }
            }

            val finalUrl = if (success) fetchedUrl else "http://localhost/"
            ApiClient.initialize(finalUrl, sessionManager)

            isApiReady = true
        }
    }

    if (!isApiReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val navController = rememberNavController()

        MainScaffold(
            isLoggedIn = isUserLoggedIn,
            onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToLogin = { navController.navigate("login") },
            onNavigateToRegister = { navController.navigate("register") },
            onNavigateToProfile = { /* TODO */ },
            onLogout = {
                coroutineScope.launch {
                    sessionManager.clearSession()
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            },
            onNavigateToSettings = {
                navController.navigate("settings")
            },
            onSearchSubmit = { query ->
                navController.navigate("search?query=$query")
            }
        ) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("login") {
                    LoginScreen(
                        onNavigateToHome = {
                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                        },
                        onNavigateToRegister = { navController.navigate("register") },
                        onNavigateToForgot = { navController.navigate("forgot") },
                        onNavigateToVerify = { email ->
                            navController.navigate("verify/$email/false")
                        }
                    )
                }

                composable("register") {
                    RegisterScreen(
                        onNavigateToLogin = { navController.popBackStack() },
                        onNavigateToVerification = { email ->
                            navController.navigate("verify/$email/false")
                        }
                    )
                }

                composable("forgot") {
                    ForgotScreen(
                        onNavigateBackToLogin = { navController.popBackStack() },
                        onNavigateToVerification = { email ->
                            navController.navigate("verify/$email/true")
                        }
                    )
                }

                composable(
                    route = "verify/{email}/{isPasswordReset}",
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("isPasswordReset") { type = NavType.BoolType }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    val isPasswordReset = backStackEntry.arguments?.getBoolean("isPasswordReset") ?: false

                    VerificationScreen(
                        email = email,
                        isPasswordReset = isPasswordReset,
                        onNavigateToLogin = {
                            navController.navigate("login") { popUpTo("login") { inclusive = true } }
                        },
                        onNavigateToNewPassword = { passedEmail, otpCode ->
                            navController.navigate("reset_password/$passedEmail/$otpCode")
                        }
                    )
                }

                composable(
                    route = "reset_password/{email}/{otpCode}",
                    arguments = listOf(
                        navArgument("email") { type = NavType.StringType },
                        navArgument("otpCode") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    val otpCode = backStackEntry.arguments?.getString("otpCode") ?: ""

                    NewPasswordScreen(
                        email = email,
                        otpCode = otpCode,
                        onNavigateToLogin = {
                            navController.navigate("login") { popUpTo("login") { inclusive = true } }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        onGameClick = { gameId ->
                            navController.navigate("gameDetails/$gameId")
                        }
                    )
                }

                composable(
                    route = "search?query={query}",
                    arguments = listOf(navArgument("query") {
                        type = NavType.StringType
                        defaultValue = ""
                    })
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query") ?: ""
                    SearchScreen(
                        initialQuery = query,
                        onGameClick = { gameId ->
                            navController.navigate("gameDetails/$gameId")
                        }
                    )
                }

                composable(
                    route = "gameDetails/{gameId}",
                    arguments = listOf(navArgument("gameId") {
                        type = NavType.LongType
                    })
                ) { backStackEntry ->
                    val gameId = backStackEntry.arguments?.getLong("gameId") ?: 0L
                    GameDetailScreen(gameId = gameId)
                }

                composable("settings") {
                    SettingsScreen(
                        onThemeChanged = onThemeChanged,
                        onLogout = {
                            coroutineScope.launch {
                                sessionManager.clearSession()
                                navController.navigate("home") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}