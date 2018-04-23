package com.pusher.pushnotifications.sample

import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.pusher.pushnotifications.fcm.MessagingService

class NotificationsMessagingService : MessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    Log.i("NotificationsService", "Got a remote message :o")
    // TODO: Do something useful here
  }
}
