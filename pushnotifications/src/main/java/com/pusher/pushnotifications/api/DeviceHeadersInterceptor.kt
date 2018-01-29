package com.pusher.pushnotifications.api

import com.pusher.pushnotifications.BuildConfig
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class DeviceHeadersInterceptor : Interceptor {
  private val sdkVersion = BuildConfig.VERSION_NAME

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request();
    val newRequest = request.newBuilder()
        .addHeader("x-pusher-library", "push-notifications-android $sdkVersion")
        .build()
    return chain.proceed(newRequest)
  }
}
