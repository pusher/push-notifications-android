package com.pusher.pushnotifications

/**
 * Generic callback to deal with async successes and failures.
 */
interface BeamsCallback<S, E> {
  fun onSuccess(vararg values: S)

  fun onFailure(error: E)
}

internal class NoopBeamsCallback<S, E> : BeamsCallback<S, E> {
  override fun onSuccess(vararg values: S) {}

  override fun onFailure(error: E) {}
}
