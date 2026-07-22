package com.gamejoint.app.data.network

import com.gamejoint.app.data.local.SessionManager
import com.gamejoint.app.data.remote.* // Imports all 6 of your Controller APIs
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit

object ApiClient {

    lateinit var authService: AuthControllerApi
    lateinit var gameService: GameControllerApi
    lateinit var moderationService: ModerationControllerApi
    lateinit var reportService: ReportControllerApi
    lateinit var reviewService: ReviewControllerApi
    lateinit var userService: UserControllerApi

    lateinit var authInterceptor: AuthInterceptor

    fun initialize(dynamicBaseUrl: String, sessionManager: SessionManager) {

        authInterceptor = AuthInterceptor(sessionManager)

        val safeUrl = if (dynamicBaseUrl.endsWith("/")) dynamicBaseUrl else "$dynamicBaseUrl/"

        // --- FIX 3: Custom Gson to parse String into LocalDate ---
        val customGson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ ->
                LocalDate.parse(json.asString)
            })
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(customGson)) // Attach custom Gson here!
            .build()

        authService = retrofit.create(AuthControllerApi::class.java)
        gameService = retrofit.create(GameControllerApi::class.java)
        moderationService = retrofit.create(ModerationControllerApi::class.java)
        reportService = retrofit.create(ReportControllerApi::class.java)
        reviewService = retrofit.create(ReviewControllerApi::class.java)
        userService = retrofit.create(UserControllerApi::class.java)
    }
}