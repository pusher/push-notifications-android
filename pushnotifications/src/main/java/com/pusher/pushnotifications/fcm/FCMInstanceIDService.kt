package com.pusher.pushnotifications.fcm

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.pusher.pushnotifications.logging.Logger

class FCMInstanceIDService : FirebaseInstanceIdService() {
  companion object {
    val log = Logger.get(this::class)
    var onRefreshToken: ((String) -> Unit)? = null
  }

  override fun onTokenRefresh() {
    FirebaseInstanceId.getInstance().token?.let { token ->
      log.d("Got refreshed FCM token: $token")
      onRefreshToken?.let {
        it(token)
      }
    }
  }
}
