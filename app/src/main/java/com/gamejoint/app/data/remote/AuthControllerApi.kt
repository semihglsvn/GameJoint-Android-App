package com.gamejoint.app.data.remote

import retrofit2.http.*
import retrofit2.Call
import com.gamejoint.app.data.model.OtpPasswordResetRequest
import com.gamejoint.app.data.model.OtpVerifyRequest
import com.gamejoint.app.data.model.PasswordResetExecuteRequest
import com.gamejoint.app.data.model.TokenResponse
import com.gamejoint.app.data.model.UserLoginRequest
import com.gamejoint.app.data.model.UserRegistrationRequest

interface AuthControllerApi {

    /**
     * CORE LOGIN
     */
    @POST("api/auth/login")
    fun login(@Body userLoginRequest: UserLoginRequest): Call<TokenResponse>

    /**
     * CORE REGISTER
     */
    @POST("api/auth/register")
    fun register(@Body userRegistrationRequest: UserRegistrationRequest): Call<Map<String, String>>

    /**
     * VERIFY ACCOUNT VIA MOBILE OTP
     */
    @POST("api/auth/verify/otp")
    fun verifyAccountOtp(@Body request: OtpVerifyRequest): Call<Map<String, String>>

    /**
     * RESET PASSWORD VIA MOBILE OTP
     */
    @POST("api/auth/password/reset/otp")
    fun resetPasswordOtp(@Body request: OtpPasswordResetRequest): Call<Map<String, String>>

    /**
     * RESEND VERIFICATION OTP
     */
    @POST("api/auth/verify/resend")
    fun resendVerification(@Body requestBody: Map<String, String>): Call<Map<String, String>>

    /**
     * REQUEST PASSWORD RESET OTP
     */
    @POST("api/auth/password/forgot")
    fun forgotPassword(@Body requestBody: Map<String, String>): Call<Map<String, String>>

    /**
     * WEB LEGACY: RESET PASSWORD VIA MAGIC LINK
     */
    @POST("api/auth/password/reset")
    fun resetPassword(@Body passwordResetExecuteRequest: PasswordResetExecuteRequest): Call<Map<String, String>>
}