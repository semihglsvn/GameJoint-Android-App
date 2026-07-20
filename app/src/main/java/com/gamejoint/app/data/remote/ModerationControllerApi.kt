package com.gamejoint.app.data.remote

import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.gamejoint.app.data.model.BanRequest

interface ModerationControllerApi {
    /**
     * POST api/moderation/users/{targetUserId}/ban
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param targetUserId 
     * @param banRequest 
     * @return [Call]<[kotlin.String]>
     */
    @POST("api/moderation/users/{targetUserId}/ban")
    fun banUser(@Path("targetUserId") targetUserId: kotlin.Long, @Body banRequest: BanRequest): Call<kotlin.String>

    /**
     * POST api/moderation/users/{targetUserId}/unban
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param targetUserId 
     * @return [Call]<[kotlin.String]>
     */
    @POST("api/moderation/users/{targetUserId}/unban")
    fun unbanUser(@Path("targetUserId") targetUserId: kotlin.Long): Call<kotlin.String>

}
