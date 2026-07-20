package com.gamejoint.app.data.remote

import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.gamejoint.app.data.model.AccountDeleteRequest
import com.gamejoint.app.data.model.PasswordChangeRequest
import com.gamejoint.app.data.model.UserProfileResponse
import com.gamejoint.app.data.model.UserProfileUpdateRequest

interface UserControllerApi {
    /**
     * PUT api/users/password
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param passwordChangeRequest 
     * @return [Call]<[kotlin.collections.Map<kotlin.String, kotlin.String>]>
     */
    @PUT("api/users/password")
    fun changePassword(@Body passwordChangeRequest: PasswordChangeRequest): Call<kotlin.collections.Map<kotlin.String, kotlin.String>>

    /**
     * DELETE api/users/account
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param accountDeleteRequest 
     * @return [Call]<[kotlin.collections.Map<kotlin.String, kotlin.String>]>
     */
    @DELETE("api/users/account")
    fun deleteAccount(@Body accountDeleteRequest: AccountDeleteRequest): Call<kotlin.collections.Map<kotlin.String, kotlin.String>>

    /**
     * GET api/users/profile
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[UserProfileResponse]>
     */
    @GET("api/users/profile")
    fun getProfile(): Call<UserProfileResponse>

    /**
     * PUT api/users/profile
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param userProfileUpdateRequest 
     * @return [Call]<[kotlin.collections.Map<kotlin.String, kotlin.String>]>
     */
    @PUT("api/users/profile")
    fun updateProfile(@Body userProfileUpdateRequest: UserProfileUpdateRequest): Call<kotlin.collections.Map<kotlin.String, kotlin.String>>

}
