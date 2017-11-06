package com.pusher.pushnotifications.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.pusher.pushnotifications.PushNotifications
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.api.OperationCallback

class MainActivity : AppCompatActivity() {
  lateinit var pn: PushNotificationsInstance
  private val instanceId = "8a070eaa-033f-46d6-bb90-f4c15acc47e1"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val pn1 = PushNotifications.start(applicationContext, instanceId)

    pn1.subscribe("hello", object : OperationCallback {
      override fun onSuccess() {
        Log.i("MainActivity", "Done subscribing to 'hello'!")
      }

      override fun onFailure(t: Throwable) {
        Log.w("MainActivity", "Failed to subscribe to 'hello'", t)
      }
    })
    pn1.unsubscribe("donuts")
  }
}
