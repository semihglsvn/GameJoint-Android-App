package com.gamejoint.app.data.remote

import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.gamejoint.app.data.model.ReportCreateRequest

interface ReportControllerApi {
    /**
     * POST api/reports
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param reportCreateRequest 
     * @return [Call]<[kotlin.collections.Map<kotlin.String, kotlin.String>]>
     */
    @POST("api/reports")
    fun submitReport(@Body reportCreateRequest: ReportCreateRequest): Call<kotlin.collections.Map<kotlin.String, kotlin.String>>

}
