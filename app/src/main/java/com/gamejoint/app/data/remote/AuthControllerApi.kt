package com.gamejoint.app.data.remote

import retrofit2.http.*
import retrofit2.Call
import com.gamejoint.app.data.model.OAuthAuthResponse
import com.gamejoint.app.data.model.OAuthLoginRequest
import com.gamejoint.app.data.model.OAuthRegistrationCompleteRequest
import com.gamejoint.app.data.model.OtpPasswordResetRequest
import com.gamejoint.app.data.model.OtpVerifyRequest
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
     * OAUTH LOGIN (Google, Steam, Discord)
     */
    @POST("api/auth/oauth/login")
    fun oauthLogin(@Body request: OAuthLoginRequest): Call<OAuthAuthResponse>

    /**
     * OAUTH COMPLETE REGISTRATION
     */
    @POST("api/auth/oauth/complete")
    fun completeOAuthRegistration(@Body request: OAuthRegistrationCompleteRequest): Call<TokenResponse>

    /**
     * VERIFY ACCOUNT VIA OTP
     */
    @POST("api/auth/verify")
    fun verifyAccount(@Body request: OtpVerifyRequest): Call<Map<String, String>>

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
     * RESET PASSWORD VIA OTP
     */
    @POST("api/auth/password/reset")
    fun resetPassword(@Body request: OtpPasswordResetRequest): Call<Map<String, String>>
}