package com.gamejoint.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.FeaturedGameResponse
import com.gamejoint.app.data.model.GameSummary
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

// 1. The State Object holds all 4 rows of data
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

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeState>(HomeState.Loading)
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        fetchHomeData()
    }

    private fun fetchHomeData() {
        _uiState.value = HomeState.Loading

        viewModelScope.launch {
            try {
                // By putting a try/catch INSIDE the async block,
                // a failure will just return an empty list instead of crashing the app!
                val featuredDef = async(Dispatchers.IO) {
                    try {
                        ApiClient.gameService.getFeaturedGames().execute().body() ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val trendingDef = async(Dispatchers.IO) {
                    try {
                        ApiClient.gameService.getTrendingGames(0, 15).execute().body()?.content ?: emptyList()
                    } catch (e: Exception) {
                        Log.e("API_DEBUG", "Trending Failed!", e) // <--- ADD THIS
                        emptyList()
                    }
                }

                val newReleasesDef = async(Dispatchers.IO) {
                    try {
                        ApiClient.gameService.getNewReleases(0, 15).execute().body()?.content ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val topRatedDef = async(Dispatchers.IO) {
                    try {
                        ApiClient.gameService.getTopRatedGames(0, 15).execute().body()?.content ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                // Wait for all 4 background tasks to finish downloading (or fail safely)
                val data = HomeData(
                    featured = featuredDef.await(),
                    trending = trendingDef.await(),
                    newReleases = newReleasesDef.await(),
                    topRated = topRatedDef.await()
                )

                // If ALL of them failed, show a user-friendly error
                if (data.featured.isEmpty() && data.trending.isEmpty() && data.newReleases.isEmpty() && data.topRated.isEmpty()) {
                    _uiState.value = HomeState.Error("Could not connect to the game server.")
                } else {
                    _uiState.value = HomeState.Success(data)
                }

            } catch (e: Exception) {
                // This will now only trigger if something catastrophic happens
                _uiState.value = HomeState.Error("Critical Error: ${e.localizedMessage}")
            }
        }
    }
}