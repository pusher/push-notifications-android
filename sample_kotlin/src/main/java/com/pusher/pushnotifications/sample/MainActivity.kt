package com.pusher.pushnotifications.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.pusher.pushnotifications.*
import com.pusher.pushnotifications.auth.AuthData
import com.pusher.pushnotifications.auth.AuthDataGetter
import com.pusher.pushnotifications.auth.BeamsTokenProvider

class MainActivity : AppCompatActivity() {
  lateinit var pn: PushNotificationsInstance
  private val instanceId = "e7e2c66e-34c6-4862-b7ca-3d90342eb171"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val tokenProvider = BeamsTokenProvider(
        authUrl = "https://beams-test-server.herokuapp.com/auth",
        authDataGetter = object: AuthDataGetter {
          override fun getAuthData(): AuthData {
            return AuthData()
          }
        }
    )
    PushNotifications.start(applicationContext, instanceId, tokenProvider)

    PushNotifications.setUserId("hello-donuts", object: Callback<Void, PusherCallbackError> {
      override fun onSuccess(vararg values: Void) {
        Log.e("MainActivity", "SUCCESSFULLY SET THE USER ID BOOOOM")
      }

      override fun onFailure(callbackError: PusherCallbackError) {
        Log.e("MainActivity", "Error setting user Id: ${callbackError.message}")
      }
    })
  }
}
