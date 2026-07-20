package com.gamejoint.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.GameSummary
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val games: List<GameSummary>) : SearchState()
    data class Error(val message: String) : SearchState()
}

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SearchState>(SearchState.Idle)
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()

    var currentQuery = MutableStateFlow("")
    var hideTbd = MutableStateFlow(false)
    var isMatchAll = MutableStateFlow(false)
    var sortBy = MutableStateFlow("Highest Rated")
    var selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    var selectedPlatforms = MutableStateFlow<Set<String>>(emptySet())

    // --- NEW: PAGINATION STATE ---
    private var currentPage = 0
    private var isLastPage = false
    var isLoadingMore = MutableStateFlow(false)
    var totalResults = MutableStateFlow(0L)

    // Call this when the user clicks "Search" or changes a filter
    fun performSearch() {
        currentPage = 0
        isLastPage = false
        _uiState.value = SearchState.Loading
        fetchGames(isRefreshing = true)
    }

    // Call this when the user scrolls to the bottom of the list
    fun loadNextPage() {
        if (isLastPage || isLoadingMore.value || _uiState.value !is SearchState.Success) return
        isLoadingMore.value = true
        currentPage++
        fetchGames(isRefreshing = false)
    }

    private fun fetchGames(isRefreshing: Boolean) {
        viewModelScope.launch {
            try {
                val q = currentQuery.value.ifEmpty { null }
                val minScore = if (hideTbd.value) 1 else null
                val genresList = selectedGenres.value.toList().ifEmpty { null }
                val platformsList = selectedPlatforms.value.toList().ifEmpty { null }

                val response = withContext(Dispatchers.IO) {
                    ApiClient.gameService.searchGames(
                        q = q,
                        minMetascore = minScore,
                        hideTbd = hideTbd.value,
                        genres = genresList,
                        platforms = platformsList,
                        isMatchAll = isMatchAll.value,
                        sortBy = sortBy.value,
                        page = currentPage,
                        size = 20 // Standard page size
                    ).execute()
                }

                if (response.isSuccessful) {
                    val pageData = response.body()
                    val newGames = pageData?.content ?: emptyList()

                    // Update the Total Results count (Safely defaulting to 0)
                    totalResults.value = pageData?.totalElements ?: 0L

                    // If we got fewer than 20 games back, we hit the end of the database
                    isLastPage = newGames.size < 20

                    if (isRefreshing) {
                        _uiState.value = SearchState.Success(newGames)
                    } else {
                        // Append the new games to the existing list!
                        val currentGames = (_uiState.value as SearchState.Success).games
                        _uiState.value = SearchState.Success(currentGames + newGames)
                    }
                } else {
                    if (isRefreshing) _uiState.value = SearchState.Error("Search failed: ${response.code()}")
                }
            } catch (e: Exception) {
                if (isRefreshing) _uiState.value = SearchState.Error("Network Error: ${e.localizedMessage}")
            } finally {
                isLoadingMore.value = false
            }
        }
    }
}