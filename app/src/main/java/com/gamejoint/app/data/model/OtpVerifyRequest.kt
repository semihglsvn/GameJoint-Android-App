package com.gamejoint.app.data.model

import com.google.gson.annotations.SerializedName

data class OtpVerifyRequest(
        @SerializedName("identifier")
        val identifier: String,

        @SerializedName("otp")
        val otp: String
)