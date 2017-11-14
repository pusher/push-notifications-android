package com.pusher.pushnotifications

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.firebase.iid.FirebaseInstanceId
import com.pusher.pushnotifications.api.OperationCallback
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.fcm.FCMInstanceIDService
import com.pusher.pushnotifications.logging.Logger

class PushNotificationsInstance(
  context: Context,
  instanceId: String) {
  private val preferencesDeviceIdKey = "deviceId"
  private val preferencesFcmTokenKey = "fcmToken"

  private val localPreferences = context.getSharedPreferences(this::class.java.name, MODE_PRIVATE)
  private val log = Logger.get(this::class)

  private val api = PushNotificationsAPI(instanceId)

  @JvmOverloads
  fun start(operationCallback: OperationCallback = OperationCallback.noop): PushNotificationsInstance {
    localPreferences.getString(preferencesDeviceIdKey, null)?.let {
      api.deviceId = it
    }

    localPreferences.getString(preferencesFcmTokenKey, null)?.let {
      api.fcmToken = it
    }

    val handleFcmToken = { fcmToken: String ->
      api.registerOrRefreshFCM(fcmToken, {
        object : OperationCallback {
          override fun onSuccess() {
            localPreferences.edit()
              .putString(preferencesDeviceIdKey, api.deviceId)
              .putString(preferencesFcmTokenKey, fcmToken)
              .apply()
            operationCallback.onSuccess()
          }

          override fun onFailure(t: Throwable) {
            operationCallback.onFailure(t)
          }
        }
      }())
    }

    FCMInstanceIDService.onRefreshToken = handleFcmToken
    FirebaseInstanceId.getInstance().token?.let(handleFcmToken)
    return this
  }

  @JvmOverloads
  fun subscribe(interest: String, operationCallback: OperationCallback = OperationCallback.noop) {
    api.subscribe(interest, operationCallback)
  }

  @JvmOverloads
  fun unsubscribe(interest: String, operationCallback: OperationCallback = OperationCallback.noop) {
    api.unsubscribe(interest, operationCallback)
  }

  @JvmOverloads
  fun unsubscribeAll(operationCallback: OperationCallback = OperationCallback.noop) {
    setSubscriptions(emptySet(), operationCallback)
  }

  @JvmOverloads
  fun setSubscriptions(interests: Set<String>, operationCallback: OperationCallback = OperationCallback.noop) {
  }
}
