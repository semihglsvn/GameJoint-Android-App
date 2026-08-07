package com.gamejoint.app.data.model

import com.google.gson.annotations.SerializedName

data class OAuthAuthResponse(
    // FIX: Catch both "newUser" (Spring Boot default) and "isNewUser"
    @SerializedName("newUser", alternate = ["isNewUser"])
    val isNewUser: Boolean,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("jwtToken")
    val jwtToken: String? = null
)