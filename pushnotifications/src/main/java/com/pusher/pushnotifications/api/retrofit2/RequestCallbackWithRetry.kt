package com.pusher.pushnotifications.api.retrofit2

import android.os.Handler
import android.os.Looper
import com.pusher.pushnotifications.logging.Logger
import retrofit2.Call
import retrofit2.Callback

abstract class RequestCallbackWithExpBackoff<T> : Callback<T> {
  private var retryCount = 0

  override fun onFailure(call: Call<T>, t: Throwable) {
    log.d(String.format("Failed to perform request (retry count: %d)", retryCount), t)
    retryCount++

    val delay = computeExponentialBackoff(retryCount)
    Handler(Looper.getMainLooper()).postDelayed({ retry(call) }, delay)
  }

  private fun computeExponentialBackoff(retryCount: Int): Long =
    Math.min(maxRetryDelayMs, baseRetryDelayMs * Math.pow(2.0, retryCount - 1.0)).toLong()

  private fun retry(call: Call<T>) {
    call.clone().enqueue(this)
  }

  companion object {
    private val log = Logger.get(this::class)
    private val baseRetryDelayMs = 200.0
    private val maxRetryDelayMs = 32000.0
  }
}
