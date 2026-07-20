package com.gamejoint.app.data.remote

import retrofit2.http.*
import retrofit2.Call
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.gamejoint.app.data.model.FeaturedGameResponse
import com.gamejoint.app.data.model.GameDetail
import com.gamejoint.app.data.model.PageGameSummary

interface GameControllerApi {
    /**
     * GET api/games
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional, default to 0)
     * @param size  (optional, default to 20)
     * @return [Call]<[PageGameSummary]>
     */
    @GET("api/games")
    fun getAllGames(@Query("page") page: kotlin.Int? = 0, @Query("size") size: kotlin.Int? = 20): Call<PageGameSummary>

    /**
     * GET api/games/featured
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [Call]<[kotlin.collections.List<FeaturedGameResponse>]>
     */
    @GET("api/games/featured")
    fun getFeaturedGames(): Call<kotlin.collections.List<FeaturedGameResponse>>

    /**
     * GET api/games/{id}
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [Call]<[GameDetail]>
     */
    @GET("api/games/{id}")
    fun getGameById(@Path("id") id: kotlin.Long): Call<GameDetail>

    /**
     * GET api/games/new-releases
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional, default to 0)
     * @param size  (optional, default to 15)
     * @return [Call]<[PageGameSummary]>
     */
    @GET("api/games/new-releases")
    fun getNewReleases(@Query("page") page: kotlin.Int? = 0, @Query("size") size: kotlin.Int? = 15): Call<PageGameSummary>

    /**
     * GET api/games/top-rated
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional, default to 0)
     * @param size  (optional, default to 15)
     * @return [Call]<[PageGameSummary]>
     */
    @GET("api/games/top-rated")
    fun getTopRatedGames(@Query("page") page: kotlin.Int? = 0, @Query("size") size: kotlin.Int? = 15): Call<PageGameSummary>

    /**
     * GET api/games/trending
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional, default to 0)
     * @param size  (optional, default to 15)
     * @return [Call]<[PageGameSummary]>
     */
    @GET("api/games/trending")
    fun getTrendingGames(@Query("page") page: kotlin.Int? = 0, @Query("size") size: kotlin.Int? = 15): Call<PageGameSummary>

    /**
     * GET api/games/search
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param q 
     * @param page  (optional, default to 0)
     * @param size  (optional, default to 20)
     * @return [Call]<[PageGameSummary]>
     */
    /**
     * GET api/games/search
     * Advanced Dynamic Search
     */
    @GET("api/games/search")
    fun searchGames(
        @Query("q") q: kotlin.String? = null,
        @Query("minMetascore") minMetascore: kotlin.Int? = null,
        @Query("hideTbd") hideTbd: kotlin.Boolean? = null,
        @Query("genres") genres: kotlin.collections.List<kotlin.String>? = null,
        @Query("platforms") platforms: kotlin.collections.List<kotlin.String>? = null,
        @Query("isMatchAll") isMatchAll: kotlin.Boolean? = false, // ADDED
        @Query("sortBy") sortBy: kotlin.String? = "Highest Rated", // ADDED
        @Query("page") page: kotlin.Int? = 0,
        @Query("size") size: kotlin.Int? = 20
    ): Call<PageGameSummary>
}
