package com.pusher.pushnotifications.validation

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import com.pusher.pushnotifications.logging.Logger

object Validations {
  private val log = Logger.get(this::class)
  private const val META_DATA_FIREBASE_NOTIFICATION_IMAGE =
      "com.google.firebase.messaging.default_notification_icon"

  private fun hasDefaultFCMIconInMetadata(context: Context): Boolean {
    try {
      val appInfo = context.packageManager.getApplicationInfo(
        context.packageName, PackageManager.GET_META_DATA)
      if (appInfo.metaData != null) {

        return appInfo.metaData.getInt(META_DATA_FIREBASE_NOTIFICATION_IMAGE, -1) != -1
      }
    } catch (_: PackageManager.NameNotFoundException) {
    }
    return false
  }

  private fun buildIsBelowOreo() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O

  private fun canCreateIconDrawable(context: Context): Boolean {
    try {
      val possibleBitmap = AdaptiveIconDrawable.createFromStream(
        context.resources.openRawResource(context.applicationInfo.icon),
        "applicationInfo.icon")

      if (possibleBitmap != null) {
        return true
      }
    } catch (_: Exception) {
    }
    return false
  }

  internal fun validateApplicationIcon(context: Context) {
    if (buildIsBelowOreo()) {
      return
    }

    if (canCreateIconDrawable(context)) {
      return
    }

    if (hasDefaultFCMIconInMetadata(context)) {
      return
    }

    log.e(
      "You are targeting Android Oreo and using adaptive icons without having a fallback drawable set for FCM notifications. \n" +
        "This can cause a irreversible crash on devices using Oreo.\n" +
        "To learn more about this issue check: https://issuetracker.google.com/issues/68716460"
    )
  }
}
