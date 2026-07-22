package com.gamejoint.app.data.remote

import com.gamejoint.app.data.model.AccountDeleteRequest
import com.gamejoint.app.data.model.EmailChangeRequest
import com.gamejoint.app.data.model.PasswordChangeRequest
import com.gamejoint.app.data.model.UserProfileResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserControllerApi {

    @GET("api/users/profile")
    fun getProfile(): Call<UserProfileResponse>

    @POST("api/users/settings/otp")
    fun requestSettingsOtp(): Call<Map<String, String>>

    @PUT("api/users/email")
    fun changeEmail(@Body request: EmailChangeRequest): Call<Map<String, String>>

    @PUT("api/users/password")
    fun changePassword(@Body request: PasswordChangeRequest): Call<Map<String, String>>

    // Retrofit's @DELETE does not support bodies. We MUST use @HTTP instead!
    @HTTP(method = "DELETE", path = "api/users/account", hasBody = true)
    fun deleteAccount(@Body request: AccountDeleteRequest): Call<Map<String, String>>
}