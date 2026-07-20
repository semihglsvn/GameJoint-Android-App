package com.gamejoint.app.data.network

import com.gamejoint.app.BuildConfig
import com.gamejoint.app.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. Start building the new request
        val requestBuilder = originalRequest.newBuilder()

        // 2. ALWAYS attach the mobile bypass secret
        requestBuilder.header("X-Mobile-App-Secret", BuildConfig.MOBILE_API_SECRET)

        // 3. Read the token from DataStore safely on this background thread
        val token = runBlocking { sessionManager.jwtTokenFlow.first() }

        // 4. If we DO have a JWT token, attach it for protected endpoints
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}