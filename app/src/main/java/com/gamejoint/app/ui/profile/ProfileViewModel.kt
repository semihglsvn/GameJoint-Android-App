package com.gamejoint.app.ui.profile

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamejoint.app.data.local.SessionManager
import com.gamejoint.app.data.model.PublicProfileResponse
import com.gamejoint.app.data.model.ReviewResponse
import com.gamejoint.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.json.JSONObject

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)

    // CHANGED: We now track username instead of ID
    val currentUsername = MutableStateFlow<String?>(null)

    val profile = MutableStateFlow<PublicProfileResponse?>(null)
    val rawReviews = MutableStateFlow<List<ReviewResponse>>(emptyList())
    val displayReviews = MutableStateFlow<List<ReviewResponse>>(emptyList())

    val isLoading = MutableStateFlow(true)
    val errorMessage = MutableStateFlow<String?>(null)

    val currentFilter = MutableStateFlow("all")
    val currentSort = MutableStateFlow("date-desc")

    init {
        extractCurrentUsername()
    }

    private fun extractCurrentUsername() {
        viewModelScope.launch {
            val token = sessionManager.jwtTokenFlow.firstOrNull()
            if (!token.isNullOrEmpty()) {
                try {
                    val parts = token.split(".")
                    if (parts.size >= 2) {
                        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
                        val json = JSONObject(payload)
                        // Extracts "sub" (subject) which holds the username in standard JWTs
                        currentUsername.value = if (json.has("sub")) json.getString("sub") else json.optString("username", null)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    // CHANGED: Accepts String username
    fun loadProfileData(targetUsername: String) {
        viewModelScope.launch {
            if (profile.value == null) isLoading.value = true
            errorMessage.value = null
            try {
                supervisorScope {
                    val profileDef = async(Dispatchers.IO) { ApiClient.userService.getPublicProfile(targetUsername).execute() }
                    val reviewsDef = async(Dispatchers.IO) { ApiClient.reviewService.getUserReviews(targetUsername).execute() }

                    val pRes = profileDef.await()
                    val rRes = reviewsDef.await()

                    if (pRes.isSuccessful && rRes.isSuccessful) {
                        profile.value = pRes.body()
                        rawReviews.value = rRes.body() ?: emptyList()
                        applyFiltersAndSort()
                    } else {
                        errorMessage.value = "User not found."
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Network error while loading profile."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun setFilter(type: String) {
        currentFilter.value = type
        applyFiltersAndSort()
    }

    fun setSort(type: String) {
        currentSort.value = type
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        var list = rawReviews.value

        list = when (currentFilter.value) {
            "green" -> list.filter { getNormalizedScore(it.score) >= 75.0 }
            "yellow" -> list.filter { getNormalizedScore(it.score) in 50.0..74.9 }
            "red" -> list.filter { getNormalizedScore(it.score) < 50.0 }
            else -> list
        }

        list = when (currentSort.value) {
            "date-desc" -> list.sortedByDescending { it.createdAt }
            "date-asc" -> list.sortedBy { it.createdAt }
            "desc" -> list.sortedByDescending { getNormalizedScore(it.score) }
            "asc" -> list.sortedBy { getNormalizedScore(it.score) }
            else -> list
        }

        displayReviews.value = list
    }

    fun getNormalizedScore(score: Int): Double {
        val scoreDouble = score.toDouble()
        return if (scoreDouble <= 10.0) scoreDouble * 10 else scoreDouble
    }
}