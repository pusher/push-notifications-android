package com.pusher.pushnotifications.internal

import android.content.Context

class DeviceStateStore(context: Context) {
  private val preferencesDeviceIdKey = "deviceId"
  private val preferencesFCMTokenKey = "fcmToken"
  private val preferencesInterestsSetKey = "interests"
  private val preferencesInstanceIdKey = "instanceId"
  private val osVersionKey = "osVersion"
  private val sdkVersionKey = "sdkVersion"

  private val preferences = context.getSharedPreferences(
    "com.pusher.pushnotifications.PushNotificationsInstance", Context.MODE_PRIVATE)

  var instanceId: String?
    get() = preferences.getString(preferencesInstanceIdKey, null)
    set(value) = preferences.edit().putString(preferencesInstanceIdKey, value).apply()

  var deviceId: String?
    get() = preferences.getString(preferencesDeviceIdKey, null)
    set(value) = preferences.edit().putString(preferencesDeviceIdKey, value).apply()

  var FCMToken: String?
    get() = preferences.getString(preferencesFCMTokenKey, null)
    set(value) = preferences.edit().putString(preferencesFCMTokenKey, value).apply()

  var osVersion: String?
    get() = preferences.getString(osVersionKey, null)
    set(value) = preferences.edit().putString(osVersionKey, value).apply()

  var sdkVersion: String?
    get() = preferences.getString(sdkVersionKey, null)
    set(value) = preferences.edit().putString(sdkVersionKey, value).apply()

  var interestsSet: MutableSet<String>
    get() = preferences.getStringSet(preferencesInterestsSetKey, mutableSetOf<String>())
    set(value) = preferences.edit().putStringSet(preferencesInterestsSetKey, value).apply()

}
