package com.pusher.pushnotifications.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pusher.pushnotifications.PushNotificationReceivedListener
import com.pusher.pushnotifications.logging.Logger

class FCMMessagingService : FirebaseMessagingService() {
  companion object {
    private var listener: PushNotificationReceivedListener? = null
    private val log = Logger.get(this::class)

    /**
     * Configures the listener that handles a remote message when the app is in the foreground.
     *
     * @param messageReceivedListener the listener that handles a remote message
     */
    @JvmStatic
    fun setOnMessageReceivedListener(messageReceivedListener: PushNotificationReceivedListener) {
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
