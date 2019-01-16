package com.pusher.pushnotifications

/**
 * Generic callback to deal with async successes and failures.
 */
interface Callback<S, E> {
  fun onSuccess(vararg values: S)

  fun onFailure(error: E)
}

internal class NoopCallback<S, E> : Callback<S, E> {
  override fun onSuccess(vararg values: S) {}

  override fun onFailure(error: E) {}
}
