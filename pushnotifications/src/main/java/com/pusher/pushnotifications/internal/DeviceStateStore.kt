package com.pusher.pushnotifications.internal

import android.content.Context

class DeviceStateStore(context: Context) {
  private val preferencesDeviceIdKey = "deviceId"
  private val preferencesFCMTokenKey = "fcmToken"
  private val preferencesInstanceIdKey = "instanceId"
  private val preferencesOSVersionKey = "osVersion"
  private val preferencesSDKVersionKey = "sdkVersion"
  private val preferencesInterestsKey = "interests"
  private val serverConfirmedInterestsHashKey = "serverConfirmedInterestsHash"
  private val startHasBeenCalledKey = "startHasBeenCalled"

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
    get() = preferences.getString(preferencesOSVersionKey, null)
    set(value) = preferences.edit().putString(preferencesOSVersionKey, value).apply()

  var sdkVersion: String?
    get() = preferences.getString(preferencesSDKVersionKey, null)
    set(value) = preferences.edit().putString(preferencesSDKVersionKey, value).apply()

  var interests: MutableSet<String>
    // We need to clone the set we get from shared preferences because mutating it is not permitted
    // by the Android SDK
    get() = preferences.getStringSet(preferencesInterestsKey, mutableSetOf<String>()).toMutableSet()
    set(value) = preferences.edit().putStringSet(preferencesInterestsKey, value).apply()

  var serverConfirmedInterestsHash: String?
    get() = preferences.getString(serverConfirmedInterestsHashKey, null)
    set(value) = preferences.edit().putString(serverConfirmedInterestsHashKey, value).apply()

  var startHasBeenCalled: Boolean
    get() = preferences.getBoolean(startHasBeenCalledKey, false)
    set(value) = preferences.edit().putBoolean(startHasBeenCalledKey, value).apply()

  fun clear() = preferences.edit().clear().commit()
}
