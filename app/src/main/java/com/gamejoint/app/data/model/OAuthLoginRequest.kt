package com.gamejoint.app.data.model

import com.google.gson.annotations.SerializedName

data class OAuthLoginRequest(
    @SerializedName("provider")
    val provider: String,

    @SerializedName("providerToken")
    val providerToken: String,

    @SerializedName("cfTurnstileResponse")
    val cfTurnstileResponse: String? = null
)