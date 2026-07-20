package com.gamejoint.app.data.remote

import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.gamejoint.app.data.model.PageReviewResponse
import com.gamejoint.app.data.model.ReviewCreateRequest
import com.gamejoint.app.data.model.ReviewUpdateRequest

interface ReviewControllerApi {
    /**
     * POST api/reviews
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param reviewCreateRequest 
     * @return [Call]<[kotlin.collections.Map<kotlin.String, kotlin.String>]>
     */
    @POST("api/reviews")
    fun createReview(@Body reviewCreateRequest: ReviewCreateRequest): Call<kotlin.collections.Map<kotlin.String, kotlin.String>>

    /**
     * DELETE api/reviews/{reviewId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param reviewId 
     * @return [Call]<[kotlin.collections.Map<kotlin.String, kotlin.String>]>
     */
    @DELETE("api/reviews/{reviewId}")
    fun deleteReview(@Path("reviewId") reviewId: kotlin.Long): Call<kotlin.collections.Map<kotlin.String, kotlin.String>>

    /**
     * GET api/reviews/game/{gameId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param gameId 
     * @param roleId  (optional, default to 5L)
     * @param page  (optional, default to 0)
     * @param size  (optional, default to 10)
     * @return [Call]<[PageReviewResponse]>
     */
    @GET("api/reviews/game/{gameId}")
    fun getGameReviews(@Path("gameId") gameId: kotlin.Long, @Query("roleId") roleId: kotlin.Long? = 5L, @Query("page") page: kotlin.Int? = 0, @Query("size") size: kotlin.Int? = 10): Call<PageReviewResponse>

    /**
     * PUT api/reviews/{reviewId}
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param reviewId 
     * @param reviewUpdateRequest 
     * @return [Call]<[kotlin.collections.Map<kotlin.String, kotlin.String>]>
     */
    @PUT("api/reviews/{reviewId}")
    fun updateReview(@Path("reviewId") reviewId: kotlin.Long, @Body reviewUpdateRequest: ReviewUpdateRequest): Call<kotlin.collections.Map<kotlin.String, kotlin.String>>

}
