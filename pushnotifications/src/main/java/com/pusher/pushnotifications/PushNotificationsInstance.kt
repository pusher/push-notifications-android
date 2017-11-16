package com.pusher.pushnotifications

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Handler
import android.os.Looper
import com.google.firebase.iid.FirebaseInstanceId
import com.pusher.pushnotifications.api.OperationCallback
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.fcm.FCMInstanceIDService
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.validation.Validations

class PushNotificationsInstance(
  context: Context,
  instanceId: String) {
  init {
    Validations.validateApplicationIcon(context)
  }

  private val preferencesDeviceIdKey = "deviceId"
  private val preferencesFcmTokenKey = "fcmToken"
  private val preferencesInterestsSetKey = "interests"

  private val localPreferences = context.getSharedPreferences(this::class.java.name, MODE_PRIVATE)
  private val log = Logger.get(this::class)

  private val api = PushNotificationsAPI(instanceId)

  fun start(): PushNotificationsInstance {
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
            log.w("Successfully started PushNotifications")
          }

          override fun onFailure(t: Throwable) {
            log.w("Failed to start PushNotifications", t)
          }
        }
      }())
    }

    FCMInstanceIDService.onRefreshToken = handleFcmToken
    FirebaseInstanceId.getInstance().token?.let(handleFcmToken)
    return this
  }

  fun subscribe(interest: String) {
    synchronized(localPreferences) {
      val interestsSet = localPreferences.getStringSet(preferencesInterestsSetKey, mutableSetOf<String>())
      if (!interestsSet.add(interest)) {
        return // not a new interest
      }
      localPreferences.edit().putStringSet(preferencesInterestsSetKey, interestsSet).apply()
    }
    api.subscribe(interest, OperationCallback.noop)
  }

  fun unsubscribe(interest: String) {
    synchronized(localPreferences) {
      val interestsSet = localPreferences.getStringSet(preferencesInterestsSetKey, mutableSetOf<String>())
      if (!interestsSet.remove(interest)) {
        return // interest wasn't present
      }
      localPreferences.edit().putStringSet(preferencesInterestsSetKey, interestsSet).apply()
    }
    api.unsubscribe(interest, OperationCallback.noop)
  }

  fun unsubscribeAll() {
    setSubscriptions(emptySet())
  }

  fun setSubscriptions(interests: Set<String>) {
    // TODO
  }
}
