package com.gamejoint.app.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class PublicProfileResponse(
        @SerializedName("id") val id: Long,
        @SerializedName("username") val username: String,
        @SerializedName("createdAt") val createdAt: LocalDateTime,
        @SerializedName("roleName") val roleName: String?,
        @SerializedName("isBanned") val isBanned: Boolean
)