package com.pusher.pushnotifications.internal

import android.content.Context

class DeviceStateStore(context: Context) {
  private val preferencesDeviceIdKey = "deviceId"
  private val preferencesFCMTokenKey = "fcmToken"
  private val preferencesInterestsSetKey = "interests"
  private val preferencesInstanceIdKey = "instanceId"

  private val preferences = context.getSharedPreferences(
    "com.pusher.pushnotifications.PushNotificationsInstance", Context.MODE_PRIVATE)

  var instanceId: String?
    get() = preferences.getString(preferencesInstanceIdKey, null)
    set(instanceId) = preferences.edit().putString(preferencesInstanceIdKey, instanceId).apply()

  var deviceId: String?
    get() = preferences.getString(preferencesDeviceIdKey, null)
    set(deviceId) = preferences.edit().putString(preferencesDeviceIdKey, deviceId).apply()

  var FCMToken: String?
    get() = preferences.getString(preferencesFCMTokenKey, null)
    set(FCMToken) = preferences.edit().putString(preferencesFCMTokenKey, FCMToken).apply()

  var interestsSet: MutableSet<String>
    get() = preferences.getStringSet(preferencesInterestsSetKey, mutableSetOf<String>())
    set(interestsSet) = preferences.edit().putStringSet(preferencesInterestsSetKey, interestsSet).apply()
}
