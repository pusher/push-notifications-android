package com.pusher.pushnotifications.api

interface OperationCallback {
  fun onSuccess()
  fun onFailure(t: Throwable)

  companion object {
    var noop = object: OperationCallback {
      override fun onSuccess() {
      }

      override fun onFailure(t: Throwable) {
      }
    }
  }
}
