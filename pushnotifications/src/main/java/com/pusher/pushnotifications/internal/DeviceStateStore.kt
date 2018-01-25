package com.pusher.pushnotifications.internal

import android.content.Context

class DeviceStateStore(context: Context) {
  private val preferencesDeviceIdKey = "deviceId"
  private val preferencesFCMTokenKey = "fcmToken"
  private val preferencesInterestsSetKey = "interests"
  private val preferencesInstanceIdKey = "instanceId"

  private val preferences = context.getSharedPreferences(
    "com.pusher.pushnotifications.PushNotificationsInstance", Context.MODE_PRIVATE)

  fun setInstanceId(instanceId: String) {
    preferences.edit().putString(preferencesInstanceIdKey, instanceId).apply()
  }

  fun getInstanceId(): String? {
    return preferences.getString(preferencesInstanceIdKey, null)
  }

  fun setDeviceId(deviceId: String?) {
    preferences.edit().putString(preferencesDeviceIdKey, deviceId).apply()
  }

  fun getDeviceId(): String? {
    return preferences.getString(preferencesDeviceIdKey, null)
  }

  fun setFCMTokenKey(fcmTokenKey: String) {
    preferences.edit().putString(preferencesFCMTokenKey, fcmTokenKey).apply()
  }

  fun getFCMTokenKey(): String? {
    return preferences.getString(preferencesFCMTokenKey, null)
  }

  fun setInterestsSet(interestsSet: Set<String>) {
    preferences.edit().putStringSet(preferencesInterestsSetKey, interestsSet).apply()
  }

  fun getInterestSet(): MutableSet<String> {
    return preferences.getStringSet(preferencesInterestsSetKey, mutableSetOf<String>())
  }
}
