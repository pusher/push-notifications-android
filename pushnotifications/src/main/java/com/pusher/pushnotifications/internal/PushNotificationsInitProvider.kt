package com.pusher.pushnotifications.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import com.pusher.pushnotifications.BuildConfig
import com.pusher.pushnotifications.api.DeviceMetadata
import com.pusher.pushnotifications.api.OperationCallback
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.logging.Logger

class PushNotificationsInitProvider: ContentProvider() {
  private val log = Logger.get(this::class)

  override fun onCreate(): Boolean {
    val deviceStateStore = DeviceStateStore(context)

    deviceStateStore.instanceId?.let {
      val api = PushNotificationsAPI(it)

      api.deviceId = deviceStateStore.deviceId
      api.setSubscriptions(deviceStateStore.interestsSet, OperationCallback.noop)

      if (deviceStateStore.sdkVersion != BuildConfig.VERSION_NAME
          || deviceStateStore.osVersion != Build.VERSION.RELEASE) {
        val metadata = DeviceMetadata(BuildConfig.VERSION_NAME, Build.VERSION.RELEASE)
        api.setMetadata(metadata, operationCallback = object: OperationCallback {
          override fun onSuccess() {
            deviceStateStore.sdkVersion = BuildConfig.VERSION_NAME
            deviceStateStore.osVersion = Build.VERSION.RELEASE
          }

          override fun onFailure(t: Throwable) {
            log.w("Failed to persist metadata.", t)
          }
        })
      }
    }

    return false
  }

  override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
  override fun insert(uri: Uri?, values: ContentValues?): Uri? = null
  override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
  override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int = 0
  override fun getType(uri: Uri?): String? = null
}
