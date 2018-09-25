package com.pusher.pushnotifications.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.pusher.pushnotifications.PushNotificationReceivedListener
import com.pusher.pushnotifications.PushNotifications
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.SubscriptionsChangedListener
import com.pusher.pushnotifications.auth.AuthData
import com.pusher.pushnotifications.auth.BeamsTokenProvider

class MainActivity : AppCompatActivity() {
  lateinit var pn: PushNotificationsInstance
  private val instanceId = "8a070eaa-033f-46d6-bb90-f4c15acc47e1"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val tokenProvider = BeamsTokenProvider(
        authUrl = "http://example.com",
        getAuthData = fun(): AuthData {
          val sessionToken = "really-secure-token"
          return AuthData(
              headers = mapOf(
                  "Authorization" to "Bearer $sessionToken"
              )
          )
        }
    )

    PushNotifications.start(applicationContext, instanceId, tokenProvider)

    PushNotifications.subscribeDeviceTo("hello")
    PushNotifications.subscribeDeviceTo("donuts")
    PushNotifications.subscribeDeviceTo("hello-donuts")
  }

  override fun onResume() {
    super.onResume()

    PushNotifications.setOnMessageReceivedListenerForVisibleActivity(this, object: PushNotificationReceivedListener {
      override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.i("MainActivity", "Remote message received while this activity is visible!")
      }
    })
  }
}
