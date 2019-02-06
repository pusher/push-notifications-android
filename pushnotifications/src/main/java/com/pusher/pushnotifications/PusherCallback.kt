package com.pusher.pushnotifications

/**
 * Generic callback to deal with async successes and failures.
 */
interface PusherCallback<S, E> {
  fun onSuccess(vararg values: S)

  fun onFailure(error: E)
}

internal class NoopPusherCallback<S, E> : PusherCallback<S, E> {
  override fun onSuccess(vararg values: S) {}

  override fun onFailure(error: E) {}
}
