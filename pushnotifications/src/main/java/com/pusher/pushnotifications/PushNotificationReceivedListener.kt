package com.pusher.pushnotifications

import com.google.firebase.messaging.RemoteMessage

/**
 * The listener interface for when a remote message when the app is in the foreground.
 */
interface PushNotificationReceivedListener {

  /**
   * Will be called when a new message is received.
   *
   * @param remoteMessage that was sent from the server
   */
  fun onMessageReceived(remoteMessage: RemoteMessage)
}
