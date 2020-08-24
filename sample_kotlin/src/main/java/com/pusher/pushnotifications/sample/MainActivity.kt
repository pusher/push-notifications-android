package com.pusher.pushnotifications.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.pusher.pushnotifications.PushNotificationReceivedListener
import com.pusher.pushnotifications.PushNotifications
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.SubscriptionsChangedListener

class MainActivity : AppCompatActivity() {
  lateinit var pn: PushNotificationsInstance
  private val instanceId = "8a070eaa-033f-46d6-bb90-f4c15acc47e1"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    PushNotifications.start(applicationContext, instanceId)

    PushNotifications.setOnDeviceInterestsChangedListener(object: SubscriptionsChangedListener {
      override fun onSubscriptionsChanged(interests: Set<String>) {
        Log.i("MainActivity", "Interests: ${interests.toString()}")
      }
    })

    PushNotifications.addDeviceInterest("hello")
    PushNotifications.addDeviceInterest("hello")
    PushNotifications.addDeviceInterest("donuts")
    PushNotifications.addDeviceInterest("hello-donuts")

    Log.i("MainActivity", "Current subscriptions are:")
    PushNotifications.getDeviceInterests().forEach { interest ->
      Log.i("MainActivity", "\t$interest")
    }
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
