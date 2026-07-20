package com.gamejoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamejoint.app.data.network.ApiClient
import com.gamejoint.app.ui.MainScaffold
import com.gamejoint.app.ui.auth.ForgotScreen
import com.gamejoint.app.ui.auth.LoginScreen
import com.gamejoint.app.ui.auth.RegisterScreen
import com.gamejoint.app.ui.home.HomeScreen
import com.gamejoint.app.ui.theme.Gamejoint_appTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize the strict Coil Cache architecture
        val imageLoader = coil.ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.10) // 10% of available RAM
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB limit
                    .build()
            }
            .crossfade(true)
            .build()

        // 2. Set it globally
        coil.Coil.setImageLoader(imageLoader)

        setContent {
            Gamejoint_appTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameJointNavigationApp()
                }
            }
        }
    }
}

@Composable
fun GameJointNavigationApp() {
    // 1. Keep track of whether Retrofit is built yet
    var isApiReady by remember { mutableStateOf(false) }

    // Grab the context and create the SessionManager
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.gamejoint.app.data.local.SessionManager(context) }

    // Coroutine Scope for the Logout button
    val coroutineScope = rememberCoroutineScope()

    // Continuously observe the token from DataStore
    val currentToken by sessionManager.jwtTokenFlow.collectAsState(initial = null)
    val isUserLoggedIn = !currentToken.isNullOrEmpty()

    // 2. This Coroutine runs exactly once when the app opens
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val gistUrl = "https://gist.githubusercontent.com/semihglsvn/2a19ca1c724e0af67545b22c78f4a9dc/raw/gamejoint_config.txt"

                val request = Request.Builder().url(gistUrl).build()
                val response = OkHttpClient().newCall(request).execute()

                val fetchedUrl = response.body?.string()?.trim()
                if (!fetchedUrl.isNullOrEmpty() && fetchedUrl.startsWith("http")) {
                    ApiClient.initialize(fetchedUrl, sessionManager)
                } else {
                    ApiClient.initialize("http://10.0.2.2:8080/", sessionManager)
                }
            } catch (e: Exception) {
                ApiClient.initialize("http://10.0.2.2:8080/", sessionManager)
            }
            isApiReady = true
        }
    }

    // 3. The UI Logic
    if (!isApiReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        // The API is ready!
        val navController = rememberNavController()

        // 4. Wrap the entire App inside the MainScaffold
        MainScaffold(
            isLoggedIn = isUserLoggedIn, // Dynamically reacts to the DataStore flow!
            onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToLogin = { navController.navigate("login") },
            onNavigateToRegister = { navController.navigate("register") },
            onNavigateToProfile = { /* TODO */ },

            onLogout = {
                coroutineScope.launch {
                    sessionManager.clearSession()
                    // Kick them back to home upon logout
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
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

            // 5. The NavHost sits inside the Scaffold
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
                        androidx.navigation.navArgument("email") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("isPasswordReset") { type = androidx.navigation.NavType.BoolType }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    val isPasswordReset = backStackEntry.arguments?.getBoolean("isPasswordReset") ?: false

                    com.gamejoint.app.ui.auth.VerificationScreen(
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
                        androidx.navigation.navArgument("email") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("otpCode") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    val otpCode = backStackEntry.arguments?.getString("otpCode") ?: ""

                    com.gamejoint.app.ui.auth.NewPasswordScreen(
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
                    arguments = listOf(androidx.navigation.navArgument("query") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    })
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query") ?: ""
                    com.gamejoint.app.ui.search.SearchScreen(
                        initialQuery = query,
                        onGameClick = { gameId ->
                            navController.navigate("gameDetails/$gameId")
                        }
                    )
                }

                composable(
                    route = "gameDetails/{gameId}",
                    arguments = listOf(androidx.navigation.navArgument("gameId") {
                        type = androidx.navigation.NavType.LongType
                    })
                ) { backStackEntry ->
                    val gameId = backStackEntry.arguments?.getLong("gameId") ?: 0L
                    com.gamejoint.app.ui.game.GameDetailScreen(gameId = gameId)
                }

                composable("settings") {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Settings Coming Soon...", color = Color.White)
                    }
                }
            }
        }
    }
}