package com.gamejoint.app.ui.game

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.local.SessionManager
import com.gamejoint.app.data.model.*
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class GameDetailState {
    object Loading : GameDetailState()
    data class Success(val game: GameDetail) : GameDetailState()
    data class Error(val message: String) : GameDetailState()
}

class GameDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("draft_reviews", Context.MODE_PRIVATE)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<GameDetailState>(GameDetailState.Loading)
    val uiState: StateFlow<GameDetailState> = _uiState.asStateFlow()

    private val _feedbackMessage = MutableSharedFlow<String>()
    val feedbackMessage: SharedFlow<String> = _feedbackMessage.asSharedFlow()

    val isLoggedIn = MutableStateFlow(false)
    val currentUserRole = MutableStateFlow(5L)
    val isBanned = MutableStateFlow(false)
    val currentUserId = MutableStateFlow<Long?>(null)
    val currentUsername = MutableStateFlow<String?>(null)

    val userReviews = MutableStateFlow<List<ReviewResponse>>(emptyList())
    val criticReviews = MutableStateFlow<List<ReviewResponse>>(emptyList())
    val avgUserScore = MutableStateFlow<Double?>(null)

    val draftScore = MutableStateFlow(0)
    val draftText = MutableStateFlow("")
    val existingReviewId = MutableStateFlow<Long?>(null)

    val showReviewModal = MutableStateFlow(false)
    val showReportModal = MutableStateFlow(false)
    val showBanModal = MutableStateFlow(false)

    val targetReviewId = MutableStateFlow<Long?>(null)
    val targetUserId = MutableStateFlow<Long?>(null)
    val targetUsername = MutableStateFlow("")

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.jwtTokenFlow.collect { token ->
                if (!token.isNullOrEmpty()) {
                    isLoggedIn.value = true
                    parseTokenData(token)
                } else {
                    isLoggedIn.value = false
                    currentUserRole.value = 5L
                    isBanned.value = false
                    currentUserId.value = null
                    currentUsername.value = null
                }
            }
        }
    }

    private fun parseTokenData(token: String) {
        try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
                val json = JSONObject(payloadJson)

                if (json.has("userId")) currentUserId.value = json.getLong("userId")
                if (json.has("id")) currentUserId.value = json.getLong("id")
                if (json.has("roleId")) currentUserRole.value = json.getLong("roleId")
                if (json.has("role")) {
                    val roleVal = json.get("role")
                    if (roleVal is Long) currentUserRole.value = roleVal
                    else if (roleVal is Int) currentUserRole.value = roleVal.toLong()
                }
                if (json.has("sub")) currentUsername.value = json.getString("sub")
                if (json.has("username")) currentUsername.value = json.getString("username")
                if (json.has("isBanned")) isBanned.value = json.getBoolean("isBanned")
            }
        } catch (e: Exception) {}
    }

    fun loadGame(gameId: Long) {
        _uiState.value = GameDetailState.Loading
        loadDraft(gameId)

        viewModelScope.launch {
            try {
                val gameResponse = withContext(Dispatchers.IO) { ApiClient.gameService.getGameById(gameId).execute() }
                if (gameResponse.isSuccessful && gameResponse.body() != null) {
                    _uiState.value = GameDetailState.Success(gameResponse.body()!!)
                } else {
                    _uiState.value = GameDetailState.Error("Failed to load game details")
                    return@launch
                }

                val userRevResponse = withContext(Dispatchers.IO) { ApiClient.reviewService.getGameReviews(gameId = gameId, roleId = 5L, size = 50).execute() }
                if (userRevResponse.isSuccessful) {
                    val reviews = userRevResponse.body()?.content ?: emptyList()
                    userReviews.value = reviews
                    avgUserScore.value = if (reviews.isNotEmpty()) reviews.map { it.score ?: 0 }.average() else null
                    detectExistingUserReview(reviews)
                }

                val criticRevResponse = withContext(Dispatchers.IO) { ApiClient.reviewService.getGameReviews(gameId = gameId, roleId = 4L, size = 50).execute() }
                if (criticRevResponse.isSuccessful) {
                    criticReviews.value = criticRevResponse.body()?.content ?: emptyList()
                }
            } catch (e: Exception) {
                _uiState.value = GameDetailState.Error("Network Error: ${e.message}")
            }
        }
    }

    private fun detectExistingUserReview(reviews: List<ReviewResponse>) {
        val cName = currentUsername.value
        val myReview = reviews.find { cName != null && it.authorUsername.equals(cName, ignoreCase = true) }
        if (myReview != null) {
            existingReviewId.value = myReview.id
            draftScore.value = myReview.score ?: 0
            draftText.value = myReview.comment ?: ""
        } else {
            existingReviewId.value = null
        }
    }

    private fun loadDraft(gameId: Long) {
        if (existingReviewId.value == null) {
            draftText.value = prefs.getString("draft_${gameId}_text", "") ?: ""
            draftScore.value = prefs.getInt("draft_${gameId}_score", 0)
        }
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

    // --- HELPER TO CHECK BANS DYNAMICALLY ---
    private fun handleNetworkError(errorBody: String?, code: Int): String {
        if (errorBody?.contains("restricted", ignoreCase = true) == true || code == 403) {
            isBanned.value = true // Instantly locks down the UI!
            return "Action blocked: Your account is restricted."
        }
        return "Action failed (Error $code)"
    }

    fun submitReview(gameId: Long) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    if (existingReviewId.value != null) {
                        ApiClient.reviewService.updateReview(existingReviewId.value!!, ReviewUpdateRequest(score = draftScore.value, comment = draftText.value)).execute()
                    } else {
                        ApiClient.reviewService.createReview(ReviewCreateRequest(gameId = gameId, score = draftScore.value, comment = draftText.value)).execute()
                    }
                }

                if (response.isSuccessful) {
                    clearDraft(gameId)
                    showReviewModal.value = false
                    _feedbackMessage.emit(if (existingReviewId.value != null) "Review updated successfully!" else "Review posted successfully!")
                    loadGame(gameId)
                } else {
                    _feedbackMessage.emit(handleNetworkError(response.errorBody()?.string(), response.code()))
                }
            } catch (e: Exception) {
                _feedbackMessage.emit("Network error occurred.")
            }
        }
    }

    fun deleteReview(gameId: Long) {
        val reviewId = existingReviewId.value ?: return
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { ApiClient.reviewService.deleteReview(reviewId).execute() }
                if (response.isSuccessful) {
                    existingReviewId.value = null
                    clearDraft(gameId)
                    _feedbackMessage.emit("Review deleted.")
                    loadGame(gameId)
                } else {
                    _feedbackMessage.emit(handleNetworkError(response.errorBody()?.string(), response.code()))
                }
            } catch (e: Exception) {
                _feedbackMessage.emit("Network error occurred.")
            }
        }
    }

    fun submitReport(reasons: List<String>) {
        val rId = targetReviewId.value ?: return
        viewModelScope.launch {
            try {
                val request = ReportCreateRequest(reviewId = rId, reasons = reasons)
                val response = withContext(Dispatchers.IO) { ApiClient.reportService.submitReport(request).execute() }
                if (response.isSuccessful) {
                    showReportModal.value = false
                    _feedbackMessage.emit("Report submitted to moderators.")
                } else {
                    _feedbackMessage.emit(handleNetworkError(response.errorBody()?.string(), response.code()))
                }
            } catch (e: Exception) {
                _feedbackMessage.emit("Network error occurred.")
            }
        }
    }

    fun banUser(durationDays: Int?, reason: String?) {
        val uid = targetUserId.value ?: return
        viewModelScope.launch {
            try {
                val request = BanRequest(durationDays = durationDays, reason = reason)
                val response = withContext(Dispatchers.IO) { ApiClient.moderationService.banUser(targetUserId = uid, banRequest = request).execute() }
                if (response.isSuccessful) {
                    showBanModal.value = false
                    _feedbackMessage.emit("User has been banned.")
                    if (_uiState.value is GameDetailState.Success) {
                        loadGame((_uiState.value as GameDetailState.Success).game.id ?: 0L)
                    }
                } else {
                    _feedbackMessage.emit("Failed to ban user (Error ${response.code()}).")
                }
            } catch (e: Exception) {
                _feedbackMessage.emit("Network error occurred.")
            }
        }
    }
}