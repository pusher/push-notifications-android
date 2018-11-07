package com.pusher.pushnotifications.auth

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class AuthData(
    val headers: Map<String, String> = emptyMap(),
    val urlParams: Map<String, String> = emptyMap()
)

interface AuthDataGetter {
  fun getAuthData(): AuthData
}

private val gson = Gson()

data class TokenResponse(
    val Token: String
)

class BeamsTokenProvider(
    private val authUrl: String,
    private val authDataGetter: AuthDataGetter
) : TokenProvider {
  override fun fetchToken(): String {
    val authData = authDataGetter.getAuthData()

    val requestBuilder = Request.Builder().url(authUrl).get()
    authData.headers.forEach { (k, v) ->
      requestBuilder.addHeader(k, v)
    }

    val response = OkHttpClient().newCall(requestBuilder.build()).execute()
    if (!response.isSuccessful) {
      throw RuntimeException("Unexpected status code: ${response.code()}")
    }

    val responseBody = response.body() ?: throw IOException("Could not read response body")

    val tokenResponse = gson.fromJson(responseBody.string(), TokenResponse::class.java)
    return tokenResponse.Token
  }
}