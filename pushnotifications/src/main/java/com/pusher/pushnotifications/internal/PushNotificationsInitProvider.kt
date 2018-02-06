package com.pusher.pushnotifications.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.pusher.pushnotifications.api.OperationCallback
import com.pusher.pushnotifications.api.PushNotificationsAPI

class PushNotificationsInitProvider: ContentProvider() {
  override fun onCreate(): Boolean {
    val deviceStateStore = DeviceStateStore(context)

    deviceStateStore.instanceId?.let {
      val api = PushNotificationsAPI(it)

      api.deviceId = deviceStateStore.deviceId
      api.setSubscriptions(deviceStateStore.interestsSet, OperationCallback.noop)
    }

    return false
  }

  override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
  override fun insert(uri: Uri?, values: ContentValues?): Uri? = null
  override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
  override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int = 0
  override fun getType(uri: Uri?): String? = null
}
