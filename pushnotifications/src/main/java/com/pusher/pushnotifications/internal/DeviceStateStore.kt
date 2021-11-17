package com.pusher.pushnotifications.internal

import android.content.Context

class DeviceStateStore(context: Context) {
  private val preferencesOldInstanceIdKey = "instanceId"
  private val preferencesInstanceIdsKey = "instanceIds"

  private val oldPreferences = context.getSharedPreferences(
      "com.pusher.pushnotifications.PushNotificationsInstance", Context.MODE_PRIVATE)
  private val preferences = context.getSharedPreferences(
      "com.pusher.pushnotifications.PushNotificationsInstances", Context.MODE_PRIVATE)

  init {
    // perform migration of old instance configuration, if needed
    val existingOldInstanceId = oldPreferences.getString(preferencesOldInstanceIdKey, null)
    existingOldInstanceId?.let { instanceId ->
      val oldInstanceDSS = InstanceDeviceStateStore(context, null)
      val newInstanceDSS = InstanceDeviceStateStore(context, instanceId)

      newInstanceDSS.deviceId = oldInstanceDSS.deviceId
      newInstanceDSS.userId = oldInstanceDSS.userId
      newInstanceDSS.FCMToken = oldInstanceDSS.FCMToken
      newInstanceDSS.osVersion = oldInstanceDSS.osVersion
      newInstanceDSS.sdkVersion = oldInstanceDSS.sdkVersion
      newInstanceDSS.interests = oldInstanceDSS.interests
      newInstanceDSS.serverConfirmedInterestsHash = oldInstanceDSS.serverConfirmedInterestsHash
      newInstanceDSS.startJobHasBeenEnqueued = oldInstanceDSS.startJobHasBeenEnqueued
      newInstanceDSS.setUserIdHasBeenCalledWith = oldInstanceDSS.setUserIdHasBeenCalledWith

      instanceIds = instanceIds.apply { add(instanceId) }
      preferences.edit().remove(preferencesOldInstanceIdKey).apply()
      oldInstanceDSS.clear()
    }
  }

  var instanceIds: MutableSet<String>
    // We need to clone the set we get from shared preferences because mutating it is not permitted
    // by the Android SDK
    get() = preferences.getStringSet(preferencesInstanceIdsKey, mutableSetOf<String>())!!.toMutableSet()
    set(value) = preferences.edit().putStringSet(preferencesInstanceIdsKey, value).apply()

  fun clear() = preferences.edit().clear().commit()
}

class InstanceDeviceStateStore(context: Context, val instanceId: String?) {
  companion object {
    private val deviceStateStores = mutableMapOf<String, InstanceDeviceStateStore>()

    internal fun obtain(instanceId: String, context: Context): InstanceDeviceStateStore {
      return synchronized(deviceStateStores) {
        deviceStateStores.getOrPut(instanceId) {
          InstanceDeviceStateStore(context, instanceId)
        }
      }
    }
  }

  private val preferencesDeviceIdKey = "deviceId"
  private val preferencesUserIdKey = "userId"
  private val preferencesFCMTokenKey = "fcmToken"
  private val preferencesOSVersionKey = "osVersion"
  private val preferencesSDKVersionKey = "sdkVersion"
  private val preferencesInterestsKey = "interests"
  private val serverConfirmedInterestsHashKey = "serverConfirmedInterestsHash"
  private val startJobHasBeenEnqueuedKey = "startJobHasBeenEnqueued"
  private val setUserIdHasBeenCalledWithKey = "setUserIdHasBeenCalledWith"

  private fun sharedPreferencesName(instanceId: String?): String {
    return if (instanceId == null) {
      // special case to handle the migration of an old instance configuration
      "com.pusher.pushnotifications.PushNotificationsInstance"
    } else {
      "com.pusher.pushnotifications.${instanceId}.PushNotificationsInstance"
    }
  }

  private val preferences = context.getSharedPreferences(sharedPreferencesName(instanceId), Context.MODE_PRIVATE)

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
    get() = preferences.getStringSet(preferencesInterestsKey, mutableSetOf<String>())!!.toMutableSet()
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
