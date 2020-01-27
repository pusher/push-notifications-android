package com.pusher.pushnotifications.internal

import android.content.Context
import android.content.pm.PackageManager

class SDKConfiguration(context: Context) {
  private val metadataOverrideHostURL = "com.pusher.pushnotifications:override-host-url"

  private val metadataBundle =
      context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
          .metaData

  val overrideHostURL: String?
    get() = metadataBundle?.getString(metadataOverrideHostURL, null)
}
