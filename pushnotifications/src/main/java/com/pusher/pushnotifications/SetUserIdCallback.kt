package com.pusher.pushnotifications

interface Callback<S, E> {
  fun onSuccess(vararg values: S)

  fun onFailure(error: E)
}
