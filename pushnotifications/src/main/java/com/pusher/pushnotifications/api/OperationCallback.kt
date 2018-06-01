package com.pusher.pushnotifications.api

interface OperationCallback<T> {
  fun onSuccess(result: T)
  fun onFailure(t: Throwable)

  companion object {
    var noop = object: OperationCallback<Any> {
      override fun onSuccess(result: Any) {
      }

      override fun onFailure(t: Throwable) {
      }
    }
  }
}

interface OperationCallbackNoArgs {
  fun onSuccess()
  fun onFailure(t: Throwable)

  companion object {
    var noop = object: OperationCallbackNoArgs {
      override fun onSuccess() {
      }

      override fun onFailure(t: Throwable) {
      }
    }
  }
}
