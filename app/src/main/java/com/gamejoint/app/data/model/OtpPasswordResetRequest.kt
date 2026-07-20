package com.gamejoint.app.data.model

import com.google.gson.annotations.SerializedName

data class OtpPasswordResetRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("otp")
    val otp: String,

    @SerializedName("newPassword")
    val newPassword: String
)