package com.gamejoint.app.ui.game

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.model.*
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class GameDetailState {
    object Loading : GameDetailState()
    data class Success(val game: GameDetail) : GameDetailState()
    data class Error(val message: String) : GameDetailState()
}

class GameDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("draft_reviews", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<GameDetailState>(GameDetailState.Loading)
    val uiState: StateFlow<GameDetailState> = _uiState.asStateFlow()

    // --- AUTHENTICATION & SESSION STATE ---
    // Update these from your Login/Session manager!
    var isLoggedIn = MutableStateFlow(false)
    var currentUserRole = MutableStateFlow(5L)
    var isBanned = MutableStateFlow(false)

    var userReviews = MutableStateFlow<List<ReviewResponse>>(emptyList())
    var criticReviews = MutableStateFlow<List<ReviewResponse>>(emptyList())
    var avgUserScore = MutableStateFlow<Double?>(null)

    // Draft Tracking
    var draftScore = MutableStateFlow(0)
    var draftText = MutableStateFlow("")
    var existingReviewId = MutableStateFlow<Long?>(null)

    // Modal Visibility Triggers
    var showReviewModal = MutableStateFlow(false)
    var showReportModal = MutableStateFlow(false)
    var showBanModal = MutableStateFlow(false)

    var targetReviewId = MutableStateFlow<Long?>(null)
    var targetUserId = MutableStateFlow<Long?>(null)
    var targetUsername = MutableStateFlow("")

    fun loadGame(gameId: Long) {
        _uiState.value = GameDetailState.Loading
        loadDraft(gameId)

        viewModelScope.launch {
            try {
                val gameResponse = withContext(Dispatchers.IO) {
                    ApiClient.gameService.getGameById(gameId).execute()
                }

                if (gameResponse.isSuccessful && gameResponse.body() != null) {
                    _uiState.value = GameDetailState.Success(gameResponse.body()!!)
                } else {
                    _uiState.value = GameDetailState.Error("Failed to load game details")
                    return@launch
                }

                // Fetch User Reviews (roleId 5)
                val userRevResponse = withContext(Dispatchers.IO) {
                    ApiClient.reviewService.getGameReviews(gameId = gameId, roleId = 5L, size = 50).execute()
                }
                if (userRevResponse.isSuccessful) {
                    val reviews = userRevResponse.body()?.content ?: emptyList()
                    userReviews.value = reviews

                    if (reviews.isNotEmpty()) {
                        avgUserScore.value = reviews.map { it.score ?: 0 }.average()
                    } else {
                        avgUserScore.value = null
                    }
                }

                // Fetch Critic Reviews (roleId 4)
                val criticRevResponse = withContext(Dispatchers.IO) {
                    ApiClient.reviewService.getGameReviews(gameId = gameId, roleId = 4L, size = 50).execute()
                }
                if (criticRevResponse.isSuccessful) {
                    criticReviews.value = criticRevResponse.body()?.content ?: emptyList()
                }

            } catch (e: Exception) {
                _uiState.value = GameDetailState.Error("Network Error: ${e.message}")
            }
        }
    }

    private fun loadDraft(gameId: Long) {
        draftText.value = prefs.getString("draft_${gameId}_text", "") ?: ""
        draftScore.value = prefs.getInt("draft_${gameId}_score", 0)
    }

    fun saveDraft(gameId: Long, text: String, score: Int) {
        draftText.value = text
        draftScore.value = score
        prefs.edit {
            putString("draft_${gameId}_text", text)
            putInt("draft_${gameId}_score", score)
        }
    }

    private fun clearDraft(gameId: Long) {
        draftText.value = ""
        draftScore.value = 0
        prefs.edit {
            remove("draft_${gameId}_text")
            remove("draft_${gameId}_score")
        }
    }

    fun submitReview(gameId: Long) {
        viewModelScope.launch {
            try {
                if (existingReviewId.value != null) {
                    val request = ReviewUpdateRequest(score = draftScore.value, comment = draftText.value)
                    withContext(Dispatchers.IO) {
                        ApiClient.reviewService.updateReview(existingReviewId.value!!, request).execute()
                    }
                } else {
                    val request = ReviewCreateRequest(gameId = gameId, score = draftScore.value, comment = draftText.value)
                    withContext(Dispatchers.IO) {
                        ApiClient.reviewService.createReview(request).execute()
                    }
                }
                clearDraft(gameId)
                showReviewModal.value = false
                loadGame(gameId)
            } catch (e: Exception) {}
        }
    }

    fun deleteReview(gameId: Long, reviewId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.reviewService.deleteReview(reviewId).execute()
                }
                existingReviewId.value = null
                clearDraft(gameId)
                loadGame(gameId)
            } catch (e: Exception) {}
        }
    }

    fun submitReport(reason: String) {
        val rId = targetReviewId.value ?: return
        viewModelScope.launch {
            try {
                val request = ReportCreateRequest(reviewId = rId, reasons = listOf(reason))
                withContext(Dispatchers.IO) {
                    ApiClient.reportService.submitReport(request).execute()
                }
                showReportModal.value = false
            } catch (e: Exception) {}
        }
    }

    fun banUser(durationDays: Int?, reason: String?) {
        val uid = targetUserId.value ?: return
        viewModelScope.launch {
            try {
                val request = BanRequest(durationDays = durationDays, reason = reason)
                withContext(Dispatchers.IO) {
                    ApiClient.moderationService.banUser(targetUserId = uid, banRequest = request).execute()
                }
                showBanModal.value = false
                if (_uiState.value is GameDetailState.Success) {
                    loadGame((_uiState.value as GameDetailState.Success).game.id ?: 0L)
                }
            } catch (e: Exception) {}
        }
    }
}