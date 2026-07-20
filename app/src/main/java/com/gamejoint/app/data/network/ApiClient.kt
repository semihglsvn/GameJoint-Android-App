package com.gamejoint.app.data.network

import com.gamejoint.app.data.local.SessionManager
import com.gamejoint.app.data.remote.* // Imports all 6 of your Controller APIs
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // We use your exact generated interface names here
    lateinit var authService: AuthControllerApi
    lateinit var gameService: GameControllerApi
    lateinit var moderationService: ModerationControllerApi
    lateinit var reportService: ReportControllerApi
    lateinit var reviewService: ReviewControllerApi
    lateinit var userService: UserControllerApi

    // Change this to lateinit so we can initialize it once we have the SessionManager
    lateinit var authInterceptor: AuthInterceptor

    // Pass the SessionManager into the initialization phase
    fun initialize(dynamicBaseUrl: String, sessionManager: SessionManager) {

        // Build the interceptor with the required session manager
        authInterceptor = AuthInterceptor(sessionManager)

        val safeUrl = if (dynamicBaseUrl.endsWith("/")) dynamicBaseUrl else "$dynamicBaseUrl/"

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create all 6 services so the rest of the app can use them instantly
        authService = retrofit.create(AuthControllerApi::class.java)
        gameService = retrofit.create(GameControllerApi::class.java)
        moderationService = retrofit.create(ModerationControllerApi::class.java)
        reportService = retrofit.create(ReportControllerApi::class.java)
        reviewService = retrofit.create(ReviewControllerApi::class.java)
        userService = retrofit.create(UserControllerApi::class.java)
    }
}