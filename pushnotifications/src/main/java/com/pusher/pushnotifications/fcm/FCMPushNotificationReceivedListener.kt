package com.pusher.pushnotifications.fcm

import com.google.firebase.messaging.RemoteMessage

interface FCMPushNotificationReceivedListener {
  fun onMessageReceived(remoteMessage: RemoteMessage)
}
