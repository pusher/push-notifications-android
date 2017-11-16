package com.pusher.pushnotifications.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pusher.pushnotifications.logging.Logger

class FCMMessagingService : FirebaseMessagingService() {
  companion object {
    private var listener: FCMPushNotificationReceivedListener? = null
    private val log = Logger.get(this::class)

    fun setOnMessageReceivedListener(messageReceivedListener: FCMPushNotificationReceivedListener) {
      listener = messageReceivedListener
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    if (remoteMessage.data["pusherTokenValidation"] == "true") {
      log.d("Received blank message from Pusher to perform token validation")
    } else {
      log.d("Received from FCM: " + remoteMessage)
      log.d("Received from FCM TITLE: " + remoteMessage.notification?.title)
      log.d("Received from FCM BODY: " + remoteMessage.notification?.body)

      listener?.onMessageReceived(remoteMessage)
    }
  }
}
