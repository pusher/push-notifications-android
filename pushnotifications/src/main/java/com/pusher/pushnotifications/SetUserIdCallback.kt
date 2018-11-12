package com.pusher.pushnotifications

interface Callback<S, E> {
  fun onSuccess(vararg values: S)

  fun onFailure(error: E)
}

class NoopCallback<S, E> : Callback<S, E> {
  override fun onSuccess(vararg values: S) {}

  override fun onFailure(error: E) {}
}
