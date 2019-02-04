package com.pusher.pushnotifications.auth

import com.google.gson.Gson
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URL

/**
 * Describes the data that will be used in the auth request.
 *
 * @param headers the request headers
 * @param queryParams the request url query params
 */
data class AuthData(
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap()
)

/**
 * Callback for getting additional data used to authenticate Beams token request.
 */
interface AuthDataGetter {

  /**
   * Returns additional data used to authenticate Beams token request.
   */
  fun getAuthData(): AuthData
}

private val gson = Gson()
private val okHttpClient = OkHttpClient()

private data class TokenResponse(
    val token: String
)

/**
 * Pusher-approved implementation of a TokenProvider that will suit most cases.
 *
 * Makes a Get request to the configured `authUrl` with the data provided by the `authDataGetter`,
 * for example, the user's session token.
 * Additionally, the `user_id` will be sent as a query param to allow for further validation.
 *
 * @param authUrl the endpoint in your server used to generate Beams Tokens
 * @param authDataGetter the callback for getting additional data used to authenticate
 *        Beams token request
 */
class BeamsTokenProvider(
    private val authUrl: String,
    private val authDataGetter: AuthDataGetter
): TokenProvider {
  override fun fetchToken(userId: String): String {
    val authData = authDataGetter.getAuthData()

    val parsedURL = URL(authUrl)
    val targetUrl = HttpUrl.Builder()
        .scheme(parsedURL.protocol)
        .host(parsedURL.host)
        .port(if (parsedURL.port == -1) { parsedURL.defaultPort } else { parsedURL.port })
        .encodedPath(parsedURL.path)
        .query(parsedURL.query)

    // always setting the `userId` as a query param by default
    targetUrl.addQueryParameter("user_id", userId)
    authData.queryParams.forEach { (k, v) ->
      targetUrl.addQueryParameter(k, v)
    }

    val requestBuilder = Request.Builder().url(targetUrl.build()).get()
    authData.headers.forEach { (k, v) ->
      requestBuilder.addHeader(k, v)
    }

    val request = requestBuilder.build()

    val response = okHttpClient.newCall(request).execute()
    if (!response.isSuccessful) {
      throw RuntimeException("Unexpected status code: ${response.code()}")
    }

    val responseBody = response.body() ?: throw IOException("Could not read response body")

    val tokenResponse = gson.fromJson(responseBody.string(), TokenResponse::class.java)
    return tokenResponse.token
  }
}
