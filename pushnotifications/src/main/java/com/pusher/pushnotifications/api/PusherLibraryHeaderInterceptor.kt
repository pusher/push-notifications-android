package com.pusher.pushnotifications.api

import com.pusher.pushnotifications.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class PusherLibraryHeaderInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val newRequest = request.newBuilder()
        .addHeader("x-pusher-library", "push-notifications-android ${BuildConfig.VERSION_NAME}")
        .build()
    return chain.proceed(newRequest)
  }
}
