package com.gamejoint.app.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class ReviewResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("gameId") val gameId: Long,
    @SerializedName("gameTitle") val gameTitle: String,
    @SerializedName("authorUsername") val authorUsername: String,
    @SerializedName("authorRole") val authorRole: String?,
    @SerializedName("score") val score: Int, // Strictly Int
    @SerializedName("comment") val comment: String?,
    @SerializedName("createdAt") val createdAt: LocalDateTime,
    @SerializedName("status") val status: String
)