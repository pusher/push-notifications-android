package com.pusher.pushnotifications.auth

import android.os.AsyncTask
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

data class AuthData(
    val headers: Map<String, String> = emptyMap(),
    val urlParams: Map<String, String> = emptyMap()
)

class BeamsTokenProvider(
    val authUrl: String,
    val getAuthData: () -> AuthData
) : TokenProvider {
  override fun fetchToken(): String {
    val authData = getAuthData()
    var requestBuilder = Request.Builder().url(authUrl).post(RequestBody.create(null, ""))
    authData.headers.forEach { (k, v) -> requestBuilder = requestBuilder.addHeader(k, v)}
    val response = OkHttpClient().newCall(requestBuilder.build()).execute()

    return response.body()!!.string()
  }

}