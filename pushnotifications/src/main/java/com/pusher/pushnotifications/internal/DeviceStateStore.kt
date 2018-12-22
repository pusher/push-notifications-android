package com.pusher.pushnotifications.internal

import android.content.Context

class DeviceStateStore(context: Context) {
  private val preferencesDeviceIdKey = "deviceId"
  private val preferencesUserIdKey = "userId"
  private val preferencesFCMTokenKey = "fcmToken"
  private val preferencesInstanceIdKey = "instanceId"
  private val preferencesOSVersionKey = "osVersion"
  private val preferencesSDKVersionKey = "sdkVersion"
  private val preferencesInterestsKey = "interests"
  private val serverConfirmedInterestsHashKey = "serverConfirmedInterestsHash"
  private val startJobHasBeenEnqueuedKey = "startJobHasBeenEnqueued"
  private val setUserIdHasBeenCalledWithKey = "setUserIdHasBeenCalledWith"


  private val preferences = context.getSharedPreferences(
    "com.pusher.pushnotifications.PushNotificationsInstance", Context.MODE_PRIVATE)

  var instanceId: String?
    get() = preferences.getString(preferencesInstanceIdKey, null)
    set(value) = preferences.edit().putString(preferencesInstanceIdKey, value).apply()

  var deviceId: String?
    get() = preferences.getString(preferencesDeviceIdKey, null)
    set(value) = preferences.edit().putString(preferencesDeviceIdKey, value).apply()

  var userId: String?
    get() = preferences.getString(preferencesUserIdKey, null)
    set(value) = preferences.edit().putString(preferencesUserIdKey, value).apply()

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

  var startJobHasBeenEnqueued: Boolean
    get() = preferences.getBoolean(startJobHasBeenEnqueuedKey, false)
    set(value) = preferences.edit().putBoolean(startJobHasBeenEnqueuedKey, value).apply()

  var setUserIdHasBeenCalledWith: String?
    get() = preferences.getString(setUserIdHasBeenCalledWithKey, null)
    set(value) = preferences.edit().putString(setUserIdHasBeenCalledWithKey, value).apply()

  fun clear() = preferences.edit().clear().commit()
}
