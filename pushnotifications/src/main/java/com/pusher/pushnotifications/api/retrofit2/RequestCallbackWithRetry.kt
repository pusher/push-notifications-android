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
    if (retryCount > MaxRetries) {
      onFailureAfterRetries(call, t)
    } else {
      val delay = Math.min(maxRetryDelayMs, baseRetryDelayMs * Math.pow(2.0, retryCount - 1.0))
      Handler(Looper.getMainLooper()).postDelayed({ retry(call) }, delay.toLong())
    }
  }

  abstract fun onFailureAfterRetries(call: Call<T>, t: Throwable)

  private fun retry(call: Call<T>) {
    call.clone().enqueue(this)
  }

  companion object {
    private val MaxRetries = 4
    private val log = Logger.get(this::class)
    private val baseRetryDelayMs = 200.0
    private val maxRetryDelayMs = 32000.0
  }
}
