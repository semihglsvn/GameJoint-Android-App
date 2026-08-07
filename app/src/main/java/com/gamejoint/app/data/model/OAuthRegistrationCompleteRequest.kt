package com.gamejoint.app.data.model

import com.google.gson.annotations.SerializedName

data class OAuthRegistrationCompleteRequest(
    @SerializedName("provider")
    val provider: String,

    @SerializedName("providerToken")
    val providerToken: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("dob")
    val dob: String, // Ensure this is sent in "YYYY-MM-DD" format

    @SerializedName("cfTurnstileResponse")
    val cfTurnstileResponse: String? = null
)