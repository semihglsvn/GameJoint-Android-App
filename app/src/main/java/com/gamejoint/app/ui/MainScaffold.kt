package com.gamejoint.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gamejoint.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val HeaderFooterDark = Color(0xFF1E1E1E)
val SearchBackground = Color(0xFF333333)

data class QuickSearchResult(
    val id: Long,
    val title: String,
    val imageUrl: String
)

@Composable
fun MainScaffold(
    isLoggedIn: Boolean,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit, // NEW: Logout Callback
    onNavigateToSettings: () -> Unit, // NEW: Setting up for your next feature!
    onSearchSubmit: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            GameJointHeader(
                isLoggedIn = isLoggedIn,
                onNavigateToHome = onNavigateToHome,
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToProfile = onNavigateToProfile,
                onLogout = onLogout,
                onNavigateToSettings = onNavigateToSettings,
                onSearchSubmit = onSearchSubmit
            )
        },
        containerColor = Color(0xFF181818) // Global app background
    ) { paddingValues ->
        content(paddingValues)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameJointHeader(
    isLoggedIn: Boolean,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSearchSubmit: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var asyncResults by remember { mutableStateOf<List<QuickSearchResult>>(emptyList()) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            delay(500)
            try {
                val response = withContext(Dispatchers.IO) {
                    com.gamejoint.app.data.network.ApiClient.gameService
                        .searchGames(q = searchQuery, page = 0, size = 3)
                        .execute()
                }

                if (response.isSuccessful) {
                    val games = response.body()?.content ?: emptyList()
                    asyncResults = games.map { game ->
                        QuickSearchResult(
                            id = game.id ?: 0L,
                            title = game.title ?: "Unknown",
                            imageUrl = game.coverImage ?: ""
                        )
                    }
                    isSearchExpanded = asyncResults.isNotEmpty()
                } else {
                    isSearchExpanded = false
                }
            } catch (e: Exception) {
                isSearchExpanded = false
            }
        } else {
            isSearchExpanded = false
            asyncResults = emptyList()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderFooterDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. THE LOGO
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "GameJoint Logo",
            modifier = Modifier
                .height(26.dp)
                .padding(end = 12.dp)
                .clickable { onNavigateToHome() }
        )

        // 2. THE SEARCH BAR
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(SearchBackground, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val queryToSubmit = searchQuery
                            searchQuery = ""
                            isSearchExpanded = false
                            onSearchSubmit(queryToSubmit)
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search...", color = Color.Gray, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Gray,
                    modifier = Modifier.clickable {
                        val queryToSubmit = searchQuery
                        searchQuery = ""
                        isSearchExpanded = false
                        onSearchSubmit(queryToSubmit)
                    }
                )
            }

            DropdownMenu(
                expanded = isSearchExpanded,
                onDismissRequest = { isSearchExpanded = false },
                modifier = Modifier
                    .background(SearchBackground)
                    .fillMaxWidth(0.6f)
                    .animateContentSize()
            ) {
                asyncResults.forEach { result ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = result.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(result.title, color = Color.White, fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            val queryToSubmit = result.title
                            searchQuery = ""
                            isSearchExpanded = false
                            onSearchSubmit(queryToSubmit)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. THE HAMBURGER MENU
        Box {
            IconButton(onClick = { isMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier.background(HeaderFooterDark)
            ) {
                if (isLoggedIn) {
                    DropdownMenuItem(
                        text = { Text("Profile", color = Color.White) },
                        onClick = { isMenuExpanded = false; onNavigateToProfile() }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = Color.White) },
                        onClick = { isMenuExpanded = false; onNavigateToSettings() }
                    )
                    // NEW: Highlight the logout button in red!
                    DropdownMenuItem(
                        text = { Text("Logout", color = Color(0xFFE74C3C)) },
                        onClick = {
                            isMenuExpanded = false
                            onLogout()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Login", color = Color.White) },
                        onClick = { isMenuExpanded = false; onNavigateToLogin() }
                    )
                    DropdownMenuItem(
                        text = { Text("Register", color = Color.White) },
                        onClick = { isMenuExpanded = false; onNavigateToRegister() }
                    )
                }
            }
        }
    }
}