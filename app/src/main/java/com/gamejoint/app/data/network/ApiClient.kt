package com.gamejoint.app.data.network

import com.gamejoint.app.data.local.SessionManager
import com.gamejoint.app.data.remote.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
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

        // --- FIXED: Explicit Object declarations prevent Kotlin Type Erasure ---
        val customGson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, object : JsonDeserializer<LocalDate> {
                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate {
                    return LocalDate.parse(json.asString)
                }
            })
            .registerTypeAdapter(LocalDateTime::class.java, object : JsonDeserializer<LocalDateTime> {
                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {
                    return LocalDateTime.parse(json.asString)
                }
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
            .addConverterFactory(GsonConverterFactory.create(customGson))
            .build()

        authService = retrofit.create(AuthControllerApi::class.java)
        gameService = retrofit.create(GameControllerApi::class.java)
        moderationService = retrofit.create(ModerationControllerApi::class.java)
        reportService = retrofit.create(ReportControllerApi::class.java)
        reviewService = retrofit.create(ReviewControllerApi::class.java)
        userService = retrofit.create(UserControllerApi::class.java)
    }
}