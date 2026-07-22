package com.gamejoint.app.ui.home

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.local.SessionManager
import com.gamejoint.app.data.model.FeaturedGameResponse
import com.gamejoint.app.data.model.GameSummary
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class HomeData(
    val featured: List<FeaturedGameResponse> = emptyList(),
    val trending: List<GameSummary> = emptyList(),
    val newReleases: List<GameSummary> = emptyList(),
    val topRated: List<GameSummary> = emptyList()
)

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val data: HomeData) : HomeState()
    data class Error(val message: String) : HomeState()
}

// CHANGED: Now an AndroidViewModel so we can access DataStore!
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<HomeState>(HomeState.Loading)
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    // --- BAN POPUP STATES ---
    val showBanPopup = MutableStateFlow(false)
    val banExpiration = MutableStateFlow<String?>("Permanent")
    private var hasShownPopupThisSession = false // Prevents spamming the user every time they go home

    init {
        fetchHomeData()
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.jwtTokenFlow.collect { token ->
                if (!token.isNullOrEmpty() && !hasShownPopupThisSession) {
                    checkIfBanned(token)
                }
            }
        }
    }

    private fun checkIfBanned(token: String) {
        try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
                val json = JSONObject(payloadJson)

                if (json.has("isBanned") && json.getBoolean("isBanned")) {
                    // Try to grab the expiration date if your backend provides it in the token!
                    if (json.has("banExpiration")) {
                        banExpiration.value = json.getString("banExpiration")
                    } else {
                        banExpiration.value = "Permanent"
                    }
                    showBanPopup.value = true
                    hasShownPopupThisSession = true
                }
            }
        } catch (e: Exception) {
            Log.e("AUTH", "Failed to parse token for ban status", e)
        }
    }

    fun dismissBanPopup() {
        showBanPopup.value = false
    }

     fun fetchHomeData() {
        _uiState.value = HomeState.Loading

        viewModelScope.launch {
            try {
                val featuredDef = async(Dispatchers.IO) {
                    try { ApiClient.gameService.getFeaturedGames().execute().body() ?: emptyList() } catch (e: Exception) { emptyList() }
                }
                val trendingDef = async(Dispatchers.IO) {
                    try { ApiClient.gameService.getTrendingGames(0, 15).execute().body()?.content ?: emptyList() } catch (e: Exception) { emptyList() }
                }
                val newReleasesDef = async(Dispatchers.IO) {
                    try { ApiClient.gameService.getNewReleases(0, 15).execute().body()?.content ?: emptyList() } catch (e: Exception) { emptyList() }
                }
                val topRatedDef = async(Dispatchers.IO) {
                    try { ApiClient.gameService.getTopRatedGames(0, 15).execute().body()?.content ?: emptyList() } catch (e: Exception) { emptyList() }
                }

                val data = HomeData(
                    featured = featuredDef.await(),
                    trending = trendingDef.await(),
                    newReleases = newReleasesDef.await(),
                    topRated = topRatedDef.await()
                )

                if (data.featured.isEmpty() && data.trending.isEmpty() && data.newReleases.isEmpty() && data.topRated.isEmpty()) {
                    _uiState.value = HomeState.Error("Could not connect to the game server.")
                } else {
                    _uiState.value = HomeState.Success(data)
                }

            } catch (e: Exception) {
                _uiState.value = HomeState.Error("Critical Error: ${e.localizedMessage}")
            }
        }
    }
}